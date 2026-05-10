/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

@KaImplementationDetail
@OptIn(KtExperimentalApi::class)
abstract class KaBaseResolver<T : KaSession> : KaBaseSessionComponent<T>(), KaResolver {
    protected abstract fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt?

    protected abstract fun performSymbolResolution(reference: KtReference): KaSymbolResolutionAttempt?

    final override fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? = withValidityAssertion {
        when (this) {
            is KtResolvableCall -> tryResolveSymbolsForResolvableCall()
            is KtOperationReferenceExpression -> tryResolveSymbolsForOperationReference()
            is KtElement -> tryResolveSymbolsForElement()
            is KtReference -> tryResolveSymbolsForReference()
            else -> null
        }
    }


    /**
     * Technically, symbol resolution can be more efficient than calls,
     * because calls require collecting more information (e.g., argument mappings).
     * However, the tradeoff is almost complete code duplication and duplicate caches that seem too high.
     * In reality, the reuse of call resolution is actually a benefit because its result is cached and
     * effectively reused at all entry points into the resolver API
     */
    private fun KtResolvableCall.tryResolveSymbolsForResolvableCall(): KaSymbolResolutionAttempt? = when (this) {
        // Both reference kinds may stand in either type or call positions, and in some cases the symbol-based
        // result is more specific (e.g., it prefers classes to constructors).
        // For enum entry super-type references this also means the enclosing enum class is returned instead of
        // the synthetic constructor of the call form.
        is KtNameReferenceExpression, is KtEnumEntrySuperclassReferenceExpression -> tryResolveSymbolsForElement()
        else -> null
    } ?: when (val callAttempt = tryResolveCall()) {
        is KaSingleCallResolutionAttempt -> callAttempt.toSingleSymbolResolutionAttempt()
        is KaMultiCallResolutionAttempt -> callAttempt.toSymbolResolutionAttempt()
        null -> null
    }

    /**
     * Logic for operations might be non-trivial, so it is more efficient to rely on the call resolution
     *
     * @see tryResolveSymbolsForResolvableCall
     */
    private fun KtOperationReferenceExpression.tryResolveSymbolsForOperationReference(): KaSymbolResolutionAttempt? {
        val resolvableCall = parent as? KtResolvableCall ?: return null
        return when (val callAttempt = resolvableCall.tryResolveCall()) {
            is KaCallResolutionError -> callAttempt.toSingleSymbolResolutionAttempt()

            // Single variable access is not expected to be a result of the symbol resolve (the assignment use case)
            is KaCallResolutionSuccess if callAttempt.call !is KaVariableAccessCall -> callAttempt.toSingleSymbolResolutionAttempt()
            is KaMultiCallResolutionAttempt -> when (callAttempt) {
                is KaCompoundArrayAccessCallResolutionAttempt -> mergeSymbolAttempts(
                    listOf(
                        callAttempt.operationCallAttempt.toSingleSymbolResolutionAttempt(),
                        callAttempt.setterCallAttempt.toSingleSymbolResolutionAttempt(),
                    )
                )

                is KaCompoundVariableAccessCallResolutionAttempt -> callAttempt.operationCallAttempt.toSingleSymbolResolutionAttempt()
                else -> callAttempt.toSymbolResolutionAttempt()
            }

            else -> null
        }
    }

    private fun <T> T.tryResolveSymbolsForElement(): KaSymbolResolutionAttempt? where T : KtResolvable, T : KtElement {
        checkValidity()
        return performSymbolResolution(this)
    }

    private fun <T> T.tryResolveSymbolsForReference(): KaSymbolResolutionAttempt? where T : KtResolvable, T : KtReference {
        element.checkValidity()
        return performSymbolResolution(this)
    }

    final override fun KtResolvable.resolveSymbols(): Collection<KaSymbol> = withValidityAssertion {
        tryResolveSymbols()?.successfulSymbols ?: emptyList()
    }

    final override fun KtResolvable.resolveSymbol(): KaSymbol? = withValidityAssertion {
        resolveSymbols().singleOrNull()
    }

    private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(): R? = resolveSymbol() as? R

    final override fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtCallElement.resolveSymbol(): KaFunctionSymbol? = resolveSymbolSafe()
    final override fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()
    final override fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? = resolveSymbolSafe()
    final override fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol? = resolveSymbolSafe()
    final override fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaNamedClassSymbol? = resolveSymbolSafe()
    final override fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? = resolveSymbolSafe()
    final override fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? = resolveSymbolSafe()
    final override fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? = resolveSymbolSafe()
    final override fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()
    final override fun KtQualifiedExpression.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()
    final override fun KtConstructorCalleeExpression.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtInstanceExpressionWithLabel.resolveSymbol(): KaDeclarationSymbol? = resolveSymbolSafe()
    final override fun KtNullableType.resolveSymbol(): KaClassifierSymbol? = resolveSymbolSafe()
    final override fun KtFunctionType.resolveSymbol(): KaClassSymbol? = resolveSymbolSafe()
    final override fun KtTypeReference.resolveSymbol(): KaClassifierSymbol? = resolveSymbolSafe()
    final override fun KtClassLiteralExpression.resolveSymbol(): KaClassifierSymbol? = resolveSymbolSafe()
    final override fun KtSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = resolveSymbolSafe()
    final override fun KtDelegatedSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? = resolveSymbolSafe()

    final override fun KtReference.resolveToSymbol(): KaSymbol? = withPsiValidityAssertion(element) {
        return resolveToSymbols().singleOrNull()
    }

    private fun KtElement.tryResolveCallImpl(): KaCallResolutionAttempt? {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallResolution)
    }

    protected abstract fun performCallResolution(psi: KtElement): KaCallResolutionAttempt?

    final override fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? = withValidityAssertion {
        if (this is KtElement) {
            checkValidity()
            tryResolveCallImpl()
        } else {
            null
        }
    }

    final override fun KtResolvableCall.resolveCall(): KaSingleOrMultiCall? = tryResolveCall()?.successfulCall

    private inline fun <reified R : KaSingleOrMultiCall> KtResolvableCall.resolveCallSafe(): R? = resolveCall() as? R

    private inline fun <reified S : KaCallableSymbol, C : KaCallableSignature<S>, reified R : KaSingleCall<S, C>> KtResolvableCall.resolveSingleCallSafe(): R? {
        val call = resolveCall() ?: return null
        checkWithAttachment(
            call is KaSingleCall<*, *>,
            { "Expected call of type ${KaSingleCall::class.simpleName}, got ${call::class.simpleName}" },
        ) {
            withResolvableEntry(this@resolveSingleCallSafe)
        }

        val callableSymbol = call.symbol
        checkWithAttachment(
            callableSymbol is S,
            { "Expected symbol of type ${S::class.simpleName}, got ${callableSymbol::class.simpleName}" },
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withResolvableEntry(this@resolveSingleCallSafe)
        }

        checkWithAttachment(
            call is R,
            { "Expected call of type ${R::class.simpleName}, got ${call::class.simpleName}" }
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withResolvableEntry(this@resolveSingleCallSafe)
        }

        return call
    }

    final override fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = resolveSingleCallSafe()
    final override fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveSingleCallSafe()
    final override fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = resolveSingleCallSafe()
    final override fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall? = resolveSingleCallSafe()
    final override fun KtCallElement.resolveCall(): KaFunctionCall<*>? = resolveCallSafe()
    final override fun KtCallableReferenceExpression.resolveCall(): KaCallableReferenceCall<*, *>? = resolveCallSafe()
    final override fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? = resolveSingleCallSafe()
    final override fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtDestructuringDeclarationEntry.resolveCall(): KaSingleCall<*, *>? = resolveCallSafe()
    final override fun KtQualifiedExpression.resolveCall(): KaSingleCall<*, *>? = resolveCallSafe()
    final override fun KtForExpression.resolveCall(): KaForLoopCall? = resolveCallSafe()
    final override fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall? = resolveCallSafe()

    final override fun KtForExpression.tryResolveCall(): KaForLoopCallResolutionAttempt? =
        tryResolveCallImpl() as? KaForLoopCallResolutionAttempt

    final override fun KtPropertyDelegate.tryResolveCall(): KaDelegatedPropertyCallResolutionAttempt? =
        tryResolveCallImpl() as? KaDelegatedPropertyCallResolutionAttempt

    final override fun KtConstructorCalleeExpression.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveSingleCallSafe()
    final override fun KtNameReferenceExpression.resolveCall(): KaSingleCall<*, *>? = resolveCallSafe()

    final override fun KtElement.resolveToCall(): KaCallInfo? = withPsiValidityAssertion {
        when (val attempt = tryResolveCallImpl()) {
            is KaCallResolutionError -> KaBaseErrorCallInfo(attempt.candidateCalls.map { it.asKaCall() }, attempt.diagnostic)
            is KaCallResolutionSuccess -> KaBaseSuccessCallInfo(attempt.kaCall)
            is KaMultiCallResolutionAttempt -> attempt.toCallInfo()
            null -> null
        }
    }

    /**
     * Returns the legacy [KaCall] view of [this] [KaSingleOrMultiCall]. Most resolution result types
     * implement [KaCall] directly. The exception is [KaCallableReferenceCall], which is part of the
     * new resolution API and intentionally does not extend the deprecated [KaCall] hierarchy. For
     * that case we emulate a legacy [KaCall] by re-encoding the reference as the corresponding
     * [KaSimpleFunctionCall] / [KaSimpleVariableAccessCall] view.
     */
    protected fun KaSingleOrMultiCall.asKaCall(): KaCall = when (this) {
        is KaBaseCallableReferenceCall<*, *> -> asLegacyKaCall()
        else -> this as KaCall
    }

    @Suppress("UNCHECKED_CAST")
    private fun KaBaseCallableReferenceCall<*, *>.asLegacyKaCall(): KaCall {
        val partiallyAppliedSymbol = backingPartiallyAppliedSymbol
        return when (partiallyAppliedSymbol.symbol) {
            is KaFunctionSymbol -> KaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
                backingArgumentMapping = emptyMap(),
                backingTypeArgumentsMapping = typeArgumentsMapping,
            )

            is KaVariableSymbol -> KaBaseSimpleVariableAccessCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
                backingTypeArgumentsMapping = typeArgumentsMapping,
                backingKind = KaBaseVariableReadAccess,
                backingIsContextSensitive = false,
            )
        }
    }

    protected inline val KaCallResolutionSuccess.kaCall: KaCall
        get() = call.asKaCall()

    private fun KtElement.collectCallCandidatesImpl(): List<KaCallCandidate> {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallCandidatesCollection).orEmpty()
    }

    protected abstract fun performCallCandidatesCollection(psi: KtElement): List<KaCallCandidate>

    final override fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate> = withValidityAssertion {
        if (this is KtElement) {
            checkValidity()
            collectCallCandidatesImpl()
        } else {
            emptyList()
        }
    }

    final override fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> = withPsiValidityAssertion {
        collectCallCandidatesImpl().map(KaCallCandidate::asKaCallCandidateInfo)
    }

    protected fun tryResolveSymbolsForReferenceViaElement(reference: KtReference): KaSymbolResolutionAttempt? {
        return (reference.element as? KtResolvable)?.tryResolveSymbols()
    }

    protected fun tryResolveSymbolsForInvokeReference(reference: KtInvokeFunctionReference): KaSymbolResolutionAttempt? =
        when (val callResult = reference.element.tryResolveCall()) {
            // There is no way to distinguish between the error regular and implicit calls, so by default only relevant errors are shown
            is KaCallResolutionError -> callResult.candidateCalls.filterIsInstance<KaImplicitInvokeCall>().map { it.symbol }
                .ifNotEmpty {
                    KaBaseSymbolResolutionError(
                        backingDiagnostic = callResult.diagnostic,
                        backingCandidateSymbols = this,
                    )
                }

            is KaCallResolutionSuccess -> when (val call = callResult.call) {
                is KaImplicitInvokeCall -> KaBaseSymbolResolutionSuccess(backingSymbol = call.symbol)
                else -> null
            }

            // Multi-call resolution attempts are never implicit invoke calls
            is KaMultiCallResolutionAttempt -> null
            null -> null
        }

    private fun tryResolveSymbolsViaResolveToSymbols(
        reference: KtReference,
    ): KaSymbolResolutionAttempt? = reference.resolveToSymbols().ifNotEmpty {
        KaBaseSymbolResolutionSuccess(backingSymbols = this.toList())
    }

    /**
     * KDocs cannot have diagnostics, so effectively they always successfully resolved.
     * This means that a special handling is not needed (at least yet) and the references'
     * result could be reused fully with no contradictions
     */
    protected fun tryResolveSymbolsForKDocReference(
        reference: KDocReference,
    ): KaSymbolResolutionAttempt? = tryResolveSymbolsViaResolveToSymbols(reference)

    protected fun tryResolveSymbolsForDefaultAnnotationArgumentReference(
        reference: KtDefaultAnnotationArgumentReference,
    ): KaSymbolResolutionAttempt? = tryResolveSymbolsViaResolveToSymbols(reference)

    protected fun KtBinaryExpression.getCompoundAssignKind(): KaCompoundAssignOperation.Kind = when (operationToken) {
        KtTokens.PLUSEQ -> KaCompoundAssignOperation.Kind.PLUS_ASSIGN
        KtTokens.MINUSEQ -> KaCompoundAssignOperation.Kind.MINUS_ASSIGN
        KtTokens.MULTEQ -> KaCompoundAssignOperation.Kind.TIMES_ASSIGN
        KtTokens.PERCEQ -> KaCompoundAssignOperation.Kind.REM_ASSIGN
        KtTokens.DIVEQ -> KaCompoundAssignOperation.Kind.DIV_ASSIGN
        else -> error("unexpected operator $operationToken")
    }

    protected fun KtUnaryExpression.getInOrDecOperationKind(): KaCompoundUnaryOperation.Kind = when (operationToken) {
        KtTokens.PLUSPLUS -> KaCompoundUnaryOperation.Kind.INC
        KtTokens.MINUSMINUS -> KaCompoundUnaryOperation.Kind.DEC
        else -> error("unexpected operator $operationToken")
    }

    protected fun KtExpression.toExplicitReceiverValue(type: KaType): KaExplicitReceiverValue =
        KaBaseExplicitReceiverValue(expression = this, backingType = type, isSafeNavigation = isReceiverOfKtSafeQualifiedExpression())

    private fun KtExpression.isReceiverOfKtSafeQualifiedExpression(): Boolean {
        val safeQualifiedExpression = parentOfType<KtSafeQualifiedExpression>() ?: return false
        return KtPsiUtil.deparenthesize(safeQualifiedExpression.receiverExpression) == KtPsiUtil.deparenthesize(this)
    }

    protected fun canBeResolvedAsCall(ktElement: KtElement): Boolean = when (ktElement) {
        is KtBinaryExpression -> ktElement.operationToken !in nonCallBinaryOperator
        is KtPrefixExpression -> true
        is KtPostfixExpression -> ktElement.operationToken != KtTokens.EXCLEXCL
        is KtCallElement -> true
        is KtConstructorCalleeExpression -> true
        is KtQualifiedExpression -> true
        is KtNameReferenceExpression -> ktElement.parent !is KtInstanceExpressionWithLabel
        is KtArrayAccessExpression -> true
        is KtCallableReferenceExpression -> true
        is KtWhenConditionInRange -> true
        is KtCollectionLiteralExpression -> true
        is KtConstructorDelegationReferenceExpression -> true
        is KtEnumEntrySuperclassReferenceExpression -> true
        is KtDestructuringDeclarationEntry -> true
        is KtForExpression -> true
        is KtPropertyDelegate -> true
        else -> false
    }

    private fun KtElement.unwrapResolvableCall(): KtElement? = when (this) {
        // Most likely we will drop call resolution for operators, and only resolveSymbol will be available for them.
        // Call resolution API is available on a parent expression (like binary or unary operator)
        is KtOperationReferenceExpression -> parent as? KtElement
        else -> this
    }?.takeIf(::canBeResolvedAsCall)

    private fun KaMultiCallResolutionAttempt.toCallInfo(): KaCallInfo = fold(
        onSuccess = { KaBaseSuccessCallInfo(it.asKaCall()) },
        onFailure = { attempts ->
            val errorAttempts = attempts.filterIsInstance<KaCallResolutionError>()
            val firstDiagnostic = errorAttempts.first().diagnostic
            val candidateCalls = errorAttempts.flatMap { it.candidateCalls.map { call -> call.asKaCall() } }
            KaBaseErrorCallInfo(candidateCalls, firstDiagnostic)
        },
    )

    private fun KaMultiCallResolutionAttempt.toSymbolResolutionAttempt(): KaSymbolResolutionAttempt =
        mergeSymbolAttempts(attempts.map { it.toSingleSymbolResolutionAttempt() })

    private fun KaSingleCallResolutionAttempt.toSingleSymbolResolutionAttempt(): KaSingleSymbolResolutionAttempt = when (this) {
        is KaCallResolutionSuccess -> KaBaseSymbolResolutionSuccess(backingSymbol = call.symbol)
        is KaCallResolutionError -> KaBaseSymbolResolutionError(
            backingDiagnostic = diagnostic,
            backingCandidateSymbols = candidateCalls.map { it.symbol },
        )
    }

    /**
     * Merges individual symbol resolution attempts into a single result, satisfying the
     * [KaCompoundSymbolResolutionError] contract: at most one [KaSymbolResolutionSuccess]
     * (combining all successful symbols) and at least one [KaSymbolResolutionError].
     */
    private fun mergeSymbolAttempts(symbolAttempts: List<KaSingleSymbolResolutionAttempt>): KaSymbolResolutionAttempt {
        val successSymbols = mutableListOf<KaSymbol>()
        val errors = mutableListOf<KaSymbolResolutionError>()

        for (attempt in symbolAttempts) when (attempt) {
            is KaSymbolResolutionSuccess -> successSymbols.addAll(attempt.symbols)
            is KaSymbolResolutionError -> errors.add(attempt)
        }

        if (errors.isEmpty()) {
            return KaBaseSymbolResolutionSuccess(successSymbols)
        }

        if (symbolAttempts.size == 1) {
            return errors.single()
        }

        val merged = buildList {
            if (successSymbols.isNotEmpty()) {
                add(KaBaseSymbolResolutionSuccess(successSymbols))
            }

            addAll(errors)
        }

        return KaBaseCompoundSymbolResolutionError(backingAttempts = merged)
    }

    protected companion object {
        private val nonCallBinaryOperator: TokenSet = TokenSet.create(
            KtTokens.ELVIS,
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ,
            KtTokens.ANDAND,
            KtTokens.OROR,
        )
    }
}

internal fun KaCallCandidateInfo.asKaCallCandidate(): KaCallCandidate {
    val call = candidate as KaSingleOrMultiCall
    return when (this) {
        is KaApplicableCallCandidateInfo -> KaBaseApplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
        )

        is KaInapplicableCallCandidateInfo -> KaBaseInapplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
            backingDiagnostic = diagnostic,
        )
    }
}

internal fun KaCallCandidate.asKaCallCandidateInfo(): KaCallCandidateInfo {
    val call = candidate as KaCall
    return when (this) {
        is KaApplicableCallCandidate -> KaBaseApplicableCallCandidateInfo(
            backingCandidate = call,
            isInBestCandidates = isInBestCandidates,
        )

        is KaInapplicableCallCandidate -> KaBaseInapplicableCallCandidateInfo(
            backingCandidate = call,
            isInBestCandidates = isInBestCandidates,
            diagnostic = diagnostic,
        )
    }
}

@OptIn(KtExperimentalApi::class)
private fun ExceptionAttachmentBuilder.withResolvableEntry(resolvable: KtResolvable) {
    if (resolvable is PsiElement) {
        withPsiEntry("psi", resolvable)
    } else {
        withEntry("ktResolvableCallClass", resolvable::class.simpleName)
    }
}
