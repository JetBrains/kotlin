/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by  the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.calls.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KtFe10FunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KtFe10VariableLikeSignature
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10ReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtCallResolver
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class KtFe10CallResolver(
    override val analysisSession: KtFe10AnalysisSession
) : AbstractKtCallResolver(), Fe10KtAnalysisSessionComponent {
    private companion object {
        private val operatorWithAssignmentVariant = setOf(
            OperatorNameConventions.PLUS,
            OperatorNameConventions.MINUS,
            OperatorNameConventions.TIMES,
            OperatorNameConventions.DIV,
            OperatorNameConventions.REM,
            OperatorNameConventions.MOD,
        )

        private val callArgErrors = setOf(
            Errors.ARGUMENT_PASSED_TWICE,
            Errors.MIXING_NAMED_AND_POSITIONED_ARGUMENTS,
            Errors.NAMED_PARAMETER_NOT_FOUND,
            Errors.NAMED_ARGUMENTS_NOT_ALLOWED,
            Errors.VARARG_OUTSIDE_PARENTHESES,
            Errors.SPREAD_OF_NULLABLE,
            Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE,
            Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS,
            Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE,
            Errors.TOO_MANY_ARGUMENTS,
            Errors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION,
            Errors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION,
            *Errors.TYPE_MISMATCH_ERRORS.toTypedArray(),
        )

        private val resolutionFailureErrors: Set<DiagnosticFactoryWithPsiElement<*, *>> = setOf(
            Errors.INVISIBLE_MEMBER,
            Errors.NO_VALUE_FOR_PARAMETER,
            Errors.MISSING_RECEIVER,
            Errors.NO_RECEIVER_ALLOWED,
            Errors.ILLEGAL_SELECTOR,
            Errors.FUNCTION_EXPECTED,
            Errors.FUNCTION_CALL_EXPECTED,
            Errors.NO_CONSTRUCTOR,
            Errors.OVERLOAD_RESOLUTION_AMBIGUITY,
            Errors.NONE_APPLICABLE,
            Errors.CANNOT_COMPLETE_RESOLVE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            Errors.CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY,
            Errors.TYPE_PARAMETER_AS_REIFIED,
            Errors.DEFINITELY_NON_NULLABLE_AS_REIFIED,
            Errors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION,
            Errors.REIFIED_TYPE_UNSAFE_SUBSTITUTION,
            Errors.CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION,
            Errors.RESOLUTION_TO_CLASSIFIER,
            Errors.RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS,
            Errors.PARENTHESIZED_COMPANION_LHS_DEPRECATION,
            Errors.RESOLUTION_TO_PRIVATE_CONSTRUCTOR_OF_SEALED_CLASS,
            Errors.UNRESOLVED_REFERENCE,
            *Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.factories,
            *Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT.factories,
        )

        private val syntaxErrors = setOf(
            Errors.ASSIGNMENT_IN_EXPRESSION_CONTEXT,
        )

        val diagnosticWithResolvedCallsAtPosition1 = setOf(
            Errors.OVERLOAD_RESOLUTION_AMBIGUITY,
            Errors.NONE_APPLICABLE,
            Errors.CANNOT_COMPLETE_RESOLVE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            Errors.ASSIGN_OPERATOR_AMBIGUITY,
            Errors.ITERATOR_AMBIGUITY,
        )

        val diagnosticWithResolvedCallsAtPosition2 = setOf(
            Errors.COMPONENT_FUNCTION_AMBIGUITY,
            Errors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY,
            Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
            Errors.DELEGATE_PD_METHOD_NONE_APPLICABLE,
        )

        private val DiagnosticFactoryForDeprecation<*, *, *>.factories: Array<DiagnosticFactoryWithPsiElement<*, *>>
            get() = arrayOf(warningFactory, errorFactory)
    }

    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun resolveCall(psi: KtElement): KtCallInfo? = with(analysisContext.analyze(psi, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)) {
        if (!canBeResolvedAsCall(psi)) return null

        val parentBinaryExpression = psi.parentOfType<KtBinaryExpression>()
        val lhs = KtPsiUtil.deparenthesize(parentBinaryExpression?.left)
        val unwrappedPsi = KtPsiUtil.deparenthesize(psi as? KtExpression) ?: psi
        if (parentBinaryExpression != null &&
            parentBinaryExpression.operationToken == KtTokens.EQ &&
            (lhs == unwrappedPsi || (lhs as? KtQualifiedExpression)?.selectorExpression == unwrappedPsi) &&
            unwrappedPsi !is KtArrayAccessExpression
        ) {
            // Specially handle property assignment because FE1.0 resolves LHS of assignment to just the property, which would then be
            // treated as a property read.
            return resolveCall(parentBinaryExpression)
        }
        when (unwrappedPsi) {
            is KtBinaryExpression -> {
                handleAsCompoundAssignment(this, unwrappedPsi)?.let { return@with it }
                handleAsFunctionCall(this, unwrappedPsi)
            }
            is KtUnaryExpression -> {
                handleAsIncOrDecOperator(this, unwrappedPsi)?.let { return@with it }
                handleAsCheckNotNullCall(unwrappedPsi)?.let { return@with it }
                handleAsFunctionCall(this, unwrappedPsi)
            }
            else -> handleAsFunctionCall(this, unwrappedPsi)
                ?: handleAsPropertyRead(this, unwrappedPsi)
                ?: handleAsGenericTypeQualifier(unwrappedPsi)
        } ?: handleResolveErrors(this, psi)
    }

    override fun collectCallCandidates(psi: KtElement): List<KtCallCandidateInfo> =
        with(analysisContext.analyze(psi, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)) {
            if (!canBeResolvedAsCall(psi)) return emptyList()

            val resolvedKtCallInfo = resolveCall(psi)
            val bestCandidateDescriptors =
                resolvedKtCallInfo?.calls?.filterIsInstance<KtFunctionCall<*>>()
                    ?.mapNotNullTo(mutableSetOf()) { it.descriptor as? CallableDescriptor }
                    ?: emptySet()

            val unwrappedPsi = KtPsiUtil.deparenthesize(psi as? KtExpression) ?: psi

            if (unwrappedPsi is KtUnaryExpression) {
                // TODO: Handle ++ or -- operator
                handleAsCheckNotNullCall(unwrappedPsi)?.let { return@with it.toKtCallCandidateInfos() }
            }
            if (unwrappedPsi is KtBinaryExpression &&
                (unwrappedPsi.operationToken in OperatorConventions.COMPARISON_OPERATIONS ||
                        unwrappedPsi.operationToken in OperatorConventions.EQUALS_OPERATIONS)
            ) {
                // TODO: Handle compound assignment
                handleAsFunctionCall(this, unwrappedPsi)?.toKtCallCandidateInfos()?.let { return@with it }
            }

            val resolutionScope = unwrappedPsi.getResolutionScope(this) ?: return emptyList()
            val call = unwrappedPsi.getCall(this)?.let {
                if (it is CallTransformer.CallForImplicitInvoke) it.outerCall else it
            } ?: return emptyList()
            val dataFlowInfo = getDataFlowInfoBefore(unwrappedPsi)
            val bindingTrace = DelegatingBindingTrace(this, "Trace for all candidates", withParentDiagnostics = false)
            val dataFlowValueFactory = DataFlowValueFactoryImpl(analysisContext.languageVersionSettings)

            val callResolutionContext = BasicCallResolutionContext.create(
                bindingTrace, resolutionScope, call, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
                ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                /* isAnnotationContext = */ false, analysisContext.languageVersionSettings,
                dataFlowValueFactory
            ).replaceCollectAllCandidates(true)

            val result = analysisContext.callResolver.resolveFunctionCall(callResolutionContext)
            val candidates = result.allCandidates?.let { analysisContext.overloadingConflictResolver.filterOutEquivalentCalls(it) }
                ?: error("allCandidates is null even when collectAllCandidates = true")

            candidates.flatMap { candidate ->
                // The current BindingContext does not have the diagnostics for each individual candidate, only for the resolved call.
                // If there are multiple candidates, we can get each one's diagnostics by reporting it to a new BindingTrace.
                val candidateTrace = DelegatingBindingTrace(this, "Trace for candidate", withParentDiagnostics = false)
                if (candidate is NewAbstractResolvedCall<*>) {
                    analysisContext.kotlinToResolvedCallTransformer.reportDiagnostics(
                        callResolutionContext,
                        candidateTrace,
                        candidate,
                        candidate.diagnostics
                    )
                }

                val candidateKtCallInfo = handleAsFunctionCall(
                    candidateTrace.bindingContext,
                    unwrappedPsi,
                    candidate,
                    candidateTrace.bindingContext.diagnostics
                )
                candidateKtCallInfo.toKtCallCandidateInfos(bestCandidateDescriptors)
            }
        }

    private val KtFunctionCall<*>.descriptor: DeclarationDescriptor?
        get() = when (val symbol = symbol) {
            is KtFe10PsiSymbol<*, *> -> symbol.descriptor
            is KtFe10DescSymbol<*> -> symbol.descriptor
            else -> null
        }

    private fun KtCallInfo?.toKtCallCandidateInfos(): List<KtCallCandidateInfo> {
        return when (this) {
            is KtSuccessCallInfo -> listOf(KtApplicableCallCandidateInfo(call, isInBestCandidates = true))
            is KtErrorCallInfo -> candidateCalls.map { KtInapplicableCallCandidateInfo(it, isInBestCandidates = true, diagnostic) }
            null -> emptyList()
        }
    }

    private fun KtCallInfo?.toKtCallCandidateInfos(bestCandidateDescriptors: Set<CallableDescriptor>): List<KtCallCandidateInfo> {
        // TODO: We should prefer to compare symbols instead of descriptors, but we can't do so while symbols are not cached.
        fun KtCall.isInBestCandidates(): Boolean {
            val descriptor = this.safeAs<KtFunctionCall<*>>()?.descriptor as? CallableDescriptor
            return descriptor != null && bestCandidateDescriptors.any { it ->
                DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(
                    it,
                    descriptor,
                    allowCopiesFromTheSameDeclaration = true,
                    kotlinTypeRefiner = analysisContext.kotlinTypeRefiner
                )
            }
        }

        return when (this) {
            is KtSuccessCallInfo -> {
                listOf(KtApplicableCallCandidateInfo(call, call.isInBestCandidates()))
            }
            is KtErrorCallInfo -> candidateCalls.map {
                KtInapplicableCallCandidateInfo(it, it.isInBestCandidates(), diagnostic)
            }
            null -> emptyList()
        }
    }

    private fun handleAsCompoundAssignment(context: BindingContext, binaryExpression: KtBinaryExpression): KtCallInfo? {
        val left = binaryExpression.left ?: return null
        val right = binaryExpression.right
        val resolvedCalls = mutableListOf<ResolvedCall<*>>()
        return when (binaryExpression.operationToken) {
            KtTokens.EQ -> {
                val resolvedCall = left.getResolvedCall(context) ?: return null
                resolvedCalls += resolvedCall
                val partiallyAppliedSymbol =
                    resolvedCall.toPartiallyAppliedVariableSymbol(context) ?: return null
                KtSimpleVariableAccessCall(
                    partiallyAppliedSymbol,
                    resolvedCall.toTypeArgumentsMapping(partiallyAppliedSymbol),
                    KtSimpleVariableAccess.Write(right)
                )
            }
            in KtTokens.AUGMENTED_ASSIGNMENTS -> {
                if (right == null) return null
                val operatorCall = binaryExpression.getResolvedCall(context) ?: return null
                resolvedCalls += operatorCall
                // This method only handles compound assignment. Other cases like `plusAssign`, `rangeTo`, `contains` are handled by plain
                // `handleAsFunctionCall`
                if (operatorCall.resultingDescriptor.name !in operatorWithAssignmentVariant) return null
                val operatorPartiallyAppliedSymbol =
                    operatorCall.toPartiallyAppliedFunctionSymbol<KtFunctionSymbol>(context) ?: return null

                val compoundAccess = KtCompoundAccess.CompoundAssign(
                    operatorPartiallyAppliedSymbol,
                    binaryExpression.getCompoundAssignKind(),
                    right
                )

                if (left is KtArrayAccessExpression) {
                    createCompoundArrayAccessCall(context, left, compoundAccess, resolvedCalls)
                } else {
                    val resolvedCall = left.getResolvedCall(context) ?: return null
                    resolvedCalls += resolvedCall
                    val variableAppliedSymbol = resolvedCall.toPartiallyAppliedVariableSymbol(context) ?: return null
                    KtCompoundVariableAccessCall(
                        variableAppliedSymbol,
                        resolvedCall.toTypeArgumentsMapping(variableAppliedSymbol),
                        compoundAccess
                    )
                }
            }
            else -> null
        }?.let { createCallInfo(context, binaryExpression, it, resolvedCalls) }
    }

    private fun handleAsIncOrDecOperator(context: BindingContext, unaryExpression: KtUnaryExpression): KtCallInfo? {
        if (unaryExpression.operationToken !in KtTokens.INCREMENT_AND_DECREMENT) return null
        val operatorCall = unaryExpression.getResolvedCall(context) ?: return null
        val resolvedCalls = mutableListOf(operatorCall)
        val operatorPartiallyAppliedSymbol = operatorCall.toPartiallyAppliedFunctionSymbol<KtFunctionSymbol>(context) ?: return null
        val baseExpression = unaryExpression.baseExpression
        val kind = unaryExpression.getInOrDecOperationKind()
        val precedence = when (unaryExpression) {
            is KtPostfixExpression -> KtCompoundAccess.IncOrDecOperation.Precedence.POSTFIX
            is KtPrefixExpression -> KtCompoundAccess.IncOrDecOperation.Precedence.PREFIX
            else -> error("unexpected KtUnaryExpression $unaryExpression")
        }
        val compoundAccess = KtCompoundAccess.IncOrDecOperation(operatorPartiallyAppliedSymbol, kind, precedence)
        return if (baseExpression is KtArrayAccessExpression) {
            createCompoundArrayAccessCall(context, baseExpression, compoundAccess, resolvedCalls)
        } else {
            val resolvedCall = baseExpression.getResolvedCall(context)
            val variableAppliedSymbol = resolvedCall?.toPartiallyAppliedVariableSymbol(context) ?: return null
            KtCompoundVariableAccessCall(variableAppliedSymbol, resolvedCall.toTypeArgumentsMapping(variableAppliedSymbol), compoundAccess)
        }?.let { createCallInfo(context, unaryExpression, it, resolvedCalls) }
    }

    private fun createCompoundArrayAccessCall(
        context: BindingContext,
        arrayAccessExpression: KtArrayAccessExpression,
        compoundAccess: KtCompoundAccess,
        resolvedCalls: MutableList<ResolvedCall<*>>
    ): KtCompoundArrayAccessCall? {
        val resolvedGetCall = context[BindingContext.INDEXED_LVALUE_GET, arrayAccessExpression] ?: return null
        resolvedCalls += resolvedGetCall
        val getPartiallyAppliedSymbol = resolvedGetCall.toPartiallyAppliedFunctionSymbol<KtFunctionSymbol>(context) ?: return null
        val resolvedSetCall = context[BindingContext.INDEXED_LVALUE_SET, arrayAccessExpression] ?: return null
        resolvedCalls += resolvedSetCall
        val setPartiallyAppliedSymbol = resolvedSetCall.toPartiallyAppliedFunctionSymbol<KtFunctionSymbol>(context) ?: return null
        return KtCompoundArrayAccessCall(
            compoundAccess,
            arrayAccessExpression.indexExpressions,
            getPartiallyAppliedSymbol,
            setPartiallyAppliedSymbol
        )
    }

    private fun handleAsCheckNotNullCall(unaryExpression: KtUnaryExpression): KtCallInfo? {
        if (unaryExpression.operationToken == KtTokens.EXCLEXCL) {
            val baseExpression = unaryExpression.baseExpression ?: return null
            return KtSuccessCallInfo(KtCheckNotNullCall(token, baseExpression))
        }
        return null
    }

    private fun handleAsFunctionCall(context: BindingContext, element: KtElement): KtCallInfo? {
        return element.getResolvedCall(context)?.let { handleAsFunctionCall(context, element, it) }
    }

    private fun handleAsFunctionCall(
        context: BindingContext,
        element: KtElement,
        resolvedCall: ResolvedCall<*>,
        diagnostics: Diagnostics = context.diagnostics
    ): KtCallInfo? {
        return if (resolvedCall is VariableAsFunctionResolvedCall) {
            if (element is KtCallExpression || element is KtQualifiedExpression) {
                // TODO: consider demoting extension receiver to the first argument to align with FIR behavior. See test case
                //  analysis/analysis-api/testData/components/callResolver/resolveCall/functionTypeVariableCall_dispatchReceiver.kt:5 where
                //  FIR and FE1.0 behaves differently because FIR unifies extension receiver of functional type as the first argument
                resolvedCall.functionCall.toFunctionKtCall(context)
            } else {
                resolvedCall.variableCall.toPropertyRead(context)
            }?.let { createCallInfo(context, element, it, listOf(resolvedCall), diagnostics) }
        } else {
            resolvedCall.toFunctionKtCall(context)?.let { createCallInfo(context, element, it, listOf(resolvedCall), diagnostics) }
        }
    }

    private fun handleAsPropertyRead(context: BindingContext, element: KtElement): KtCallInfo? {
        val call = element.getResolvedCall(context) ?: return null
        return call.toPropertyRead(context)?.let { createCallInfo(context, element, it, listOf(call)) }
    }

    /**
     * Handles call expressions like `Foo<Bar>` or `test.Foo<Bar>` in calls like `Foo<Bar>::foo` and `test.Foo<Bar>::foo`.
     *
     * ATM does not perform any resolve checks, since it does not seem possible with [BindingContext], so it might give some
     * false positives.
     */
    private fun handleAsGenericTypeQualifier(element: KtElement): KtCallInfo? {
        if (element !is KtExpression) return null

        val wholeQualifier = element.getQualifiedExpressionForSelector() as? KtDotQualifiedExpression ?: element

        val call = wholeQualifier.getPossiblyQualifiedCallExpression() ?: return null
        if (call.typeArgumentList == null || call.valueArgumentList != null) return null

        return KtSuccessCallInfo(KtGenericTypeQualifier(token, wholeQualifier))
    }

    private fun ResolvedCall<*>.toPropertyRead(context: BindingContext): KtVariableAccessCall? {
        val partiallyAppliedSymbol = toPartiallyAppliedVariableSymbol(context) ?: return null
        return KtSimpleVariableAccessCall(
            partiallyAppliedSymbol,
            toTypeArgumentsMapping(partiallyAppliedSymbol),
            KtSimpleVariableAccess.Read
        )
    }

    private fun ResolvedCall<*>.toFunctionKtCall(context: BindingContext): KtFunctionCall<*>? {
        val partiallyAppliedSymbol = toPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol>(context) ?: return null
        val argumentMapping = createArgumentMapping(partiallyAppliedSymbol.signature)
        if (partiallyAppliedSymbol.signature.symbol is KtConstructorSymbol) {
            @Suppress("UNCHECKED_CAST")
            val partiallyAppliedConstructorSymbol = partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>
            when (val callElement = call.callElement) {
                is KtAnnotationEntry -> return KtAnnotationCall(partiallyAppliedSymbol, argumentMapping)
                is KtConstructorDelegationCall -> return KtDelegatedConstructorCall(
                    partiallyAppliedConstructorSymbol,
                    if (callElement.isCallToThis) KtDelegatedConstructorCall.Kind.THIS_CALL else KtDelegatedConstructorCall.Kind.SUPER_CALL,
                    argumentMapping
                )
                is KtSuperTypeCallEntry -> return KtDelegatedConstructorCall(
                    partiallyAppliedConstructorSymbol,
                    KtDelegatedConstructorCall.Kind.SUPER_CALL,
                    argumentMapping
                )
            }
        }

        return KtSimpleFunctionCall(
            partiallyAppliedSymbol,
            argumentMapping,
            toTypeArgumentsMapping(partiallyAppliedSymbol),
            call.callType == Call.CallType.INVOKE
        )
    }

    private fun ResolvedCall<*>.toPartiallyAppliedVariableSymbol(context: BindingContext): KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>? {
        val partiallyAppliedSymbol = toPartiallyAppliedSymbol(context) ?: return null
        if (partiallyAppliedSymbol.signature !is KtVariableLikeSignature<*>) return null
        @Suppress("UNCHECKED_CAST")
        return partiallyAppliedSymbol as KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>
    }


    private inline fun <reified S : KtFunctionLikeSymbol> ResolvedCall<*>.toPartiallyAppliedFunctionSymbol(context: BindingContext): KtPartiallyAppliedFunctionSymbol<S>? {
        val partiallyAppliedSymbol = toPartiallyAppliedSymbol(context) ?: return null
        if (partiallyAppliedSymbol.symbol !is S) return null
        @Suppress("UNCHECKED_CAST")
        return partiallyAppliedSymbol as KtPartiallyAppliedFunctionSymbol<S>
    }

    private fun ResolvedCall<*>.toPartiallyAppliedSymbol(context: BindingContext): KtPartiallyAppliedSymbol<*, *>? {
        val targetDescriptor = candidateDescriptor
        val symbol = targetDescriptor.toKtCallableSymbol(analysisContext) ?: return null
        val signature = createSignature(symbol, resultingDescriptor) ?: return null
        if (targetDescriptor.isSynthesizedPropertyFromJavaAccessors()) {
            // FE1.0 represents synthesized properties as an extension property of the Java class. Hence we use the extension receiver as
            // the dispatch receiver and always pass null for extension receiver (because in Java there is no way to specify an extension
            // receiver)
            return KtPartiallyAppliedSymbol(
                signature,
                extensionReceiver?.toKtReceiverValue(context, this),
                null
            )
        } else {
            return KtPartiallyAppliedSymbol(
                signature,
                dispatchReceiver?.toKtReceiverValue(context, this, smartCastDispatchReceiverType),
                extensionReceiver?.toKtReceiverValue(context, this),
            )
        }
    }

    private fun ReceiverValue.toKtReceiverValue(
        context: BindingContext,
        resolvedCall: ResolvedCall<*>,
        smartCastType: KotlinType? = null
    ): KtReceiverValue? {
        val ktType = type.toKtType(analysisContext)
        val result = when (this) {
            is ExpressionReceiver -> expression.toExplicitReceiverValue(ktType)
            is ExtensionReceiver -> {
                val extensionReceiverParameter = this.declarationDescriptor.extensionReceiverParameter ?: return null
                KtImplicitReceiverValue(KtFe10ReceiverParameterSymbol(extensionReceiverParameter, analysisContext), ktType)
            }
            is ImplicitReceiver -> {
                val symbol = this.declarationDescriptor.toKtSymbol(analysisContext) ?: return null
                KtImplicitReceiverValue(symbol, ktType)
            }
            else -> null
        }
        var smartCastTypeToUse = smartCastType
        if (smartCastTypeToUse == null) {
            when (result) {
                is KtExplicitReceiverValue -> {
                    smartCastTypeToUse = context[BindingContext.SMARTCAST, result.expression]?.type(resolvedCall.call)
                }
                is KtImplicitReceiverValue -> {
                    smartCastTypeToUse =
                        context[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, resolvedCall.call.calleeExpression]?.receiverTypes?.get(this)
                }
                else -> {}
            }
        }
        return if (smartCastTypeToUse != null && result != null) {
            KtSmartCastedReceiverValue(result, smartCastTypeToUse.toKtType(analysisContext))
        } else {
            result
        }
    }

    private fun createSignature(symbol: KtSymbol, resultingDescriptor: CallableDescriptor): KtCallableSignature<*>? {
        val returnType = if (resultingDescriptor is ValueParameterDescriptor && resultingDescriptor.isVararg) {
            val arrayType = resultingDescriptor.returnType ?: return null
            analysisContext.builtIns.getArrayElementType(arrayType)
        } else {
            resultingDescriptor.returnType
        }
        val ktReturnType = returnType?.toKtType(analysisContext) ?: return null
        val receiverType = if (resultingDescriptor.isSynthesizedPropertyFromJavaAccessors()) {
            // FE1.0 represents synthesized properties as an extension property of the Java class. Hence the extension receiver type should
            // always be null
            null
        } else {
            resultingDescriptor.extensionReceiverParameter?.returnType?.toKtType(analysisContext)
        }
        return when (symbol) {
            is KtVariableLikeSymbol -> KtFe10VariableLikeSignature(symbol, ktReturnType, receiverType)
            is KtFunctionLikeSymbol -> KtFe10FunctionLikeSignature(
                symbol,
                ktReturnType,
                receiverType,
                @Suppress("UNCHECKED_CAST")
                symbol.valueParameters.zip(resultingDescriptor.valueParameters).map { (symbol, resultingDescriptor) ->
                    createSignature(symbol, resultingDescriptor) as KtVariableLikeSignature<KtValueParameterSymbol>
                })
            else -> error("unexpected callable symbol $this")
        }
    }

    private fun CallableDescriptor?.isSynthesizedPropertyFromJavaAccessors() =
        this is PropertyDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    private fun ResolvedCall<*>.createArgumentMapping(signature: KtFunctionLikeSignature<*>): LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> {
        val parameterSignatureByName = signature.valueParameters.associateBy {
            // ResolvedCall.valueArguments have their names affected by the `@ParameterName` annotations,
            // so we use `name` instead of `symbol.name`
            it.name
        }
        val result = LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>()
        for ((parameter, arguments) in valueArguments) {
            val parameterSymbol = KtFe10DescValueParameterSymbol(parameter, analysisContext)

            for (argument in arguments.arguments) {
                val expression = argument.getArgumentExpression() ?: continue
                result[expression] = parameterSignatureByName[parameterSymbol.name] ?: continue
            }
        }
        return result
    }

    private fun createCallInfo(
        context: BindingContext,
        psi: KtElement,
        ktCall: KtCall,
        resolvedCalls: List<ResolvedCall<*>>,
        diagnostics: Diagnostics = context.diagnostics
    ): KtCallInfo {
        val failedResolveCall = resolvedCalls.firstOrNull { !it.status.isSuccess } ?: return KtSuccessCallInfo(ktCall)

        val diagnostic = getDiagnosticToReport(context, psi, ktCall, diagnostics)?.let { KtFe10Diagnostic(it, token) }
            ?: KtNonBoundToPsiErrorDiagnostic(
                factoryName = null,
                "${failedResolveCall.status} with ${failedResolveCall.resultingDescriptor.name}",
                token
            )
        return KtErrorCallInfo(listOf(ktCall), diagnostic, token)
    }

    private fun handleResolveErrors(context: BindingContext, psi: KtElement): KtErrorCallInfo? {
        val diagnostic = getDiagnosticToReport(context, psi, null) ?: return null
        val ktDiagnostic = diagnostic.let { KtFe10Diagnostic(it, token) }
        val calls = when (diagnostic.factory) {
            in diagnosticWithResolvedCallsAtPosition1 -> {
                require(diagnostic is DiagnosticWithParameters1<*, *>)
                @Suppress("UNCHECKED_CAST")
                diagnostic.a as Collection<ResolvedCall<*>>
            }
            in diagnosticWithResolvedCallsAtPosition2 -> {
                require(diagnostic is DiagnosticWithParameters2<*, *, *>)
                @Suppress("UNCHECKED_CAST")
                diagnostic.b as Collection<ResolvedCall<*>>
            }
            else -> {
                emptyList()
            }
        }
        return KtErrorCallInfo(calls.mapNotNull { it.toFunctionKtCall(context) ?: it.toPropertyRead(context) }, ktDiagnostic, token)
    }

    private fun getDiagnosticToReport(
        context: BindingContext,
        psi: KtElement,
        ktCall: KtCall?,
        diagnostics: Diagnostics = context.diagnostics
    ) = diagnostics.firstOrNull { diagnostic ->
        if (diagnostic.severity != Severity.ERROR) return@firstOrNull false
        if (diagnostic.factory in syntaxErrors) return@firstOrNull true
        val isResolutionError = diagnostic.factory in resolutionFailureErrors
        val isCallArgError = diagnostic.factory in callArgErrors
        val reportedPsi = diagnostic.psiElement
        val reportedPsiParent = reportedPsi.parent
        when {
            // Errors reported on the querying element or the `selectorExpression`/`calleeExpression` of the querying element
            isResolutionError &&
                    (reportedPsi == psi ||
                            psi is KtQualifiedExpression && reportedPsi == psi.selectorExpression ||
                            psi is KtCallElement && reportedPsi.parentsWithSelf.any { it == psi.calleeExpression }) -> true
            // Errors reported on the value argument list or the right most parentheses (not enough argument, for example)
            isResolutionError &&
                    reportedPsi is KtValueArgumentList || reportedPsiParent is KtValueArgumentList && reportedPsi == reportedPsiParent.rightParenthesis -> true
            // errors on call args for normal function calls
            isCallArgError &&
                    reportedPsiParent is KtValueArgument &&
                    (psi is KtQualifiedExpression && psi.selectorExpression?.safeAs<KtCallExpression>()?.valueArguments?.contains(
                        reportedPsiParent
                    ) == true ||
                            psi is KtCallElement && reportedPsiParent in psi.valueArguments) -> true
            // errors on receiver of invoke function calls
            isCallArgError &&
                    (psi is KtQualifiedExpression && reportedPsiParent == psi.selectorExpression ||
                            psi is KtCallElement && reportedPsiParent == psi) -> true
            // errors on index args for array access convention
            isCallArgError &&
                    reportedPsiParent is KtContainerNode && reportedPsiParent.parent is KtArrayAccessExpression -> true
            // errors on lambda args
            isCallArgError &&
                    reportedPsi is KtLambdaExpression || reportedPsi is KtLambdaArgument -> true
            // errors on value to set using array access convention
            isCallArgError &&
                    ktCall is KtSimpleFunctionCall && (reportedPsiParent as? KtBinaryExpression)?.right == reportedPsi -> true
            else -> false
        }
    }

    private fun ResolvedCall<*>.toTypeArgumentsMapping(
        partiallyAppliedSymbol: KtPartiallyAppliedSymbol<*, *>
    ): Map<KtTypeParameterSymbol, KtType> {
        if (typeArguments.isEmpty()) return emptyMap()

        val typeParameters = partiallyAppliedSymbol.symbol.typeParameters

        val result = mutableMapOf<KtTypeParameterSymbol, KtType>()
        for ((parameter, type) in typeArguments) {
            val ktParameter = typeParameters.getOrNull(parameter.index) ?: return emptyMap()

            // i.e. we were not able to infer some types
            if (type.contains { it: UnwrappedType -> it.constructor is TypeVariableTypeConstructor }) return emptyMap()

            result[ktParameter] = type.toKtType(analysisContext)
        }

        return result
    }
}