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
    private val script = getJsScript()

    fun generateStatement(): JsStatement {
        if (script.statements.isEmpty()) return JsEmpty

        val newStatements = script.statements.toMutableList().apply {
            val expression = (script.statements.last() as? JsReturn)?.expression ?: return@apply

            if (expression is JsPrefixOperation && expression.operator == JsUnaryOperator.VOID) {
                removeLastOrNull()
            } else {
                set(lastIndex, expression.makeStmt())
            }
        }

        val statements = when (newStatements.size) {
            0 -> return JsEmpty
            1 -> newStatements.map { it.withSource(jsOrJsFuncCall, context) }
            // TODO: use transparent block (e.g. JsCompositeBlock)
            else -> newStatements
        }

        return JsScript(statements, script.comments)
    }

    fun generateExpression(): JsExpression {
        if (script.statements.isEmpty()) return JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)) // TODO: report warning or even error

        val lastStatement = script.statements.last()
        val lastExpression = when (lastStatement) {
            is JsReturn -> lastStatement.expression
            is JsExpressionStatement -> lastStatement.expression
            else -> null
        }
        if (script.statements.size == 1 && lastExpression != null) {
            return JsScript(
                listOf(lastExpression.withSource(jsOrJsFuncCall, context).makeStmt()),
                script.comments
            )
        }

        val newStatements = script.statements.toMutableList()

        when (lastStatement) {
            is JsReturn -> {
            }
            is JsExpressionStatement -> {
                newStatements[script.statements.lastIndex] = JsReturn(lastStatement.expression)
            }
            // TODO: report warning or even error
            else -> newStatements += JsReturn(JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)))
        }

        val syntheticFunction = JsFunction(
            emptyScope,
            JsBlock(JsScript(newStatements, script.comments)),
            ""
        )
        return JsInvocation(syntheticFunction).withSource(jsOrJsFuncCall, context)
    }

    private fun getJsScript(): JsScript {
        return when {
            context.checkIfJsCode(jsOrJsFuncCall.symbol) -> {
                translateJsCodeIntoJsScript(
                    jsOrJsFuncCall.getValueArgument(0) ?: compilationException("JsCode is expected", jsOrJsFuncCall),
                    context.staticContext.backendContext
                )
                    ?: compilationException("Cannot compute js code", jsOrJsFuncCall)
            }

            context.checkIfAnnotatedWithJsFunc(jsOrJsFuncCall.symbol) ->
                FunctionWithJsFuncAnnotationInliner(jsOrJsFuncCall, context).generateResultJsScript()

            else -> compilationException("`js` function call or function with @JsFunc annotation expected", jsOrJsFuncCall)
        }
    }
}