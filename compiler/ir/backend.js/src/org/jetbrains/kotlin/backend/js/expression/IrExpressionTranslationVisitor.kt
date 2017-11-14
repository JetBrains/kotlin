/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.expression

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.parameterCount
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.types.KotlinType

class IrExpressionTranslationVisitor(private val context: IrTranslationContext) : IrElementVisitor<JsExpression?, Unit> {
    override fun visitElement(element: IrElement, data: Unit): JsExpression? = null

    override fun visitCall(expression: IrCall, data: Unit): JsExpression? {
        val function = expression.symbol

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, Unit)
        val extensionReceiver = expression.extensionReceiver?.accept(this, Unit)
        val arguments = translateArguments(expression)

        val allArguments = listOfNotNull(extensionReceiver) + arguments
        val qualifier = JsNameRef(context.names[function], dispatchReceiver)

        return JsInvocation(qualifier, allArguments)
    }

    private fun translateArguments(expression: IrFunctionAccessExpression): List<JsExpression> {
        return (0 until expression.symbol.parameterCount)
                .map { expression.getValueArgument(it) }
                .map { it?.accept(this, Unit) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0)) }
                .toList()
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): JsExpression? = expression.argument.accept(this, data)

    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): JsExpression? {
        for (statement in expression.statements.dropLast(1)) {
            statement.accept(this, Unit)?.let {  context.addStatement(JsExpressionStatement(it)) }
        }
        val result = expression.statements.lastOrNull()?.accept(this, Unit)
        return if (KotlinBuiltIns.isUnit(expression.type)) {
            result?.let { context.addStatement(JsExpressionStatement(it)) }
            null
        }
        else {
            result
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): JsExpression? {
        val name = context.names[declaration.symbol]
        val initializer = declaration.initializer?.accept(this, Unit)
        context.addStatement(JsVars(JsVars.JsVar(name, initializer)))
        return null
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): JsExpression =
            JsNameRef(context.names[expression.symbol])

    override fun visitSetVariable(expression: IrSetVariable, data: Unit): JsExpression? {
        val lhs = JsNameRef(context.names[expression.symbol])
        val rhs = expression.value.accept(this, data) ?: JsNullLiteral()
        context.addStatement(JsExpressionStatement(JsBinaryOperation(JsBinaryOperator.ASG, lhs, rhs)))
        return null
    }

    override fun visitWhen(expression: IrWhen, data: Unit): JsExpression? {
        return context.savingStatements {
            val jsBranches = mutableListOf<JsIf>()

            withTemporaryVar(expression.type) { tmpVar ->
                for (branch in expression.branches) {
                    if (branch is IrElseBranch) {
                        tmpVar.translate(branch.result)
                    }
                    else {
                        val jsBranch = JsIf(branch.condition.accept(this, Unit) ?: JsBooleanLiteral(false), JsBlock(), JsBlock())
                        jsBranches += jsBranch
                        context.addStatement(jsBranch)

                        context.statements = (jsBranch.thenStatement as JsBlock).statements
                        tmpVar.translate(branch.result)

                        if (context.statements.size == 1) {
                            jsBranch.thenStatement = context.statements.first()
                        }

                        context.statements = (jsBranch.elseStatement as JsBlock).statements
                    }
                }

                for (jsBranch in jsBranches) {
                    (jsBranch.elseStatement as? JsBlock)?.let {
                        when (it.statements.size) {
                            0 -> jsBranch.elseStatement = null
                            1 -> jsBranch.elseStatement = it.statements[0]
                        }
                    }
                }
            }
        }
    }

    override fun visitReturn(expression: IrReturn, data: Unit): JsExpression? {
        val jsResult = expression.value.accept(this, Unit)
        context.addStatement(JsReturn(jsResult))
        return null
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Unit): JsExpression? {
        val value = expression.value
        return when (value) {
            is String -> JsStringLiteral(value)
            is Int -> JsIntLiteral(value)
            is Boolean -> JsBooleanLiteral(value)
            is Char -> JsIntLiteral(value.toInt())
            is Byte -> JsIntLiteral(value.toInt())
            is Short -> JsIntLiteral(value.toInt())
            is Float -> JsDoubleLiteral(value.toDouble())
            is Double -> JsDoubleLiteral(value)
            null -> JsNullLiteral()
            else -> null
        }
    }

    private fun withTemporaryVar(type: KotlinType, action: (TemporaryVar) -> Unit): JsExpression? {
        val tmpVarName = if (!KotlinBuiltIns.isUnit(type)) {
            JsScope.declareTemporary().also { context.addStatement(JsVars(JsVars.JsVar(it))) }
        }
        else {
            null
        }
        var tmpVarUsed = false

        val tmpVar = object : TemporaryVar {
            override fun translate(expression: IrExpression) {
                val result = expression.accept(this@IrExpressionTranslationVisitor, Unit)
                if (tmpVarName != null) {
                    if (result != null) {
                        tmpVarUsed = true
                        context.addStatement(JsExpressionStatement(JsAstUtils.assignment(tmpVarName.makeRef(), result)))
                    }
                }
                else {
                    if (result != null) {
                        context.addStatement(JsExpressionStatement(result))
                    }
                }
            }
        }

        action(tmpVar)
        return if (tmpVarUsed) tmpVarName?.makeRef() else null
    }

    interface TemporaryVar {
        fun translate(expression: IrExpression)
    }
}