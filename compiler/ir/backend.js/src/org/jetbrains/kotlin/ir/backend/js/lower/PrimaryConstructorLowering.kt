/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val IrDeclaration.isSyntheticPrimaryConstructor: Boolean
    get() = origin == PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR

// Create primary constructor if it doesn't exist
class PrimaryConstructorLowering(val context: JsCommonBackendContext) : DeclarationTransformer {

    private var IrClass.syntheticPrimaryConstructor by context.mapping.classToSyntheticPrimaryConstructor

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.kind != ClassKind.INTERFACE) {
            val constructors = declaration.constructors

            if (constructors.any { it.isPrimary }) return null

            declaration.syntheticPrimaryConstructor = createPrimaryConstructor(declaration)
        }

        return null
    }

    companion object {
        val SYNTHETIC_PRIMARY_CONSTRUCTOR by IrDeclarationOriginImpl
    }

    private val unitType = context.irBuiltIns.unitType

    private fun createPrimaryConstructor(irClass: IrClass): IrConstructor {
        val declaration = irClass.addConstructor {
            origin = SYNTHETIC_PRIMARY_CONSTRUCTOR
            isPrimary = true
            visibility = DescriptorVisibilities.PRIVATE
        }

        declaration.body = irClass.run {
            factory.createBlockBody(startOffset, endOffset, listOf(IrInstanceInitializerCallImpl(startOffset, endOffset, symbol, unitType)))
        }

        return declaration
    }
}

class DelegateToSyntheticPrimaryConstructor(context: JsCommonBackendContext) : BodyLoweringPass {

    private var IrClass.syntheticPrimaryConstructor by context.mapping.classToSyntheticPrimaryConstructor

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrConstructor && !container.isPrimary) {
            container.parentAsClass.syntheticPrimaryConstructor?.let { primary ->
                val initializeTransformer = object : IrElementTransformerVoid() {
                    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement = declaration // optimize visiting

                    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = expression.run {
                        IrDelegatingConstructorCallImpl(
                            startOffset, endOffset, type,
                            primary.symbol,
                            valueArgumentsCount = primary.valueParameters.size,
                            typeArgumentsCount = primary.typeParameters.size
                        )
                    }
                }

                irBody.transformChildrenVoid(initializeTransformer)
            }
        }
    }
}
