/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Transforms `return when { ... }` into per-branch returns so that each
 * branch's result is individually wrapped in an [IrReturn].
 *
 * Before:
 * ```
 *   return when {
 *       cond1 -> expr1
 *       else  -> expr2
 *   }
 * ```
 *
 * After:
 * ```
 *   when {
 *       cond1 -> return expr1
 *       else  -> return expr2
 *   }
 * ```
 *
 * Nested `when`/`if` and trailing-expression blocks are handled recursively.
 * This makes each branch's terminal value directly visible as an [IrReturn],
 * which simplifies tail-position analysis.
 */
fun normalizeReturnWhen(func: IrSimpleFunction) {
    val body = func.body as? IrBlockBody ?: return
    val funcSymbol = func.symbol
    body.transform(object : IrTransformer<Nothing?>() {
        override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
            expression.transformChildren(this, data)
            if (expression.returnTargetSymbol != funcSymbol) return expression
            return distributeReturn(expression.value, expression) ?: expression
        }

        private fun distributeReturn(expr: IrExpression, proto: IrReturn): IrExpression? {
            when (expr) {
                is IrWhen -> {
                    for (branch in expr.branches) {
                        branch.result = returned(branch.result, proto)
                    }
                }
                // A return@label inside a returnable block would bypass a return placed on its last statement.
                is IrReturnableBlock -> return null
                is IrContainerExpression -> {
                    val last = expr.statements.lastOrNull() as? IrExpression ?: return null
                    expr.statements[expr.statements.lastIndex] = returned(last, proto)
                }
                else -> return null
            }
            return expr
        }

        private fun returned(expr: IrExpression, proto: IrReturn): IrExpression {
            if (expr is IrReturn) return expr
            return distributeReturn(expr, proto) ?: IrReturnImpl(
                expr.startOffset, expr.endOffset,
                proto.type,
                proto.returnTargetSymbol,
                expr,
            )
        }
    }, null)
}
