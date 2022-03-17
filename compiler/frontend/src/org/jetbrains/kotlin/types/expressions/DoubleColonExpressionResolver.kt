/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.checkReservedYield
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.expressions.FunctionWithBigAritySupport.LanguageVersionDependent
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.util.*
import javax.inject.Inject

sealed class DoubleColonLHS(val type: KotlinType) {
    /**
     * [isObjectQualifier] is true iff the LHS of a callable reference is a qualified expression which references a named object.
     * Note that such LHS can be treated both as a type and as an expression, so special handling may be required.
     *
     * For example, if `Obj` is an object:
     *
     *     Obj::class         // object qualifier
     *     test.Obj::class    // object qualifier
     *     (Obj)::class       // not an object qualifier (can only be treated as an expression, not as a type)
     *     { Obj }()::class   // not an object qualifier
     */
    class Expression(val typeInfo: KotlinTypeInfo, val isObjectQualifier: Boolean) : DoubleColonLHS(typeInfo.type!!) {
        val dataFlowInfo: DataFlowInfo = typeInfo.dataFlowInfo
    }

    class Type(type: KotlinType, val possiblyBareType: PossiblyBareType) : DoubleColonLHS(type)
}

@TypeRefinement
private fun KotlinTypeRefiner.refineBareType(type: PossiblyBareType): PossiblyBareType {
    if (type.isBare) return type
    val newType = type.actualType.let { refineType(it) }
    return PossiblyBareType.type(newType)
}

// Returns true if this expression has the form "A<B>" which means it's a type on the LHS of a double colon expression
internal val KtCallExpression.isWithoutValueArguments: Boolean
    get() = valueArgumentList == null && lambdaArguments.isEmpty()

class DoubleColonExpressionResolver(
    val callResolver: CallResolver,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val dataFlowAnalyzer: DataFlowAnalyzer,
    val reflectionTypes: ReflectionTypes,
    val typeResolver: TypeResolver,
    val languageVersionSettings: LanguageVersionSettings,
    val additionalCheckers: Iterable<ClassLiteralChecker>,
    val dataFlowValueFactory: DataFlowValueFactory,
    val bigAritySupport: FunctionWithBigAritySupport,
    val genericArrayClassLiteralSupport: GenericArrayClassLiteralSupport,
    val kotlinTypeRefiner: KotlinTypeRefiner
) {
    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    fun visitClassLiteralExpression(expression: KtClassLiteralExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        if (expression.isEmptyLHS) {
            // "::class" will maybe mean "this::class", a class of "this" instance
            c.trace.report(UNSUPPORTED.on(expression, "Class literals with empty left hand side are not yet supported"))
        } else {
            val result = resolveDoubleColonLHS(expression, c)

            if (c.inferenceSession is BuilderInferenceSession && result?.type?.contains { it is StubTypeForBuilderInference } == true) {
                c.inferenceSession.addOldCallableReferenceCalls(expression)
            }

            if (result != null && !result.type.isError) {
                val inherentType = result.type
                val dataFlowInfo = (result as? DoubleColonLHS.Expression)?.dataFlowInfo ?: c.dataFlowInfo
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(expression.receiverExpression!!, inherentType, c)
                val type =
                    if (!dataFlowInfo.getStableNullability(dataFlowValue).canBeNull()) inherentType.makeNotNullable()
                    else inherentType
                checkClassLiteral(c, expression, type, result)
                val variance =
                    if (result is DoubleColonLHS.Expression && !result.isObjectQualifier) Variance.OUT_VARIANCE else Variance.INVARIANT
                val kClassType = reflectionTypes.getKClassType(Annotations.EMPTY, type, variance)
                return dataFlowAnalyzer.checkType(createTypeInfo(kClassType, dataFlowInfo), expression, c)
            }
        }

        return createTypeInfo(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, expression.toString()), c)
    }

    private fun checkClassLiteral(
        c: ExpressionTypingContext,
        expression: KtClassLiteralExpression,
        type: KotlinType,
        result: DoubleColonLHS
    ) {
        if (result is DoubleColonLHS.Expression) {
            if (!result.isObjectQualifier) {
                if (!type.isSubtypeOf(type.builtIns.anyType)) {
                    c.trace.report(EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS.on(expression.receiverExpression!!, type))
                }
                reportUnsupportedIfNeeded(expression, c)
            }
            return
        }

        result as DoubleColonLHS.Type
        val descriptor = type.constructor.declarationDescriptor
        if (result.possiblyBareType.isBare) {
            if (descriptor is ClassDescriptor && KotlinBuiltIns.isNonPrimitiveArray(descriptor) &&
                !languageVersionSettings.supportsFeature(LanguageFeature.BareArrayClassLiteral)
            ) {
                c.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression))
            }
        }

        if (type is SimpleType && !type.isMarkedNullable && descriptor is TypeParameterDescriptor && !descriptor.isReified) {
            c.trace.report(TYPE_PARAMETER_AS_REIFIED.on(expression, descriptor))
        }
        // Note that "T::class" is allowed for type parameter T without a non-null upper bound
        else if ((TypeUtils.isNullableType(type) && descriptor !is TypeParameterDescriptor) || expression.hasQuestionMarks) {
            c.trace.report(NULLABLE_TYPE_IN_CLASS_LITERAL_LHS.on(expression))
        } else if (!result.possiblyBareType.isBare && !isAllowedInClassLiteral(type)) {
            c.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression))
        }
        for (additionalChecker in additionalCheckers) {
            additionalChecker.check(expression, type, c)
        }
    }

    // Returns true if the expression is not a call expression without value arguments (such as "A<B>") or a qualified expression
    // which contains such call expression as one of its parts.
    // In this case it's pointless to attempt to type check an expression on the LHS in "A<B>::class", since "A<B>" certainly means a type.
    private fun KtExpression.canBeConsideredProperExpression(): Boolean {
        return when (this) {
            is KtCallExpression ->
                !isWithoutValueArguments
            is KtDotQualifiedExpression ->
                receiverExpression.canBeConsideredProperExpression() &&
                        selectorExpression?.canBeConsideredProperExpression() ?: false
            else -> true
        }
    }

    private fun KtExpression.canBeConsideredProperType(): Boolean {
        return when (this) {
            is KtSimpleNameExpression ->
                true
            is KtCallExpression ->
                isWithoutValueArguments
            is KtDotQualifiedExpression ->
                receiverExpression.canBeConsideredProperType() && selectorExpression.let { it != null && it.canBeConsideredProperType() }
            else -> false
        }
    }

    private fun shouldTryResolveLHSAsExpression(expression: KtDoubleColonExpression): Boolean {
        val lhs = expression.receiverExpression ?: return false
        return lhs.canBeConsideredProperExpression() && !expression.hasQuestionMarks /* TODO: test this */
    }

    private fun shouldTryResolveLHSAsType(expression: KtDoubleColonExpression): Boolean {
        val lhs = expression.receiverExpression
        return lhs != null && lhs.canBeConsideredProperType()
    }

    private fun reportUnsupportedIfNeeded(expression: KtDoubleColonExpression, c: ExpressionTypingContext) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences)) {
            c.trace.report(
                UNSUPPORTED_FEATURE.on(
                    expression.receiverExpression!!,
                    LanguageFeature.BoundCallableReferences to languageVersionSettings
                )
            )
        }
    }

    private fun shouldTryResolveLHSAsReservedExpression(expression: KtDoubleColonExpression): Boolean {
        val lhs = expression.receiverExpression ?: return false
        return (expression.hasQuestionMarks && lhs.canBeConsideredProperExpression()) ||
                (lhs is KtCallExpression && lhs.canBeReservedGenericPropertyCall())
    }

    private fun KtExpression.getQualifierChainParts(): List<KtExpression>? {
        if (this !is KtQualifiedExpression) return listOf(this)

        val result = ArrayDeque<KtExpression>()
        var finger: KtQualifiedExpression = this
        while (true) {
            if (finger.operationSign != KtTokens.DOT) return null

            finger.selectorExpression?.let { result.push(it) }

            val receiver = finger.receiverExpression
            if (receiver is KtQualifiedExpression) {
                finger = receiver
            } else {
                result.push(receiver)
                return result.toList()
            }
        }
    }

    private fun shouldTryResolveLHSAsReservedCallChain(expression: KtDoubleColonExpression): Boolean {
        val lhs = (expression.receiverExpression as? KtQualifiedExpression) ?: return false
        val parts = lhs.getQualifierChainParts() ?: return false
        return parts.all { it.canBeReservedGenericPropertyCall() } &&
                parts.any { it is KtCallExpression && it.typeArguments.isNotEmpty() }
    }

    private fun KtExpression?.canBeReservedGenericPropertyCall(): Boolean =
        getQualifiedNameStringPart() != null

    private fun KtExpression?.getQualifiedNameStringPart(): String? =
        when (this) {
            is KtNameReferenceExpression ->
                text
            is KtCallExpression ->
                if (valueArguments.isEmpty() && typeArguments.isNotEmpty())
                    (calleeExpression as? KtNameReferenceExpression)?.text
                else
                    null
            else ->
                null
        }

    private fun KtQualifiedExpression.buildNewExpressionForReservedGenericPropertyCallChainResolution(): KtExpression? {
        val parts = this.getQualifierChainParts()?.map { it.getQualifiedNameStringPart() ?: return null } ?: return null
        val qualifiedExpressionText = parts.joinToString(separator = ".")
        return KtPsiFactory(this, markGenerated = false).createExpression(qualifiedExpressionText)
    }

    private fun resolveReservedExpressionOnLHS(expression: KtExpression, c: ExpressionTypingContext): DoubleColonLHS.Expression? {
        val doubleColonExpression = expression.parent as? KtDoubleColonExpression ?: return null // should assert here?

        if (expression is KtCallExpression && expression.typeArguments.isNotEmpty()) {
            val callee = expression.calleeExpression ?: return null
            val calleeAsDoubleColonLHS = resolveExpressionOnLHS(callee, c) ?: return null

            for (typeArgument in expression.typeArguments) {
                val typeReference = typeArgument.typeReference ?: continue
                typeResolver.resolveType(c.scope, typeReference, c.trace, true)
            }

            return calleeAsDoubleColonLHS
        } else if (doubleColonExpression.hasQuestionMarks) {
            return resolveExpressionOnLHS(expression, c)
        } else {
            return null
        }
    }

    private fun resolveReservedCallChainOnLHS(expression: KtExpression, c: ExpressionTypingContext): DoubleColonLHS.Expression? {
        if (expression !is KtQualifiedExpression) return null

        val newExpression = expression.buildNewExpressionForReservedGenericPropertyCallChainResolution() ?: return null

        val temporaryTraceAndCache =
            TemporaryTraceAndCache.create(c, "resolve reserved generic property call chain in '::' LHS", newExpression)
        val contextForCallChainResolution =
            c.replaceTraceAndCache(temporaryTraceAndCache)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)

        return resolveExpressionOnLHS(expression, contextForCallChainResolution)
    }

    private fun resolveReservedExpressionSyntaxOnDoubleColonLHS(doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext):
            ReservedDoubleColonLHSResolutionResult {
        val resultForReservedExpr = tryResolveLHS(
            doubleColonExpression, c,
            this::shouldTryResolveLHSAsReservedExpression,
            this::resolveReservedExpressionOnLHS
        )
        if (resultForReservedExpr != null) {
            val lhs = resultForReservedExpr.lhs
            if (lhs != null) {
                c.trace.report(RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS.on(resultForReservedExpr.expression))
                return ReservedDoubleColonLHSResolutionResult(true, resultForReservedExpr.commit(), resultForReservedExpr.traceAndCache)
            }
        }

        val resultForReservedCallChain = tryResolveLHS(
            doubleColonExpression, c,
            this::shouldTryResolveLHSAsReservedCallChain,
            this::resolveReservedCallChainOnLHS
        )
        if (resultForReservedCallChain != null) {
            val lhs = resultForReservedCallChain.lhs
            if (lhs != null) {
                c.trace.report(RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS.on(resultForReservedCallChain.expression))
                // DO NOT commit trace from resultForReservedCallChain here
                return ReservedDoubleColonLHSResolutionResult(true, null, resultForReservedExpr?.traceAndCache)
            }
        }

        return ReservedDoubleColonLHSResolutionResult(
            false, null, resultForReservedExpr?.traceAndCache ?: resultForReservedCallChain?.traceAndCache
        )
    }

    internal fun resolveDoubleColonLHS(doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext): DoubleColonLHS? {
        val resultForExpr = tryResolveLHS(doubleColonExpression, c, this::shouldTryResolveLHSAsExpression, this::resolveExpressionOnLHS)
        if (resultForExpr != null) {
            val lhs = resultForExpr.lhs
            // If expression result is an object, we remember this and skip it here, because there are valid situations where
            // another type (representing another classifier) should win
            if (lhs != null && !lhs.isObjectQualifier) {
                return resultForExpr.commit()
            }
        }

        val (isReservedExpressionSyntax, doubleColonLHS, traceAndCacheFromReservedDoubleColonLHS) =
            resolveReservedExpressionSyntaxOnDoubleColonLHS(doubleColonExpression, c)

        if (isReservedExpressionSyntax) return doubleColonLHS

        val resultForType = tryResolveLHS(doubleColonExpression, c, this::shouldTryResolveLHSAsType) { expression, context ->
            resolveTypeOnLHS(expression, doubleColonExpression, context)?.let {
                // If lhs is not expression then ExpressionTypingVisitor don't refine it's type and we should do this manually
                @OptIn(TypeRefinement::class)
                DoubleColonLHS.Type(kotlinTypeRefiner.refineType(it.type), kotlinTypeRefiner.refineBareType(it.possiblyBareType))
            }
        }
        if (resultForType != null) {
            val lhs = resultForType.lhs
            if (resultForExpr != null && lhs != null && lhs.type == resultForExpr.lhs?.type) {
                // If we skipped an object expression result before and the type result is the same, this means that
                // there were no other classifier except that object that could win. We prefer to treat the LHS as an expression here,
                // to have a bound callable reference / class literal
                return resultForExpr.commit()
            }
            if (lhs != null) {
                return resultForType.commit()
            }
        }

        if (resultForExpr != null) return resultForExpr.commit()
        if (resultForType != null) return resultForType.commit()

        /*
         * If the LHS could be resolved neither as an expression nor as a type,
         * but it was resolved as expression with reserved syntax like `foo?::bar?::bar`,
         * then we commit the trace of that resolution result.
         */
        traceAndCacheFromReservedDoubleColonLHS?.commit()

        return null
    }

    private data class ReservedDoubleColonLHSResolutionResult(
        val isReservedExpressionSyntax: Boolean,
        val lhs: DoubleColonLHS?,
        val traceAndCache: TemporaryTraceAndCache?
    )

    private class LHSResolutionResult<out T : DoubleColonLHS>(
        val lhs: T?,
        val expression: KtExpression,
        val traceAndCache: TemporaryTraceAndCache
    ) {
        fun commit(): T? {
            if (lhs != null) {
                traceAndCache.trace.record(BindingContext.DOUBLE_COLON_LHS, expression, lhs)
            }
            traceAndCache.commit()
            return lhs
        }
    }

    /**
     * Returns null if the LHS is definitely not an expression. Returns a non-null result if a resolution was attempted and led to
     * either a successful result or not.
     */
    private fun <T : DoubleColonLHS> tryResolveLHS(
        doubleColonExpression: KtDoubleColonExpression,
        context: ExpressionTypingContext,
        criterion: (KtDoubleColonExpression) -> Boolean,
        resolve: (KtExpression, ExpressionTypingContext) -> T?
    ): LHSResolutionResult<T>? {
        val expression = doubleColonExpression.receiverExpression ?: return null

        if (!criterion(doubleColonExpression)) return null

        val traceAndCache = TemporaryTraceAndCache.create(context, "resolve '::' LHS", doubleColonExpression)
        val c = context
            .replaceTraceAndCache(traceAndCache)
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        val lhs = resolve(expression, c)
        return LHSResolutionResult(lhs, expression, traceAndCache)
    }

    private fun resolveExpressionOnLHS(expression: KtExpression, c: ExpressionTypingContext): DoubleColonLHS.Expression? {
        val typeInfo = expressionTypingServices.getTypeInfo(expression, c)

        // TODO: do not lose data flow info maybe
        if (typeInfo.type == null) return null

        // Be careful not to call a utility function to get a resolved call by an expression which may accidentally
        // deparenthesize that expression, as this is undesirable here
        val call = c.trace.bindingContext[BindingContext.CALL, expression.getQualifiedElementSelector()]
        val resolvedCall = call.getResolvedCall(c.trace.bindingContext)

        if (resolvedCall != null) {
            val resultingDescriptor = resolvedCall.resultingDescriptor
            if (resultingDescriptor is FakeCallableDescriptorForObject) {
                val classDescriptor = resultingDescriptor.classDescriptor
                if (classDescriptor.companionObjectDescriptor != null) return null

                if (DescriptorUtils.isObject(classDescriptor) ||
                    (!languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences) &&
                            DescriptorUtils.isEnumEntry(classDescriptor))) {
                    return DoubleColonLHS.Expression(typeInfo, isObjectQualifier = true)
                }
            }

            // Check if this is resolved to a function (with the error "arguments expected"), such as in "Runnable::class"
            if (expression.canBeConsideredProperType() && resultingDescriptor !is VariableDescriptor) return null
        }

        return DoubleColonLHS.Expression(typeInfo, isObjectQualifier = false)
    }

    private fun resolveTypeOnLHS(
        expression: KtExpression, doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext
    ): DoubleColonLHS.Type? {
        val qualifierResolutionResult =
            qualifiedExpressionResolver.resolveDescriptorForDoubleColonLHS(expression, c.scope, c.trace, c.isDebuggerContext)

        val typeResolutionContext = TypeResolutionContext(
            c.scope, c.trace, /* checkBounds = */ true, /* allowBareTypes = */ true,
            /* isDebuggerContext = */ expression.suppressDiagnosticsInDebugMode() /* TODO: test this */
        )

        val classifier = qualifierResolutionResult.classifierDescriptor
        if (classifier == null) {
            typeResolver.resolveTypeProjections(
                typeResolutionContext, ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, expression.text).constructor, qualifierResolutionResult.allProjections
            )
            return null
        }

        val possiblyBareType = typeResolver.resolveTypeForClassifier(
            typeResolutionContext, classifier, qualifierResolutionResult, expression, Annotations.EMPTY
        )

        val type = if (possiblyBareType.isBare) {
            val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor as? ClassDescriptor
                    ?: error("Only classes can produce bare types: $possiblyBareType")

            if (doubleColonExpression is KtCallableReferenceExpression) {
                c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(expression, descriptor.typeConstructor.parameters.size, descriptor))
            }

            val arguments = descriptor.typeConstructor.parameters.map(TypeUtils::makeStarProjection)
            KotlinTypeFactory.simpleType(
                TypeAttributes.Empty, descriptor.typeConstructor, arguments,
                possiblyBareType.isNullable || doubleColonExpression.hasQuestionMarks
            )
        } else {
            val actualType = possiblyBareType.actualType
            if (doubleColonExpression.hasQuestionMarks) actualType.makeNullable() else actualType
        }

        return DoubleColonLHS.Type(type, possiblyBareType)
    }

    private fun isAllowedInClassLiteral(type: KotlinType): Boolean {
        when (val descriptor = type.constructor.declarationDescriptor) {
            is ClassDescriptor -> {
                if (genericArrayClassLiteralSupport.isEnabled ||
                    !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitGenericArrayClassLiteral)
                ) {
                    if (KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                        return type.arguments.none { typeArgument ->
                            typeArgument.isStarProjection || !isAllowedInClassLiteral(typeArgument.type)
                        }
                    }
                }

                return type.arguments.isEmpty()
            }
            is TypeParameterDescriptor -> return descriptor.isReified
            else -> return false
        }
    }

    fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        val callableReference = expression.callableReference
        if (callableReference.getReferencedName().isEmpty()) {
            if (!expression.isEmptyLHS) resolveDoubleColonLHS(expression, c)
            c.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference))
            val errorType = ErrorUtils.createErrorType(ErrorTypeKind.EMPTY_CALLABLE_REFERENCE)
            return dataFlowAnalyzer.createCheckedTypeInfo(errorType, c, expression)
        }

        val (lhs, resolutionResults) = resolveCallableReference(expression, c, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS)
        val result = getCallableReferenceType(expression, lhs, resolutionResults, c)
        val doesSomeExtensionReceiverContainsStubType =
            resolutionResults != null && resolutionResults.resultingCalls.any { resolvedCall ->
                resolvedCall.extensionReceiver?.type?.contains { it is StubTypeForBuilderInference } == true
            }

        val unrestrictedBuilderInferenceSupported = languageVersionSettings.supportsFeature(LanguageFeature.UnrestrictedBuilderInference)

        if (doesSomeExtensionReceiverContainsStubType && !unrestrictedBuilderInferenceSupported) {
            c.trace.reportDiagnosticOnce(TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE.on(expression))
            return noTypeInfo(c)
        }

        val dataFlowInfo = (lhs as? DoubleColonLHS.Expression)?.dataFlowInfo ?: c.dataFlowInfo

        if (c.inferenceSession is BuilderInferenceSession && result?.contains { it is StubTypeForBuilderInference } == true) {
            c.inferenceSession.addOldCallableReferenceCalls(expression)
        }

        return dataFlowAnalyzer.checkType(createTypeInfo(result, dataFlowInfo), expression, c)
    }

    private fun getCallableReferenceType(
        expression: KtCallableReferenceExpression,
        lhs: DoubleColonLHS?,
        resolutionResults: OverloadResolutionResults<*>?,
        context: ExpressionTypingContext
    ): KotlinType? {
        val descriptor =
            if (resolutionResults != null && !resolutionResults.isNothing) {
                val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(resolutionResults, context)
                resolvedCall?.resultingDescriptor ?: return null
            } else {
                if (lhs != null || expression.isEmptyLHS) {
                    context.trace.report(UNRESOLVED_REFERENCE.on(expression.callableReference, expression.callableReference))
                }
                return null
            }

        checkReferenceIsToAllowedMember(descriptor, context.trace, expression)

        val scope = context.scope.ownerDescriptor
        val type = createKCallableTypeForReference(descriptor, lhs, reflectionTypes, scope) ?: return null

        when (descriptor) {
            is FunctionDescriptor -> bindFunctionReference(expression, type, context, descriptor)
            is PropertyDescriptor -> bindPropertyReference(expression, type, context, isMutablePropertyReference(descriptor, lhs, scope))
        }

        return type
    }

    internal fun checkReferenceIsToAllowedMember(
        descriptor: CallableDescriptor, trace: BindingTrace, expression: KtCallableReferenceExpression
    ) {
        val simpleName = expression.callableReference
        if (!languageVersionSettings.supportsFeature(LanguageFeature.CallableReferencesToClassMembersWithEmptyLHS)) {
            if (expression.isEmptyLHS &&
                (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) {
                trace.report(
                    UNSUPPORTED_FEATURE.on(
                        simpleName, LanguageFeature.CallableReferencesToClassMembersWithEmptyLHS to languageVersionSettings
                    )
                )
            }
        }
        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(descriptor.containingDeclaration)) {
            trace.report(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR.on(simpleName))
        }
        if (descriptor is CallableMemberDescriptor && isMemberExtension(descriptor)) {
            trace.report(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED.on(simpleName, descriptor))
        }
        if (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor) {
            trace.report(UNSUPPORTED.on(simpleName, "References to variables aren't supported yet"))
        }
    }

    private fun isMemberExtension(descriptor: CallableMemberDescriptor): Boolean {
        val original = (descriptor as? ImportedFromObjectCallableDescriptor<*>)?.callableFromObject ?: descriptor
        return original.extensionReceiverParameter != null && original.dispatchReceiverParameter != null
    }

    internal fun bindFunctionReference(
        expression: KtCallableReferenceExpression,
        type: KotlinType,
        context: ResolutionContext<*>,
        referencedFunction: FunctionDescriptor
    ) {
        val functionDescriptor = AnonymousFunctionDescriptor(
            context.scope.ownerDescriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            expression.toSourceElement(),
            /* isCoroutine = */ ReflectionTypes.isNumberedKSuspendFunction(type) || referencedFunction.isSuspend
        )

        functionDescriptor.initialize(
            null, null, emptyList(), emptyList(),
            createValueParametersForInvokeInFunctionType(functionDescriptor, type.arguments.dropLast(1)),
            type.arguments.last().type,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )

        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor)

        if (functionDescriptor.valueParameters.size >= BuiltInFunctionArity.BIG_ARITY &&
            bigAritySupport.shouldCheckLanguageVersionSettings &&
            !languageVersionSettings.supportsFeature(LanguageFeature.FunctionTypesWithBigArity)
        ) {
            context.trace.report(
                UNSUPPORTED_FEATURE.on(expression, LanguageFeature.FunctionTypesWithBigArity to languageVersionSettings)
            )
        }
    }

    internal fun bindPropertyReference(
        expression: KtCallableReferenceExpression,
        referenceType: KotlinType,
        context: ResolutionContext<*>,
        mutable: Boolean = true
    ) {
        val localVariable = LocalVariableDescriptor(
            context.scope.ownerDescriptor, Annotations.EMPTY, SpecialNames.ANONYMOUS, referenceType,
            mutable, false, expression.toSourceElement()
        )

        context.trace.record(BindingContext.VARIABLE, expression, localVariable)
    }

    fun resolveCallableReference(
        expression: KtCallableReferenceExpression,
        context: ExpressionTypingContext,
        resolveArgumentsMode: ResolveArgumentsMode
    ): Pair<DoubleColonLHS?, OverloadResolutionResults<*>?> {
        val lhsResult =
            if (expression.isEmptyLHS) null
            else resolveDoubleColonLHS(expression, context)

        val resolutionResults = resolveCallableReferenceRHS(expression, lhsResult, context, resolveArgumentsMode)

        reportUnsupportedCallableReferenceIfNeeded(expression, context, lhsResult, resolutionResults)

        return lhsResult to resolutionResults
    }

    private fun reportUnsupportedCallableReferenceIfNeeded(
        expression: KtCallableReferenceExpression,
        context: ExpressionTypingContext,
        lhsResult: DoubleColonLHS?,
        resolutionResults: OverloadResolutionResults<CallableDescriptor>?
    ) {
        val descriptor =
            if (resolutionResults?.isSingleResult == true) resolutionResults.resultingDescriptor else null
        if (descriptor is PropertyDescriptor && descriptor.isBuiltInCoroutineContext()) {
            context.trace.report(UNSUPPORTED.on(expression.callableReference, "Callable reference to suspend property"))
        }

        val expressionResult = lhsResult as? DoubleColonLHS.Expression ?: return
        // "<expr>::foo" was not supported without bound callable references, except the case of a nested class constructor in an object
        if (!expressionResult.isObjectQualifier || descriptor !is ConstructorDescriptor) {
            reportUnsupportedIfNeeded(expression, context)
        }
    }

    private class ResolutionResultsAndTraceCommitCallback(
        val results: OverloadResolutionResults<CallableDescriptor>,
        val commitTrace: () -> Unit
    )

    private fun tryResolveRHSWithReceiver(
        traceTitle: String,
        receiver: Receiver?,
        reference: KtSimpleNameExpression,
        outerContext: ResolutionContext<*>,
        resolutionMode: ResolveArgumentsMode,
        lhs: DoubleColonLHS?
    ): ResolutionResultsAndTraceCommitCallback? {
        // we should preserve information about `call` because callable references are analyzed two times,
        // otherwise there will be not completed calls in trace
        val call =
            outerContext.trace[BindingContext.CALL, reference] ?: CallMaker.makeCall(reference, receiver, null, reference, emptyList())
        val temporaryTrace = TemporaryTraceAndCache.create(outerContext, traceTitle, reference)
        val newContext =
            if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS)
                outerContext
                    .replaceTraceAndCache(temporaryTrace)
                    .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                    .replaceContextDependency(ContextDependency.DEPENDENT)
            else
                outerContext.replaceTraceAndCache(temporaryTrace)

        val resolutionResults = callResolver.resolveCallForMember(
            reference,
            BasicCallResolutionContext.create(
                newContext.replaceCallPosition(CallPosition.CallableReferenceRhs(lhs)),
                call,
                CheckArgumentTypesMode.CHECK_CALLABLE_TYPE
            )
        )

        return when {
            resolutionResults.isNothing -> null
            else -> ResolutionResultsAndTraceCommitCallback(resolutionResults) {
                if (!languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
                    checkReservedYield(reference, outerContext.trace)
                }
                if (resolutionMode != ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS || resolutionResults.isSuccess) {
                    temporaryTrace.commit()
                }
            }
        }
    }

    private fun resolveCallableReferenceRHS(
        expression: KtCallableReferenceExpression,
        lhs: DoubleColonLHS?,
        c: ResolutionContext<*>,
        mode: ResolveArgumentsMode
    ): OverloadResolutionResults<CallableDescriptor>? {
        val reference = expression.callableReference

        val lhsType = lhs?.type
        if (lhsType == null) {
            if (!expression.isEmptyLHS) return null

            return tryResolveRHSWithReceiver("resolve callable reference with empty LHS", null, reference, c, mode, lhs)
                ?.apply { commitTrace() }?.results
        }

        val resultSequence = sequence {
            when (lhs) {
                is DoubleColonLHS.Type -> {
                    val classifier = lhsType.constructor.declarationDescriptor
                    if (classifier !is ClassDescriptor) {
                        c.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(expression))
                        return@sequence
                    }

                    val qualifier = c.trace.get(BindingContext.QUALIFIER, expression.receiverExpression!!)
                    if (qualifier is ClassQualifier) {
                        yieldIfNotNull(
                            tryResolveRHSWithReceiver(
                                "resolve unbound callable reference in static scope", qualifier, reference, c, mode, lhs
                            )
                        )
                    }

                    yieldIfNotNull(
                        tryResolveRHSWithReceiver(
                            "resolve unbound callable reference with receiver", TransientReceiver(lhsType), reference, c, mode, lhs
                        )
                    )
                }
                is DoubleColonLHS.Expression -> {
                    val expressionReceiver = ExpressionReceiver.create(expression.receiverExpression!!, lhsType, c.trace.bindingContext)
                    yieldIfNotNull(
                        tryResolveRHSWithReceiver(
                            "resolve bound callable reference", expressionReceiver, reference, c, mode, lhs
                        )
                    )

                    if (lhs.isObjectQualifier) {
                        val classifier = lhsType.constructor.declarationDescriptor
                        val calleeExpression = expression.receiverExpression?.getCalleeExpressionIfAny()
                        if (calleeExpression is KtSimpleNameExpression && classifier is ClassDescriptor) {
                            val qualifier = ClassQualifier(calleeExpression, classifier)
                            yieldIfNotNull(
                                tryResolveRHSWithReceiver(
                                    "resolve object callable reference in static scope", qualifier, reference, c, mode, lhs
                                )
                            )
                        }
                    }
                }
            }
        }

        // TODO: Maybe it makes sense to report all results when all of them are unsuccessful (NONE_APPLICABLE or something like this)
        var resultToCommit: ResolutionResultsAndTraceCommitCallback? = null
        for (result in resultSequence) {
            resultToCommit = result
            if (result.results.isSuccess) {
                break
            }
        }
        return resultToCommit?.let {
            it.commitTrace()
            it.results
        }
    }

    companion object {
        private fun contextReceiverTypesFor(descriptor: CallableDescriptor): List<KotlinType> =
            descriptor.contextReceiverParameters.map { it.type }

        private fun receiverTypeFor(descriptor: CallableDescriptor, lhs: DoubleColonLHS?): KotlinType? =
            (descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter)?.let { (lhs as? DoubleColonLHS.Type)?.type }

        private fun isMutablePropertyReference(
            descriptor: PropertyDescriptor,
            lhs: DoubleColonLHS?,
            scopeOwnerDescriptor: DeclarationDescriptor
        ): Boolean {
            val receiver = receiverTypeFor(descriptor, lhs)?.let(::TransientReceiver)
            val setter = descriptor.setter
            return descriptor.isVar && (setter == null || DescriptorVisibilities.isVisible(receiver, setter, scopeOwnerDescriptor, false))
        }

        fun createKCallableTypeForReference(
            descriptor: CallableDescriptor,
            lhs: DoubleColonLHS?,
            reflectionTypes: ReflectionTypes,
            scopeOwnerDescriptor: DeclarationDescriptor
        ): KotlinType? {
            val contextReceiverTypes = contextReceiverTypesFor(descriptor)
            val receiverType = receiverTypeFor(descriptor, lhs)
            return when (descriptor) {
                is FunctionDescriptor -> {
                    val returnType = descriptor.returnType ?: return null
                    val parametersTypes = descriptor.valueParameters.map { it.type }
                    val parametersNames = descriptor.valueParameters.map { it.name }
                    return reflectionTypes.getKFunctionType(
                        Annotations.EMPTY, receiverType,
                        contextReceiverTypes, parametersTypes, parametersNames, returnType, descriptor.builtIns, descriptor.isSuspend
                    )
                }
                is PropertyDescriptor -> {
                    val mutable = isMutablePropertyReference(descriptor, lhs, scopeOwnerDescriptor)
                    reflectionTypes.getKPropertyType(Annotations.EMPTY, listOfNotNull(receiverType), descriptor.type, mutable)
                }
                is VariableDescriptor -> null
                else -> throw UnsupportedOperationException("Callable reference resolved to an unsupported descriptor: $descriptor")
            }
        }
    }
}

/**
 * By default, function types with big arity are enabled. On platforms where they are not supported by default (e.g. JVM),
 * [LanguageVersionDependent] should be used which makes the code check if the corresponding language feature is enabled.
 */
@DefaultImplementation(FunctionWithBigAritySupport.Enabled::class)
interface FunctionWithBigAritySupport {
    val shouldCheckLanguageVersionSettings: Boolean

    object Enabled : FunctionWithBigAritySupport {
        override val shouldCheckLanguageVersionSettings: Boolean = false
    }

    object LanguageVersionDependent : FunctionWithBigAritySupport {
        override val shouldCheckLanguageVersionSettings: Boolean = true
    }
}

/**
 * Generic array class literals (`Array<String>::class.java`) are enabled on all platforms until 1.4, and only on JVM since 1.4.
 */
@DefaultImplementation(GenericArrayClassLiteralSupport.Disabled::class)
interface GenericArrayClassLiteralSupport {
    val isEnabled: Boolean

    object Enabled : GenericArrayClassLiteralSupport {
        override val isEnabled: Boolean = true
    }

    object Disabled : GenericArrayClassLiteralSupport {
        override val isEnabled: Boolean = false
    }
}
