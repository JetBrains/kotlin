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

package org.jetbrains.kotlin.effectsystem.adapters

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.effectsystem.factories.createConstant
import org.jetbrains.kotlin.effectsystem.functors.*
import org.jetbrains.kotlin.effectsystem.resolving.FunctorResolver
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema
import org.jetbrains.kotlin.effectsystem.structure.UNIT_ID
import org.jetbrains.kotlin.effectsystem.structure.calltree.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.ifEmpty

/**
 * Visits Psi-tree and builds Call Tree
 */
class CallTreeBuilder(
        private val bindingContext: BindingContext,
        private val moduleDescriptor: ModuleDescriptor,
        private val functorResolver: FunctorResolver
) : KtVisitor<CTNode, Unit>() {

    override fun visitKtElement(element: KtElement, data: Unit): CTNode = UNKNOWN_CALL

    override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): CTNode {
        val bindingContext = bindingContext

        val type: KotlinType = bindingContext.getType(expression) ?: return UNKNOWN_CALL

        val compileTimeConstant: CompileTimeConstant<*>
                = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression) ?: return UNKNOWN_CALL
        val value: Any? = compileTimeConstant.getValue(type)
        return CTConstant(ValueIdsFactory.idForConstant(value), type, value)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit): CTNode {
        // TODO: make proper resolving
        if (expression.text == "Unit") return CTConstant(UNIT_ID, DefaultBuiltIns.Instance.unitType, Unit)
        return tryCreateVariable(expression)
    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): CTNode {
        val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)
        return if (deparenthesized == expression) UNKNOWN_CALL else deparenthesized.accept(this, data)
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): CTCall {
        tryGetCachedCall(expression)?.let { return it }

        val argNode = expression.baseExpression?.accept(this, data) ?: return UNKNOWN_CALL
        return when (expression.operationToken) {
            KtTokens.EXCL -> CTCall(NotFunctor(), listOf(argNode))
            else -> return UNKNOWN_CALL
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Unit): CTCall {
        tryGetCachedCall(expression)?.let { return it }

        val receiver = expression.receiverExpression.accept(this, data)

        val resolvedCall = expression.selectorExpression.getResolvedCall(bindingContext) ?: return UNKNOWN_CALL
        val argNodes = resolvedCall.valueArgumentsByIndex?.map {
            (it as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression()?.accept(this, data) ?: return UNKNOWN_CALL
        } ?: return UNKNOWN_CALL

        val functor = functorResolver.resolveFunctor(resolvedCall) ?: return UNKNOWN_CALL
        return CTCall(functor, listOf(receiver) + argNodes)
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Unit?): CTNode {
        val dfv = expression.createDataFlowValue() ?: return UNKNOWN_CALL // Could be possible for unavailable/incorrect this
        return CTVariable(ValueIdsFactory.dfvBased(dfv), dfv.type)
    }

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Unit?): CTNode = tryCreateVariable(expression)

    override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): CTNode = expression.baseExpression?.accept(this, data) ?: UNKNOWN_CALL

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): CTCall {
        tryGetCachedCall(expression)?.let { return it }

        val leftNode = expression.left?.accept(this, data) ?: return UNKNOWN_CALL
        val rightNode = expression.right?.accept(this, data) ?: return UNKNOWN_CALL

        val functor = when (expression.operationToken) {
            KtTokens.EQEQ, KtTokens.EQEQEQ -> {
                rightNode as? CTConstant ?: return UNKNOWN_CALL
                return CTCall(EqualsToBinaryConstantFunctor(false, createConstant(rightNode.id, rightNode.value, rightNode.type)), listOf(leftNode))
            }
            KtTokens.EXCLEQ, KtTokens.EXCLEQEQEQ -> {
                rightNode as? CTConstant ?: return UNKNOWN_CALL
                return CTCall(EqualsToBinaryConstantFunctor(true, createConstant(rightNode.id, rightNode.value, rightNode.type)), listOf(leftNode))
            }
            KtTokens.ANDAND -> AndFunctor()
            KtTokens.OROR -> OrFunctor()
            else -> return UNKNOWN_CALL
        }
        return CTCall(functor, listOf(leftNode, rightNode))
    }

    override fun visitCallExpression(expression: KtCallExpression, data: Unit): CTCall {
        tryGetCachedCall(expression)?.let { return it }

        if (expression.parent is KtDotQualifiedExpression && expression.parent.cast<KtDotQualifiedExpression>().receiverExpression != expression) {
            // Don't analyze call alone without receiver
            return visitDotQualifiedExpression(expression.parent as KtDotQualifiedExpression, data)
        }

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return UNKNOWN_CALL
        val functor = functorResolver.resolveFunctor(resolvedCall) ?: return UNKNOWN_CALL

        val argNodes = resolvedCall.valueArgumentsByIndex?.map {
            (it as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression()?.accept(this, data) ?: return UNKNOWN_CALL
        } ?: return UNKNOWN_CALL


        val receiverValue = if (resolvedCall.extensionReceiver != null) listOf(UNKNOWN_CALL) else emptyList()
        return CTCall(functor, receiverValue + argNodes)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression, data: Unit?): CTNode = tryCreateVariable(expression)

    override fun visitIsExpression(expression: KtIsExpression, data: Unit): CTCall {
        tryGetCachedCall(expression)?.let { return it }

        val leftNode = expression.leftHandSide.accept(this, data)
        val rightType: KotlinType = bindingContext.get(BindingContext.TYPE, expression.typeReference) ?: return UNKNOWN_CALL
        val functor = IsFunctor(rightType, expression.isNegated)
        return CTCall(functor, listOf(leftNode))
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit?): CTNode {
        // We don't care about lambda type in fact, and it can be non-resolved at this point anyway
        val dfv = DataFlowValueFactory.createDataFlowValue(expression, DefaultBuiltIns.Instance.anyType, bindingContext, moduleDescriptor)
        val id = ValueIdsFactory.dfvBased(dfv)
        return CTLambda(id, DefaultBuiltIns.Instance.nullableAnyType, null)
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Unit): CTConstant {
        val concatenatedString = expression.entries.map { it.text }.ifEmpty { listOf("") }.reduce { s, acc -> s + acc }
        return CTConstant(ValueIdsFactory.idForConstant(concatenatedString), DefaultBuiltIns.Instance.stringType, concatenatedString)
    }

    private fun KtExpression.createDataFlowValue(): DataFlowValue? {
        return DataFlowValueFactory.createDataFlowValue(
                expression = this,
                type = bindingContext.getType(this) ?: return null,
                bindingContext = bindingContext,
                containingDeclarationOrModule = moduleDescriptor
        )
    }

    private fun tryCreateVariable(expression: KtExpression): CTNode {
        val dfv = expression.createDataFlowValue() ?: return UNKNOWN_CALL
        return CTVariable(ValueIdsFactory.dfvBased(dfv), dfv.type)
    }

    private fun tryGetCachedCall(expression: KtExpression): CTCall? {
        val cachedSchema = bindingContext[BindingContext.EXPRESSION_EFFECTS, expression] ?: return null
        // return call with functor that just returns cached schema on 'apply'
        return CTCall(object : ESFunctor {
            override fun apply(arguments: List<EffectSchema>): EffectSchema? = cachedSchema
        }, listOf())
    }

    private val UNKNOWN_CALL = CTCall(UnknownFunctor, listOf())
}