/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.K1Deprecation
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

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.noErrorsInValueArguments(): Boolean {
    return call.valueArguments.all { argument -> !getArgumentMapping(argument!!).isError() }
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedArguments(): Boolean {
    return call.valueArguments.any { argument -> getArgumentMapping(argument!!) == ArgumentUnmapped }
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedParameters(): Boolean {
    val parameterToArgumentMap = valueArguments
    return !parameterToArgumentMap.keys.containsAll(resultingDescriptor.valueParameters)
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMapped() =
    call.valueArguments.all { argument -> getArgumentMapping(argument) is ArgumentMatch }

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.hasTypeMismatchErrorOnParameter(parameter: ValueParameterDescriptor): Boolean {
    val resolvedValueArgument = valueArguments[parameter]
    if (resolvedValueArgument == null) return true

    return resolvedValueArgument.arguments.any { argument ->
        val argumentMapping = getArgumentMapping(argument)
        argumentMapping is ArgumentMatch && argumentMapping.status == ArgumentMatchStatus.TYPE_MISMATCH
    }
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.getParameterForArgument(valueArgument: ValueArgument?): ValueParameterDescriptor? {
    return (valueArgument?.let { getArgumentMapping(it) } as? ArgumentMatch)?.valueParameter
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCall<D>.usesDefaultArguments(): Boolean {
    return valueArgumentsByIndex?.any { it is DefaultValueArgument } ?: false
}


// call

@K1Deprecation
fun <C : ResolutionContext<C>> Call.hasUnresolvedArguments(context: ResolutionContext<C>): Boolean =
    hasUnresolvedArguments(context.trace.bindingContext, context.statementFilter)

@K1Deprecation
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

@K1Deprecation
fun Call.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

@K1Deprecation
fun KtCallElement.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()

@K1Deprecation
fun Call.getValueArgumentListOrElement(): KtElement =
    if (this is CallTransformer.CallForImplicitInvoke) {
        outerCall.getValueArgumentListOrElement()
    } else {
        valueArgumentList ?: calleeExpression ?: callElement
    }

@Suppress("UNCHECKED_CAST")
private fun List<ValueArgument?>.filterArgsInParentheses() = filter { it !is KtLambdaArgument } as List<ValueArgument>

@K1Deprecation
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
@K1Deprecation
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

@K1Deprecation
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

@K1Deprecation
fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return context[RESOLVED_CALL, this]
}

@K1Deprecation
fun KtElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return this?.getCall(context)?.getResolvedCall(context)
}

@K1Deprecation
fun KtElement?.getParentResolvedCall(context: BindingContext, strict: Boolean = true): ResolvedCall<out CallableDescriptor>? {
    return this?.getParentCall(context, strict)?.getResolvedCall(context)
}

@K1Deprecation
fun KtElement.getCallWithAssert(context: BindingContext): Call {
    return getCall(context).sure { "No call for ${this.getTextWithLocation()}" }
}

@K1Deprecation
fun KtElement.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.getTextWithLocation()}" }
}

@K1Deprecation
fun Call.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.callElement.getTextWithLocation()}" }
}

@K1Deprecation
fun KtExpression.getFunctionResolvedCallWithAssert(context: BindingContext): ResolvedCall<out FunctionDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is FunctionDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends FunctionDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out FunctionDescriptor>
}

@K1Deprecation
fun KtExpression.getPropertyResolvedCallWithAssert(context: BindingContext): ResolvedCall<out PropertyDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is PropertyDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends PropertyDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out PropertyDescriptor>
}

@K1Deprecation
fun KtExpression.getVariableResolvedCallWithAssert(context: BindingContext): ResolvedCall<out VariableDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.resultingDescriptor is VariableDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends PropertyDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out VariableDescriptor>
}

@K1Deprecation
fun KtExpression.getType(context: BindingContext): KotlinType? {
    val type = context.getType(this)
    if (type != null) return type
    val resolvedCall = this.getResolvedCall(context)
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        return resolvedCall.variableCall.resultingDescriptor.type
    }
    return null
}

@K1Deprecation
val KtElement.isFakeElement: Boolean
    get() {
        // Don't use getContainingKtFile() because in IDE we can get an element with JavaDummyHolder as containing file
        val file = containingFile
        return file is KtFile && file.doNotAnalyze != null
    }

@K1Deprecation
val PsiElement.isFakePsiElement: Boolean
    get() = this is KtElement && isFakeElement

@K1Deprecation
fun Call.isSafeCall(): Boolean {
    if (this is CallTransformer.CallForImplicitInvoke) {
        //implicit safe 'invoke'
        if (outerCall.isSemanticallyEquivalentToSafeCall) {
            return true
        }
    }
    return isSemanticallyEquivalentToSafeCall
}

@K1Deprecation
fun Call.isCallableReference(): Boolean {
    val callElement = callElement
    return callElement.isCallableReference()
}

@K1Deprecation
fun PsiElement.isCallableReference(): Boolean =
    this is KtNameReferenceExpression && (parent as? KtCallableReferenceExpression)?.callableReference == this

@K1Deprecation
fun PsiElement.asCallableReferenceExpression(): KtCallableReferenceExpression? =
    when {
        isCallableReference() -> parent as KtCallableReferenceExpression
        this is KtCallableReferenceExpression -> this
        else -> null
    }

@K1Deprecation
fun Call.createLookupLocation(): KotlinLookupLocation {
    val calleeExpression = calleeExpression
    val element =
        if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
        else callElement
    return KotlinLookupLocation(element)
}

@K1Deprecation
fun KtExpression.createLookupLocation(): KotlinLookupLocation? =
    if (!isFakeElement) KotlinLookupLocation(this) else null

@K1Deprecation
fun ResolvedCall<*>.getFirstArgumentExpression(): KtExpression? =
    valueArgumentsByIndex?.run { get(0).arguments[0].getArgumentExpression() }

@K1Deprecation
fun ResolvedCall<*>.getReceiverExpression(): KtExpression? =
    (extensionReceiver as? ExpressionReceiver)?.expression ?: (dispatchReceiver as? ExpressionReceiver)?.expression

@K1Deprecation
inline fun BindingTrace.reportTrailingLambdaErrorOr(
    expression: KtExpression?,
    originalDiagnostic: (KtExpression) -> Diagnostic
) {
    expression?.let { expr ->
        if (expr is KtLambdaExpression && expr.isTrailingLambdaOnNewLine) {
            report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(expr))
        } else {
            report(originalDiagnostic(expr))
        }
    }
}

@K1Deprecation
fun NewTypeSubstitutor.toOldSubstitution(): TypeSubstitution = object : TypeSubstitution() {
    override fun get(key: KotlinType): TypeProjection? {
        return safeSubstitute(key.unwrap()).takeIf { it !== key }?.asTypeProjection()
    }

    override fun isEmpty(): Boolean {
        return isEmpty
    }
}

@K1Deprecation
fun <D : CallableDescriptor> ResolvedCallImpl<D>.shouldBeSubstituteWithStubTypes() =
    typeArguments.any { argument -> argument.value.contains { it is StubTypeForBuilderInference } }
            || dispatchReceiver?.type?.contains { it is StubTypeForBuilderInference } == true
            || extensionReceiver?.type?.contains { it is StubTypeForBuilderInference } == true
            || valueArguments.any { argument -> argument.key.type.contains { it is StubTypeForBuilderInference } }

@K1Deprecation
fun KotlinCall.extractCallableReferenceExpression(): KtCallableReferenceExpression? =
    psiKotlinCall.psiCall.extractCallableReferenceExpression()

@K1Deprecation
fun Call.extractCallableReferenceExpression(): KtCallableReferenceExpression? = callElement.asCallableReferenceExpression()
