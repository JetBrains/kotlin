/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.*

class JsCallTransformer(private val jsOrJsFuncCall: IrCall, private val context: JsGenerationContext) {
    private val statements = getJsStatements()

    fun generateStatement(): JsStatement {
        if (statements.isEmpty()) return JsEmpty

        val newStatements = statements.toMutableList().apply {
            val expression = (last() as? JsReturn)?.expression ?: return@apply

            if (expression is JsPrefixOperation && expression.operator == JsUnaryOperator.VOID) {
                removeLastOrNull()
            } else {
                set(lastIndex, expression.makeStmt())
            }
        }

        return when (newStatements.size) {
            0 -> JsEmpty
            1 -> newStatements.single().withSource(jsOrJsFuncCall, context)
            // TODO: use transparent block (e.g. JsCompositeBlock)
            else -> JsCompositeBlock(newStatements)
        }
    }

    fun generateExpression(): JsExpression {
        if (statements.isEmpty()) return JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)) // TODO: report warning or even error

        val lastStatement = statements.findLast { it !is JsSingleLineComment && it !is JsMultiLineComment }
        val lastExpression = when (lastStatement) {
            is JsReturn -> lastStatement.expression
            is JsExpressionStatement -> lastStatement.expression
            else -> null
        }
        if (statements.size == 1 && lastExpression != null) {
            return lastExpression.withSource(jsOrJsFuncCall, context)
        }

        val newStatements = statements.toMutableList()

        when (lastStatement) {
            is JsReturn -> {
            }
            is JsExpressionStatement -> {
                newStatements[statements.lastIndex] = JsReturn(lastStatement.expression)
            }
            // TODO: report warning or even error
            else -> newStatements += JsReturn(JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)))
        }

        val syntheticFunction = JsFunction(emptyScope, JsBlock(newStatements), "")
        return JsInvocation(syntheticFunction).withSource(jsOrJsFuncCall, context)
    }

    private fun getJsStatements(): List<JsStatement> {
        return when {
            context.checkIfJsCode(jsOrJsFuncCall.symbol) -> {
                translateJsCodeIntoStatementList(
                    jsOrJsFuncCall.getValueArgument(0) ?: compilationException("JsCode is expected", jsOrJsFuncCall),
                    context.staticContext.backendContext,
                    context.currentFile.fileEntry
                )
                    ?: compilationException("Cannot compute js code", jsOrJsFuncCall)
            }

            context.checkIfHasAssociatedJsCode(jsOrJsFuncCall.symbol) ->
                FunctionWithJsFuncAnnotationInliner(jsOrJsFuncCall, context).generateResultStatement()

            else -> compilationException("`js` function call or function with @JsFunc annotation expected", jsOrJsFuncCall)
        }
    }
}
