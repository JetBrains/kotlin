/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isSyntheticSingleton
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

@PhaseDescription(
    name = "StaticCallableReferencePhase",
    description = "Turn static callable references into singletons"
)
internal class StaticCallableReferenceLowering(val backendContext: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid()
        if (declaration.isSyntheticSingleton) {
            declaration.declarations += backendContext.cachedDeclarations.getFieldForObjectInstance(declaration).apply {
                initializer = backendContext.createIrBuilder(symbol).run {
                    irExprBody(irCall(declaration.primaryConstructor!!))
                }
            }
        }
        return declaration
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructedClass = expression.symbol.owner.constructedClass
        if (!constructedClass.isSyntheticSingleton)
            return super.visitConstructorCall(expression)

        val instanceField = backendContext.cachedDeclarations.getFieldForObjectInstance(constructedClass)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceField.symbol, expression.type)
    }
}
