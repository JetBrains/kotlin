/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Removes redundant casts like
 * - `TYPE_OP type=kotlin.Int origin=IMPLICIT_CAST typeOperand=kotlin.Int`
 */
class RedundantCastsRemoverLowering(val context: LoweringContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(Transformer())
    }

    private class Transformer : IrElementTransformerVoid() {
        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            if (expression.operator == IrTypeOperator.IMPLICIT_CAST && expression.type == expression.argument.type) {
                // Replace a redundant implicit cast with its argument
                return super.visitExpression(expression.argument)
            }
            return super.visitTypeOperator(expression)
        }
    }
}
