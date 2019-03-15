/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val jvmTypeOperatorLoweringPhase = makeIrFilePhase(
    ::JvmTypeOperatorLowering,
    name = "JvmTypeOperatorLoweringPhase",
    description = "Handle JVM-specific type operator lowerings"
)

private class JvmTypeOperatorLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

            // Make sure that When expressions that are coerced to unit always produces a value.
            // If the When expression do not have an else branch, we add one of the right type
            // so that a value is always produced that can be pop off the stack.
            //
            // Otherwise, code such as
            //
            // val b = getBoolean()
            // if (b) 5
            // else if (b) 4
            //
            // leads to the generation of code that will underflow the stack.
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression.operator === IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
                    val argument = expression.argument
                    if (argument is IrWhen
                        && argument.branches.size > 0
                        && argument.branches.last() !is IrElseBranch) {
                        argument.branches.add(
                            IrElseBranchImpl(
                                IrConstImpl.constTrue(argument.startOffset, argument.endOffset, context.irBuiltIns.booleanType),
                                IrBlockImpl(argument.startOffset, argument.endOffset, argument.type))
                        )
                    }
                }
                return expression
            }
        })
    }
}
