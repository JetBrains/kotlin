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
import org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolver
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

@KaImplementationDetail
@OptIn(KtExperimentalApi::class)
abstract class KaBaseResolver<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsResolver {
    protected abstract fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt?

    final override fun tryResolveSymbols(resolvable: KtResolvable): KaSymbolResolutionAttempt? = withValidityAssertion {
        when (resolvable) {
            is KtOperationReferenceExpression -> resolvable.tryResolveSymbolsForOperationReference()
            is KtResolvableCall -> resolvable.tryResolveSymbolsForResolvableCall()
            is KtElement -> resolvable.tryResolveSymbolsForElement()
            else -> null
        }
    }

    override fun resolveToSymbols(reference: KtReference): Collection<KaSymbol> = withPsiValidityAssertion(reference.element) {
        with(reference as? KaResolvableReferenceBridge) {
            if (this != null) {
                analysisSession.resolveToSymbols()
            } else {
                emptyList()
            }
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
    } ?: when (val callAttempt = tryResolveCall(this)) {
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
        return when (val callAttempt = tryResolveCall(this)) {
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

    final override fun resolveSymbols(resolvable: KtResolvable): Collection<KaSymbol> = withValidityAssertion {
        tryResolveSymbols(resolvable)?.successfulSymbols ?: emptyList()
    }

    final override fun resolveSymbol(resolvable: KtResolvable): KaSymbol? = withValidityAssertion {
        resolveSymbols(resolvable).singleOrNull()
    }

    private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(): R? = resolveSymbol(this) as? R

    final override fun resolveSymbol(annotationEntry: KtAnnotationEntry): KaConstructorSymbol? = annotationEntry.resolveSymbolSafe()
    final override fun resolveSymbol(superTypeCallEntry: KtSuperTypeCallEntry): KaConstructorSymbol? =
        superTypeCallEntry.resolveSymbolSafe()

    final override fun resolveSymbol(constructorDelegationCall: KtConstructorDelegationCall): KaConstructorSymbol? =
        constructorDelegationCall.resolveSymbolSafe()

    final override fun resolveSymbol(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaConstructorSymbol? =
        constructorDelegationReferenceExpression.resolveSymbolSafe()

    final override fun resolveSymbol(callElement: KtCallElement): KaFunctionSymbol? = callElement.resolveSymbolSafe()
    final override fun resolveSymbol(callableReferenceExpression: KtCallableReferenceExpression): KaCallableSymbol? =
        callableReferenceExpression.resolveSymbolSafe()

    final override fun resolveSymbol(arrayAccessExpression: KtArrayAccessExpression): KaNamedFunctionSymbol? =
        arrayAccessExpression.resolveSymbolSafe()

    final override fun resolveSymbol(collectionLiteralExpression: KtCollectionLiteralExpression): KaNamedFunctionSymbol? =
        collectionLiteralExpression.resolveSymbolSafe()

    final override fun resolveSymbol(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaNamedClassSymbol? =
        enumEntrySuperclassReferenceExpression.resolveSymbolSafe()

    final override fun resolveSymbol(labelReferenceExpression: KtLabelReferenceExpression): KaDeclarationSymbol? =
        labelReferenceExpression.resolveSymbolSafe()

    final override fun resolveSymbol(returnExpression: KtReturnExpression): KaFunctionSymbol? = returnExpression.resolveSymbolSafe()
    final override fun resolveSymbol(whenConditionInRange: KtWhenConditionInRange): KaNamedFunctionSymbol? =
        whenConditionInRange.resolveSymbolSafe()

    final override fun resolveSymbol(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaCallableSymbol? =
        destructuringDeclarationEntry.resolveSymbolSafe()

    final override fun resolveSymbol(qualifiedExpression: KtQualifiedExpression): KaCallableSymbol? =
        qualifiedExpression.resolveSymbolSafe()

    final override fun resolveSymbol(constructorCalleeExpression: KtConstructorCalleeExpression): KaConstructorSymbol? =
        constructorCalleeExpression.resolveSymbolSafe()

    final override fun resolveSymbol(instanceExpressionWithLabel: KtInstanceExpressionWithLabel): KaDeclarationSymbol? =
        instanceExpressionWithLabel.resolveSymbolSafe()

    final override fun resolveSymbol(nullableType: KtNullableType): KaClassifierSymbol? = nullableType.resolveSymbolSafe()
    final override fun resolveSymbol(functionType: KtFunctionType): KaClassSymbol? = functionType.resolveSymbolSafe()
    final override fun resolveSymbol(typeReference: KtTypeReference): KaClassifierSymbol? = typeReference.resolveSymbolSafe()
    final override fun resolveSymbol(classLiteralExpression: KtClassLiteralExpression): KaClassifierSymbol? =
        classLiteralExpression.resolveSymbolSafe()

    final override fun resolveSymbol(superTypeEntry: KtSuperTypeEntry): KaClassifierSymbol? = superTypeEntry.resolveSymbolSafe()
    final override fun resolveSymbol(delegatedSuperTypeEntry: KtDelegatedSuperTypeEntry): KaClassifierSymbol? =
        delegatedSuperTypeEntry.resolveSymbolSafe()

    final override fun resolveToSymbol(reference: KtReference): KaSymbol? = withPsiValidityAssertion(reference.element) {
        return resolveToSymbols(reference).singleOrNull()
    }

    private fun KtElement.tryResolveCallImpl(): KaCallResolutionAttempt? {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallResolution)
    }

    protected abstract fun performCallResolution(psi: KtElement): KaCallResolutionAttempt?

    final override fun tryResolveCall(resolvableCall: KtResolvableCall): KaCallResolutionAttempt? = withValidityAssertion {
        if (resolvableCall is KtElement) {
            resolvableCall.checkValidity()
            resolvableCall.tryResolveCallImpl()
        } else {
            null
        }
    }

    final override fun resolveCall(resolvableCall: KtResolvableCall): KaSingleOrMultiCall? = tryResolveCall(resolvableCall)?.successfulCall

    private inline fun <reified R : KaSingleOrMultiCall> KtResolvableCall.resolveCallSafe(): R? = resolveCall(this) as? R

    private inline fun <reified S : KaCallableSymbol, C : KaCallableSignature<S>, reified R : KaSingleCall<S, C>> KtResolvableCall.resolveSingleCallSafe(): R? {
        val call = resolveCall(this) ?: return null
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

    final override fun resolveCall(annotationEntry: KtAnnotationEntry): KaAnnotationCall? = annotationEntry.resolveSingleCallSafe()
    final override fun resolveCall(superTypeCallEntry: KtSuperTypeCallEntry): KaFunctionCall<KaConstructorSymbol>? =
        superTypeCallEntry.resolveSingleCallSafe()

    final override fun resolveCall(constructorDelegationCall: KtConstructorDelegationCall): KaDelegatedConstructorCall? =
        constructorDelegationCall.resolveSingleCallSafe()

    final override fun resolveCall(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaDelegatedConstructorCall? =
        constructorDelegationReferenceExpression.resolveSingleCallSafe()

    final override fun resolveCall(callElement: KtCallElement): KaFunctionCall<*>? = callElement.resolveCallSafe()
    final override fun resolveCall(callableReferenceExpression: KtCallableReferenceExpression): KaCallableReferenceCall<*, *>? =
        callableReferenceExpression.resolveCallSafe()

    final override fun resolveCall(arrayAccessExpression: KtArrayAccessExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        arrayAccessExpression.resolveSingleCallSafe()

    final override fun resolveCall(collectionLiteralExpression: KtCollectionLiteralExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        collectionLiteralExpression.resolveSingleCallSafe()

    final override fun resolveCall(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaDelegatedConstructorCall? =
        enumEntrySuperclassReferenceExpression.resolveSingleCallSafe()

    final override fun resolveCall(whenConditionInRange: KtWhenConditionInRange): KaFunctionCall<KaNamedFunctionSymbol>? =
        whenConditionInRange.resolveSingleCallSafe()

    final override fun resolveCall(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaSingleCall<*, *>? =
        destructuringDeclarationEntry.resolveCallSafe()

    final override fun resolveCall(qualifiedExpression: KtQualifiedExpression): KaSingleCall<*, *>? = qualifiedExpression.resolveCallSafe()
    final override fun resolveCall(forExpression: KtForExpression): KaForLoopCall? = forExpression.resolveCallSafe()
    final override fun resolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCall? = propertyDelegate.resolveCallSafe()

    final override fun tryResolveCall(forExpression: KtForExpression): KaForLoopCallResolutionAttempt? =
        forExpression.tryResolveCallImpl() as? KaForLoopCallResolutionAttempt

    final override fun tryResolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCallResolutionAttempt? =
        propertyDelegate.tryResolveCallImpl() as? KaDelegatedPropertyCallResolutionAttempt

    final override fun resolveCall(constructorCalleeExpression: KtConstructorCalleeExpression): KaFunctionCall<KaConstructorSymbol>? =
        constructorCalleeExpression.resolveSingleCallSafe()

    final override fun resolveCall(nameReferenceExpression: KtNameReferenceExpression): KaSingleCall<*, *>? =
        nameReferenceExpression.resolveCallSafe()

    final override fun resolveToCall(element: KtElement): KaCallInfo? = element.withPsiValidityAssertion {
        when (val attempt = element.tryResolveCallImpl()) {
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

    final override fun collectCallCandidates(resolvableCall: KtResolvableCall): List<KaCallCandidate> = withValidityAssertion {
        if (resolvableCall is KtElement) {
            resolvableCall.checkValidity()
            resolvableCall.collectCallCandidatesImpl()
        } else {
            emptyList()
        }
    }

    final override fun resolveToCallCandidates(element: KtElement): List<KaCallCandidateInfo> = element.withPsiValidityAssertion {
        element.collectCallCandidatesImpl().map(KaCallCandidate::asKaCallCandidateInfo)
    }

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

    @Deprecated(
        "Use `KtSimpleNameExpression` instead",
        replaceWith = ReplaceWith(
            "(element as? KtSimpleNameExpression)?.contextSensitiveResolutionStatus is KaContextSensitiveResolutionStatus.Used",
            "org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus",
        )
    )
    final override fun usesContextSensitiveResolution(reference: KtReference): Boolean =
        withPsiValidityAssertion(reference.element) {
            (reference.element as? KtSimpleNameExpression)?.let { contextSensitiveResolutionStatus(it) } is KaContextSensitiveResolutionStatus.Used
        }

    @Deprecated(
        message = "Use `KtSimpleNameExpression` instead",
        replaceWith = ReplaceWith("(element as? KtSimpleNameExpression)?.isImplicitReferenceToCompanion() == true"),
    )
    final override fun isImplicitReferenceToCompanion(reference: KtReference): Boolean = withPsiValidityAssertion(reference.element) {
        (reference.element as? KtSimpleNameExpression)?.let { isImplicitReferenceToCompanion(it) } == true
    }

    @KaImplementationDetail
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
