/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.isError

class DataFlowValueFactoryImpl
@Deprecated("Please, avoid to use that implementation explicitly. If you need DataFlowValueFactory, use injection")
constructor(private val languageVersionSettings: LanguageVersionSettings) : DataFlowValueFactory {

    // Receivers
    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(receiverValue, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ) = when (receiverValue) {
        is TransientReceiver, is ImplicitReceiver -> createDataFlowValueForStableReceiver(receiverValue)
        is ExpressionReceiver -> createDataFlowValue(
            receiverValue.expression,
            receiverValue.getType(),
            bindingContext,
            containingDeclarationOrModule
        )
        else -> throw UnsupportedOperationException("Unsupported receiver value: " + receiverValue::class.java.name)
    }

    override fun createDataFlowValueForStableReceiver(receiver: ReceiverValue) =
        DataFlowValue(IdentifierInfo.Receiver(receiver), receiver.type)


    // Property
    override fun createDataFlowValueForProperty(
        property: KtProperty,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue {
        val identifierInfo = IdentifierInfo.Variable(
            variableDescriptor,
            variableDescriptor.variableKind(usageContainingModule, bindingContext, property, languageVersionSettings),
            bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, variableDescriptor]
        )
        return DataFlowValue(identifierInfo, variableDescriptor.type)
    }


    // Expressions
    override fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(expression, type, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    override fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue {
        return when {
            expression is KtConstantExpression && expression.node.elementType === KtNodeTypes.NULL ->
                DataFlowValue.nullValue(containingDeclarationOrModule.builtIns)

            type.isError -> DataFlowValue.ERROR

            KotlinBuiltIns.isNullableNothing(type) ->
                DataFlowValue.nullValue(containingDeclarationOrModule.builtIns) // 'null' is the only inhabitant of 'Nothing?'

        // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
        // by calling `getImmanentNullability` (as it happens below).
        //
        // But there are some problem with types built on type parameters, e.g.
        // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            ExpressionTypingUtils.isExclExclExpression(KtPsiUtil.deparenthesize(expression)) ->
                DataFlowValue(IdentifierInfo.Expression(expression), type, Nullability.NOT_NULL)

            isComplexExpression(expression) ->
                DataFlowValue(IdentifierInfo.Expression(expression, stableComplex = true), type)

            else -> {
                val result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule, languageVersionSettings)
                DataFlowValue(if (result === IdentifierInfo.NO) IdentifierInfo.Expression(expression) else result, type)
            }
        }
    }

    private fun isComplexExpression(expression: KtExpression): Boolean = when (expression) {
        is KtBlockExpression, is KtIfExpression, is KtWhenExpression -> true

        is KtBinaryExpression -> expression.operationToken === KtTokens.ELVIS

        is KtParenthesizedExpression -> {
            val deparenthesized = KtPsiUtil.deparenthesize(expression)
            deparenthesized != null && isComplexExpression(deparenthesized)
        }

        else -> false
    }
}