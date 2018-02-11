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

package org.jetbrains.kotlin.contracts.parsing

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.*
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

internal class PsiConditionParser(val trace: BindingTrace, val dispatcher: PsiContractParserDispatcher) :
    KtVisitor<BooleanExpression?, Unit>() {
    override fun visitIsExpression(expression: KtIsExpression, data: Unit): BooleanExpression? {
        val variable = dispatcher.parseVariable(expression.leftHandSide) ?: return null
        val typeReference = expression.typeReference ?: return null
        val type = trace[BindingContext.TYPE, typeReference] ?: return null

        return IsInstancePredicate(variable, type, expression.isNegated)
    }

    override fun visitKtElement(element: KtElement, data: Unit): BooleanExpression? {
        val resolvedCall = element.getResolvedCall(trace.bindingContext)
        val descriptor = resolvedCall?.resultingDescriptor ?: return null

        // boolean variable
        if (descriptor is ValueDescriptor) {
            val booleanVariable = dispatcher.parseVariable(element as? KtExpression) ?: return null
            // we don't report type mismatch because it will be reported by the typechecker
            return booleanVariable as? BooleanVariableReference
        }

        // operator
        when {
            descriptor.isEqualsDescriptor() -> {
                val left = dispatcher.parseValue((resolvedCall.dispatchReceiver as? ExpressionReceiver)?.expression) ?: return null
                val right = dispatcher.parseValue(resolvedCall.firstArgumentAsExpressionOrNull()) ?: return null
                val isNegated = (element as? KtBinaryExpression)?.operationToken == KtTokens.EXCLEQ ?: false

                if (left is ConstantReference && left == ConstantReference.NULL && right is VariableReference) {
                    return IsNullPredicate(right, isNegated)
                }

                if (right is ConstantReference && right == ConstantReference.NULL && left is VariableReference) {
                    return IsNullPredicate(left, isNegated)
                }

                trace.report(Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(element, "only equality comparisons with 'null' allowed"))
                return null
            }

            else -> {
                trace.report(Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(element, "unsupported construction"))
                return null
            }
        }
    }

    override fun visitConstantExpression(expression: KtConstantExpression, data: Unit?): BooleanExpression? {
        // we don't report type mismatch because it will be reported by the typechecker
        return dispatcher.parseConstant(expression) as? BooleanConstantReference
    }

    override fun visitCallExpression(expression: KtCallExpression, data: Unit?): BooleanExpression? {
        trace.report(Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(expression, "call-expressions are not supported yet"))
        return null
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): BooleanExpression? {
        val operationConstructor: (BooleanExpression, BooleanExpression) -> BooleanExpression

        when (expression.operationToken) {
            KtTokens.ANDAND -> operationConstructor = ::LogicalAnd
            KtTokens.OROR -> operationConstructor = ::LogicalOr
            else -> return super.visitBinaryExpression(expression, data) // pass binary expression further
        }

        val left = expression.left?.accept(this, data) ?: return null
        val right = expression.right?.accept(this, data) ?: return null
        return operationConstructor(left, right)
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): BooleanExpression? {
        if (expression.operationToken != KtTokens.EXCL) return super.visitUnaryExpression(expression, data)
        val arg = expression.baseExpression?.accept(this, data) ?: return null
        if (arg !is ContractDescriptionValue) {
            trace.report(
                Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(
                    expression.baseExpression!!,
                    "negations in contract description can be applied only to variables/values"
                )
            )
        }
        return LogicalNot(arg)
    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): BooleanExpression? =
        KtPsiUtil.deparenthesize(expression)?.accept(this, data)
}