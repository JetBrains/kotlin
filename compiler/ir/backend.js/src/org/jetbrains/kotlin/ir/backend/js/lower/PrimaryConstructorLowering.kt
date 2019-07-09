/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


// Create primary constructor if it doesn't exist
class PrimaryConstructorLowering(context: CommonBackendContext) : ClassLoweringPass {
    private val unitType = context.irBuiltIns.unitType

    override fun lower(irClass: IrClass) {
        val constructors = irClass.declarations.filterIsInstance<IrConstructor>()

        if (constructors.any { it.isPrimary }) return

        val primary = createPrimaryConstructor(irClass)

        val initializeTransformer = object : IrElementTransformerVoid() {
            override fun visitDeclaration(declaration: IrDeclaration) = declaration // optimize visiting

            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = expression.run {
                IrDelegatingConstructorCallImpl(startOffset, endOffset, type, primary.symbol)
            }
        }

        constructors.forEach { it.transformChildrenVoid(initializeTransformer) }
    }

    private object SYNTHETIC_PRIMARY_CONSTRUCTOR : IrDeclarationOriginImpl("SYNTHETIC_PRIMARY_CONSTRUCTOR")

    private fun createPrimaryConstructor(irClass: IrClass): IrConstructor {
        val declaration = irClass.addConstructor {
            origin = SYNTHETIC_PRIMARY_CONSTRUCTOR
            isPrimary = true
            visibility = Visibilities.PRIVATE
        }

        declaration.body = irClass.run {
            IrBlockBodyImpl(startOffset, endOffset, listOf(IrInstanceInitializerCallImpl(startOffset, endOffset, symbol, unitType)))
        }

        return declaration
    }
}