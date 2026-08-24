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
        is KaSimpleCallResolutionAttempt -> callAttempt.toSimpleSymbolResolutionAttempt()
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
            is KaCallResolutionError -> callAttempt.toSimpleSymbolResolutionAttempt()

            // Single variable access is not expected to be a result of the symbol resolve (the assignment use case)
            is KaCallResolutionSuccess if callAttempt.call !is KaVariableAccessCall -> callAttempt.toSimpleSymbolResolutionAttempt()
            is KaMultiCallResolutionAttempt -> when (callAttempt) {
                is KaCompoundArrayAccessCallResolutionAttempt -> mergeSymbolAttempts(
                    listOf(
                        callAttempt.operationCallAttempt.toSimpleSymbolResolutionAttempt(),
                        callAttempt.setterCallAttempt.toSimpleSymbolResolutionAttempt(),
                    )
                )

                is KaCompoundVariableAccessCallResolutionAttempt -> callAttempt.operationCallAttempt.toSimpleSymbolResolutionAttempt()
                else -> callAttempt.toSymbolResolutionAttempt()
            }

            else -> null
        }
    }

    private fun <T> T.tryResolveSymbolsForElement(): KaSymbolResolutionAttempt? where T : KtResolvable, T : KtElement {
        checkValidity()
        return performSymbolResolution(this)
    }

    final override fun resolveSuccessfulSymbols(resolvable: KtResolvable): Collection<KaSymbol> = withValidityAssertion {
        tryResolveSymbols(resolvable)?.successfulSymbols ?: emptyList()
    }

    final override fun resolveSuccessfulSymbol(resolvable: KtResolvable): KaSymbol? = withValidityAssertion {
        resolveSuccessfulSymbols(resolvable).singleOrNull()
    }

    private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(): R? = resolveSuccessfulSymbol(this) as? R

    final override fun resolveSuccessfulSymbol(annotationEntry: KtAnnotationEntry): KaConstructorSymbol? = annotationEntry.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(superTypeCallEntry: KtSuperTypeCallEntry): KaConstructorSymbol? =
        superTypeCallEntry.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(constructorDelegationCall: KtConstructorDelegationCall): KaConstructorSymbol? =
        constructorDelegationCall.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaConstructorSymbol? =
        constructorDelegationReferenceExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(callElement: KtCallElement): KaFunctionSymbol? = callElement.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(callableReferenceExpression: KtCallableReferenceExpression): KaCallableSymbol? =
        callableReferenceExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(arrayAccessExpression: KtArrayAccessExpression): KaNamedFunctionSymbol? =
        arrayAccessExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(collectionLiteralExpression: KtCollectionLiteralExpression): KaNamedFunctionSymbol? =
        collectionLiteralExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaNamedClassSymbol? =
        enumEntrySuperclassReferenceExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(labelReferenceExpression: KtLabelReferenceExpression): KaDeclarationSymbol? =
        labelReferenceExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(returnExpression: KtReturnExpression): KaFunctionSymbol? = returnExpression.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(whenConditionInRange: KtWhenConditionInRange): KaNamedFunctionSymbol? =
        whenConditionInRange.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaCallableSymbol? =
        destructuringDeclarationEntry.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(qualifiedExpression: KtQualifiedExpression): KaCallableSymbol? =
        qualifiedExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(constructorCalleeExpression: KtConstructorCalleeExpression): KaConstructorSymbol? =
        constructorCalleeExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(instanceExpressionWithLabel: KtInstanceExpressionWithLabel): KaDeclarationSymbol? =
        instanceExpressionWithLabel.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(nullableType: KtNullableType): KaClassifierSymbol? = nullableType.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(functionType: KtFunctionType): KaClassSymbol? = functionType.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(typeReference: KtTypeReference): KaClassifierSymbol? = typeReference.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(classLiteralExpression: KtClassLiteralExpression): KaClassifierSymbol? =
        classLiteralExpression.resolveSymbolSafe()

    final override fun resolveSuccessfulSymbol(superTypeEntry: KtSuperTypeEntry): KaClassifierSymbol? = superTypeEntry.resolveSymbolSafe()
    final override fun resolveSuccessfulSymbol(delegatedSuperTypeEntry: KtDelegatedSuperTypeEntry): KaClassifierSymbol? =
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

    final override fun resolveSuccessfulCall(resolvableCall: KtResolvableCall): KaSimpleOrMultiCall? = tryResolveCall(resolvableCall)?.successful

    private inline fun <reified R : KaSimpleOrMultiCall> KtResolvableCall.resolveCallSafe(): R? = resolveSuccessfulCall(this) as? R

    private inline fun <reified S : KaCallableSymbol, C : KaCallableSignature<S>, reified R : KaSimpleCall<S, C>> KtResolvableCall.resolveSimpleCallSafe(): R? {
        val call = resolveSuccessfulCall(this) ?: return null
        checkWithAttachment(
            call is KaSimpleCall<*, *>,
            { "Expected call of type ${KaSimpleCall::class.simpleName}, got ${call::class.simpleName}" },
        ) {
            withResolvableEntry(this@resolveSimpleCallSafe)
        }

        val callableSymbol = call.symbol
        checkWithAttachment(
            callableSymbol is S,
            { "Expected symbol of type ${S::class.simpleName}, got ${callableSymbol::class.simpleName}" },
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withResolvableEntry(this@resolveSimpleCallSafe)
        }

        checkWithAttachment(
            call is R,
            { "Expected call of type ${R::class.simpleName}, got ${call::class.simpleName}" }
        ) {
            withEntry("symbol", callableSymbol) {
                KaDebugRenderer(renderExtra = true).render(analysisSession, callableSymbol)
            }

            withResolvableEntry(this@resolveSimpleCallSafe)
        }

        return call
    }

    final override fun resolveSuccessfulCall(annotationEntry: KtAnnotationEntry): KaAnnotationCall? = annotationEntry.resolveSimpleCallSafe()
    final override fun resolveSuccessfulCall(superTypeCallEntry: KtSuperTypeCallEntry): KaFunctionCall<KaConstructorSymbol>? =
        superTypeCallEntry.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(constructorDelegationCall: KtConstructorDelegationCall): KaDelegatedConstructorCall? =
        constructorDelegationCall.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaDelegatedConstructorCall? =
        constructorDelegationReferenceExpression.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(callElement: KtCallElement): KaFunctionCall<*>? = callElement.resolveCallSafe()
    final override fun resolveSuccessfulCall(callableReferenceExpression: KtCallableReferenceExpression): KaCallableReferenceCall<*, *>? =
        callableReferenceExpression.resolveCallSafe()

    final override fun resolveSuccessfulCall(arrayAccessExpression: KtArrayAccessExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        arrayAccessExpression.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(collectionLiteralExpression: KtCollectionLiteralExpression): KaFunctionCall<KaNamedFunctionSymbol>? =
        collectionLiteralExpression.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaDelegatedConstructorCall? =
        enumEntrySuperclassReferenceExpression.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(whenConditionInRange: KtWhenConditionInRange): KaFunctionCall<KaNamedFunctionSymbol>? =
        whenConditionInRange.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaSimpleCall<*, *>? =
        destructuringDeclarationEntry.resolveCallSafe()

    final override fun resolveSuccessfulCall(qualifiedExpression: KtQualifiedExpression): KaSimpleCall<*, *>? = qualifiedExpression.resolveCallSafe()
    final override fun resolveSuccessfulCall(forExpression: KtForExpression): KaForLoopCall? = forExpression.resolveCallSafe()
    final override fun resolveSuccessfulCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCall? = propertyDelegate.resolveCallSafe()

    final override fun tryResolveCall(forExpression: KtForExpression): KaForLoopCallResolutionAttempt? =
        forExpression.tryResolveCallImpl() as? KaForLoopCallResolutionAttempt

    final override fun tryResolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCallResolutionAttempt? =
        propertyDelegate.tryResolveCallImpl() as? KaDelegatedPropertyCallResolutionAttempt

    final override fun resolveSuccessfulCall(constructorCalleeExpression: KtConstructorCalleeExpression): KaFunctionCall<KaConstructorSymbol>? =
        constructorCalleeExpression.resolveSimpleCallSafe()

    final override fun resolveSuccessfulCall(nameReferenceExpression: KtNameReferenceExpression): KaSimpleCall<*, *>? =
        nameReferenceExpression.resolveCallSafe()

    final override fun resolveToCall(element: KtElement): KaCallInfo? = element.withPsiValidityAssertion {
        when (val attempt = element.tryResolveCallImpl()) {
            is KaCallResolutionError -> KaBaseErrorCallInfo(attempt.candidateCalls.map { it.asKaCall() }, attempt.diagnostic)
            is KaCallResolutionSuccess -> KaBaseSuccessCallInfo(attempt.call.asKaCall())
            is KaMultiCallResolutionAttempt -> attempt.toCallInfo()
            null -> null
        }
    }

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
        mergeSymbolAttempts(attempts.map { it.toSimpleSymbolResolutionAttempt() })

    private fun KaSimpleCallResolutionAttempt.toSimpleSymbolResolutionAttempt(): KaSimpleSymbolResolutionAttempt = when (this) {
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
    private fun mergeSymbolAttempts(symbolAttempts: List<KaSimpleSymbolResolutionAttempt>): KaSymbolResolutionAttempt {
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
    val call = candidate as KaSimpleOrMultiCall
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
    val call = candidate.asKaCall()
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

/**
 * Returns the legacy [KaCall] view of [this] [KaSimpleOrMultiCall]. Most resolution result types
 * implement [KaCall] directly. The exception is [KaCallableReferenceCall], which is part of the
 * new resolution API and intentionally does not extend the deprecated [KaCall] hierarchy. For
 * that case we emulate a legacy [KaCall] by re-encoding the reference as the corresponding
 * [KaSimpleFunctionCall] / [KaSimpleVariableAccessCall] view.
 */
private fun KaSimpleOrMultiCall.asKaCall(): KaCall = when (this) {
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

private fun ExceptionAttachmentBuilder.withResolvableEntry(resolvable: KtResolvable) {
    if (resolvable is PsiElement) {
        withPsiEntry("psi", resolvable)
    } else {
        withEntry("ktResolvableCallClass", resolvable::class.simpleName)
    }
}
