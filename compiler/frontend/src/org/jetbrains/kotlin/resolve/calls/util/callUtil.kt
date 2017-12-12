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

package org.jetbrains.kotlin.resolve.calls.callUtil

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CALL
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure

// resolved call

fun <D : CallableDescriptor> ResolvedCall<D>.noErrorsInValueArguments(): Boolean {
    return call.valueArguments.all { argument -> !getArgumentMapping(argument!!).isError() }
}

fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedArguments(): Boolean {
    return call.valueArguments.any { argument -> getArgumentMapping(argument!!) == ArgumentUnmapped }
}

fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedParameters(): Boolean {
    val parameterToArgumentMap = valueArguments
    return !parameterToArgumentMap.keys.containsAll(resultingDescriptor.valueParameters)
}

fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMapped()
        = call.valueArguments.all { argument -> getArgumentMapping(argument) is ArgumentMatch }

fun <D : CallableDescriptor> ResolvedCall<D>.hasTypeMismatchErrorOnParameter(parameter: ValueParameterDescriptor): Boolean {
    val resolvedValueArgument = valueArguments[parameter]
    if (resolvedValueArgument == null) return true

    return resolvedValueArgument.arguments.any { argument ->
        val argumentMapping = getArgumentMapping(argument)
        argumentMapping is ArgumentMatch && argumentMapping.status == ArgumentMatchStatus.TYPE_MISMATCH
    }
}

fun <D : CallableDescriptor> ResolvedCall<D>.getParameterForArgument(valueArgument: ValueArgument?): ValueParameterDescriptor? {
    return (valueArgument?.let { getArgumentMapping(it) } as? ArgumentMatch)?.valueParameter
}

fun <D : CallableDescriptor> ResolvedCall<D>.usesDefaultArguments(): Boolean {
    return valueArgumentsByIndex?.any { it is DefaultValueArgument } ?: false
}


// call

fun <C: ResolutionContext<C>> Call.hasUnresolvedArguments(context: ResolutionContext<C>): Boolean {
    val arguments = valueArguments.map { it.getArgumentExpression() }
    return arguments.any (fun (argument: KtExpression?): Boolean {
        if (argument == null || ArgumentTypeResolver.isFunctionLiteralOrCallableReference(argument, context)) return false

        val resolvedCall = argument.getResolvedCall(context.trace.bindingContext) as MutableResolvedCall<*>?
        if (resolvedCall != null && !resolvedCall.hasInferredReturnType()) return false

        val expressionType = context.trace.bindingContext.getType(argument)
        return expressionType == null || expressionType.isError
    })
}

fun Call.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

fun KtCallElement.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

fun Call.getValueArgumentListOrElement(): KtElement =
        if (this is CallTransformer.CallForImplicitInvoke) {
            outerCall.getValueArgumentListOrElement()
        }
        else {
            valueArgumentList ?: calleeExpression ?: callElement
        }

@Suppress("UNCHECKED_CAST")
private fun List<ValueArgument?>.filterArgsInParentheses() = filter { it !is KtLambdaArgument } as List<ValueArgument>

fun Call.getValueArgumentForExpression(expression: KtExpression): ValueArgument? {
    fun KtElement.deparenthesizeStructurally(): KtElement? {
        val deparenthesized = if (this is KtExpression) KtPsiUtil.deparenthesizeOnce(this) else this
        return when {
            deparenthesized != this -> deparenthesized
            this is KtLambdaExpression -> this.functionLiteral
            this is KtFunctionLiteral -> this.bodyExpression
            else -> null
        }
    }
    fun KtElement.isParenthesizedExpression() = generateSequence(this) { it.deparenthesizeStructurally() }.any { it == expression }
    return valueArguments.firstOrNull { it?.getArgumentExpression()?.isParenthesizedExpression() ?: false }
}

// Get call / resolved call from binding context

fun KtElement?.getCalleeExpressionIfAny(): KtExpression? {
    val element = if (this is KtExpression) KtPsiUtil.deparenthesize(this) else this
    return when (element) {
        is KtSimpleNameExpression -> element
        is KtCallElement -> element.calleeExpression
        is KtQualifiedExpression -> element.selectorExpression.getCalleeExpressionIfAny()
        is KtOperationExpression -> element.operationReference
        else -> null
    }
}

/**
 *  For expressions like <code>a(), a[i], a.b.c(), +a, a + b, (a()), a(): Int, @label a()</code>
 *  returns a corresponding call.
 *
 *  Note: special construction like <code>a!!, a ?: b, if (c) a else b</code> are resolved as calls,
 *  so there is a corresponding call for them.
 */
fun KtElement.getCall(context: BindingContext): Call? {
    val element = if (this is KtExpression) KtPsiUtil.deparenthesize(this) else this
    if (element == null) return null

    // Do not use Call bound to outer call expression (if any) to prevent stack overflow during analysis
    if (element is KtCallElement && element.calleeExpression == null) return null

    val parent = element.parent
    val reference: KtExpression? = when (parent) {
        is KtInstanceExpressionWithLabel -> parent
        is KtUserType -> parent.parent.parent as? KtConstructorCalleeExpression
        else -> element.getCalleeExpressionIfAny()
    }
    if (reference != null) {
        return context[CALL, reference]
    }
    return context[CALL, element]
}

fun KtElement.getParentCall(context: BindingContext, strict: Boolean = true): Call? {
    val callExpressionTypes = arrayOf<Class<out KtElement>?>(
            KtSimpleNameExpression::class.java, KtCallElement::class.java, KtBinaryExpression::class.java,
            KtUnaryExpression::class.java, KtArrayAccessExpression::class.java)

    val parent = if (strict) {
        PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
    } else {
        PsiTreeUtil.getNonStrictParentOfType(this, *callExpressionTypes)
    }
    return parent?.getCall(context)
}

fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return context[RESOLVED_CALL, this]
}

fun KtElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return this?.getCall(context)?.getResolvedCall(context)
}

fun KtElement?.getParentResolvedCall(context: BindingContext, strict: Boolean = true): ResolvedCall<out CallableDescriptor>? {
    return this?.getParentCall(context, strict)?.getResolvedCall(context)
}

fun KtElement.getCallWithAssert(context: BindingContext): Call {
    return getCall(context).sure { "No call for ${this.getTextWithLocation()}" }
}

fun KtElement.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.getTextWithLocation()}" }
}

fun Call.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.callElement.getTextWithLocation()}" }
}

fun KtExpression.getFunctionResolvedCallWithAssert(context: BindingContext): ResolvedCall<out FunctionDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is FunctionDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends FunctionDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out FunctionDescriptor>
}

fun KtExpression.getPropertyResolvedCallWithAssert(context: BindingContext): ResolvedCall<out PropertyDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is PropertyDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends PropertyDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out PropertyDescriptor>
}

fun KtExpression.getVariableResolvedCallWithAssert(context: BindingContext): ResolvedCall<out VariableDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is VariableDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends PropertyDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out VariableDescriptor>
}

fun KtExpression.getType(context: BindingContext): KotlinType? {
    val type = context.getType(this)
    if (type != null) return type
    val resolvedCall = this.getResolvedCall(context)
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        return resolvedCall.variableCall.resultingDescriptor.type
    }
    return null
}

val KtElement.isFakeElement: Boolean
    get() {
        // Don't use getContainingKtFile() because in IDE we can get an element with JavaDummyHolder as containing file
        val file = containingFile
        return file is KtFile && file.doNotAnalyze != null
    }

fun Call.isSafeCall(): Boolean {
    if (this is CallTransformer.CallForImplicitInvoke) {
        //implicit safe 'invoke'
        if (outerCall.isSemanticallyEquivalentToSafeCall) {
            return true
        }
    }
    return isSemanticallyEquivalentToSafeCall
}

fun Call.isCallableReference(): Boolean {
    val callElement = callElement
    return callElement is KtNameReferenceExpression &&
           (callElement.parent as? KtCallableReferenceExpression)?.callableReference == callElement
}

fun Call.createLookupLocation(): KotlinLookupLocation {
    val calleeExpression = calleeExpression
    val element =
            if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
            else callElement
    return KotlinLookupLocation(element)
}

fun ResolvedCall<*>.getFirstArgumentExpression(): KtExpression? =
        valueArgumentsByIndex?.run { get(0).arguments[0].getArgumentExpression() }

fun ResolvedCall<*>.getReceiverExpression(): KtExpression? =
        extensionReceiver.safeAs<ExpressionReceiver>()?.expression ?:
        dispatchReceiver.safeAs<ExpressionReceiver>()?.expression