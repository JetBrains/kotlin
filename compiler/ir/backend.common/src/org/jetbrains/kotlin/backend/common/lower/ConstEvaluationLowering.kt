/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.backend.common.ir.evaluation.evaluate
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Evaluates the same functions that [FirExpressionEvaluator] does, but inside bodies.
 *
 *  Note: While the following expression "1 + 1 + n" (where `n` is non const variable) will be transformed into "2 + n",
"n + 1 + 1" will NOT be transformed into "n + 2", because on IR level it is represented as "(n + 1) + 1".
 */
class ConstEvaluationLowering(
    val context: CommonBackendContext,
    private val isFloatingPointOptimizationEnabled: Boolean = true
) : FileLoweringPass {
    private val inlineConstTracker = context.configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER]

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrTransformer<Nothing?>() {
            override fun visitExpression(expression: IrExpression, data: Nothing?): IrExpression {
                val superResult = super.visitExpression(expression, data)
                val evaluateResult = evaluate(
                    superResult,
                    irFile,
                    context.irBuiltIns,
                    inlineConstTracker,
                    isFloatingPointOptimizationEnabled = isFloatingPointOptimizationEnabled
                )
                return evaluateResult ?: superResult
            }
        }, null)
    }
}
