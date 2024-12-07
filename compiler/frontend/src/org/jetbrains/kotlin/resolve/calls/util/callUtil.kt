/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CALL
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.psiKotlinCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains
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

fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMapped() =
    call.valueArguments.all { argument -> getArgumentMapping(argument) is ArgumentMatch }

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

fun <C : ResolutionContext<C>> Call.hasUnresolvedArguments(context: ResolutionContext<C>): Boolean =
    hasUnresolvedArguments(context.trace.bindingContext, context.statementFilter)

fun Call.hasUnresolvedArguments(bindingContext: BindingContext, statementFilter: StatementFilter): Boolean {
    val arguments = valueArguments.map { it.getArgumentExpression() }
    return arguments.any(fun(argument: KtExpression?): Boolean {
        if (argument == null || ArgumentTypeResolver.isFunctionLiteralOrCallableReference(argument, statementFilter)) return false

        when (val resolvedCall = argument.getResolvedCall(bindingContext)) {
            is MutableResolvedCall<*> -> if (!resolvedCall.hasInferredReturnType()) return false
            is NewResolvedCallImpl<*> -> if (resolvedCall.resultingDescriptor.returnType?.isError == true) return false
        }

        val expressionType = bindingContext.getType(argument)
        return expressionType == null || expressionType.isError
    })
}

fun Call.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

fun KtCallElement.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

fun Call.getValueArgumentListOrElement(): KtElement =
    if (this is CallTransformer.CallForImplicitInvoke) {
        outerCall.getValueArgumentListOrElement()
    } else {
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

    if (element is KtWhenExpression) {
        val subjectVariable = element.subjectVariable
        if (subjectVariable != null) {
            return subjectVariable.getCall(context) ?: context[CALL, element]
        }
    }

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
    val callExpressionTypes = arrayOf(
        KtSimpleNameExpression::class.java, KtCallElement::class.java, KtBinaryExpression::class.java,
        KtUnaryExpression::class.java, KtArrayAccessExpression::class.java
    )

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

val PsiElement.isFakePsiElement: Boolean
    get() = this is KtElement && isFakeElement

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
    return callElement.isCallableReference()
}

fun PsiElement.isCallableReference(): Boolean =
    this is KtNameReferenceExpression && (parent as? KtCallableReferenceExpression)?.callableReference == this

fun PsiElement.asCallableReferenceExpression(): KtCallableReferenceExpression? =
    when {
        isCallableReference() -> parent as KtCallableReferenceExpression
        this is KtCallableReferenceExpression -> this
        else -> null
    }

fun Call.createLookupLocation(): KotlinLookupLocation {
    val calleeExpression = calleeExpression
    val element =
        if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
        else callElement
    return KotlinLookupLocation(element)
}

fun KtExpression.createLookupLocation(): KotlinLookupLocation? =
    if (!isFakeElement) KotlinLookupLocation(this) else null

fun ResolvedCall<*>.getFirstArgumentExpression(): KtExpression? =
    valueArgumentsByIndex?.run { get(0).arguments[0].getArgumentExpression() }

fun ResolvedCall<*>.getReceiverExpression(): KtExpression? =
    (extensionReceiver as? ExpressionReceiver)?.expression ?: (dispatchReceiver as? ExpressionReceiver)?.expression

val KtLambdaExpression.isTrailingLambdaOnNewLIne
    get(): Boolean {
        (parent as? KtLambdaArgument)?.let { lambdaArgument ->
            var prevSibling = lambdaArgument.prevSibling

            while (prevSibling != null && prevSibling !is KtElement) {
                if (prevSibling is PsiWhiteSpace && prevSibling.textContains('\n'))
                    return true
                prevSibling = prevSibling.prevSibling
            }
        }

        return false
    }


inline fun BindingTrace.reportTrailingLambdaErrorOr(
    expression: KtExpression?,
    originalDiagnostic: (KtExpression) -> Diagnostic
) {
    expression?.let { expr ->
        if (expr is KtLambdaExpression && expr.isTrailingLambdaOnNewLIne) {
            report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(expr))
        } else {
            report(originalDiagnostic(expr))
        }
    }
}

fun NewTypeSubstitutor.toOldSubstitution(): TypeSubstitution = object : TypeSubstitution() {
    override fun get(key: KotlinType): TypeProjection? {
        return safeSubstitute(key.unwrap()).takeIf { it !== key }?.asTypeProjection()
    }

    override fun isEmpty(): Boolean {
        return isEmpty
    }
}

fun <D : CallableDescriptor> ResolvedCallImpl<D>.shouldBeSubstituteWithStubTypes() =
    typeArguments.any { argument -> argument.value.contains { it is StubTypeForBuilderInference } }
            || dispatchReceiver?.type?.contains { it is StubTypeForBuilderInference } == true
            || extensionReceiver?.type?.contains { it is StubTypeForBuilderInference } == true
            || valueArguments.any { argument -> argument.key.type.contains { it is StubTypeForBuilderInference } }

fun KotlinCall.extractCallableReferenceExpression(): KtCallableReferenceExpression? =
    psiKotlinCall.psiCall.extractCallableReferenceExpression()

fun Call.extractCallableReferenceExpression(): KtCallableReferenceExpression? = callElement.asCallableReferenceExpression()
