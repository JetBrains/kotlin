/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@KaImplementationDetail
@OptIn(KtExperimentalApi::class)
abstract class KaBaseResolver<T : KaSession> : KaBaseSessionComponent<T>(), KaResolver {
    protected abstract fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt?

    final override fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? = withValidityAssertion {
        if (this is KtElement) {
            checkValidity()
            performSymbolResolution(this)
        } else {
            null
        }
    }

    final override fun KtResolvable.resolveSymbols(): Collection<KaSymbol> = withValidityAssertion {
        when (val attempt = tryResolveSymbols()) {
            is KaSymbolResolutionSuccess -> attempt.symbols
            is KaSymbolResolutionError, null -> emptyList()
        }
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
    final override fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? = resolveSymbolSafe()
    final override fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? = resolveSymbolSafe()
    final override fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? = resolveSymbolSafe()
    final override fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()

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

    final override fun KtResolvableCall.resolveCall(): KaSingleOrMultiCall? = (tryResolveCall() as? KaCallResolutionSuccess)?.call

    private inline fun <reified R : KaSingleOrMultiCall> KtResolvableCall.resolveCallSafe(): R? = resolveCall() as? R

    private inline fun <reified S : KaCallableSymbol, C : KaCallableSignature<S>, reified R : KaSingleCall<S, C>> KtResolvableCall.resolveSingleCallSafe(): R? {
        val call = resolveCall() ?: return null
        checkWithAttachment(
            call is KaSingleCall<*, *>,
            { "Expected call of type ${KaSingleCall::class.simpleName}, got ${call::class.simpleName}" },
        ) {
            withResolvableEntry(this@resolveSingleCallSafe)
        }

        val callableSymbol = call.signature.symbol
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
    final override fun KtCallableReferenceExpression.resolveCall(): KaSingleCall<*, *>? = resolveCallSafe()
    final override fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? = resolveSingleCallSafe()
    final override fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSingleCallSafe()
    final override fun KtDestructuringDeclarationEntry.resolveCall(): KaSingleCall<*, *>? = resolveCallSafe()

    final override fun KtElement.resolveToCall(): KaCallInfo? = withPsiValidityAssertion {
        when (val attempt = tryResolveCallImpl()) {
            is KaCallResolutionError -> KaBaseErrorCallInfo(attempt.candidateCalls.map { it.asKaCall() }, attempt.diagnostic)
            is KaCallResolutionSuccess -> KaBaseSuccessCallInfo(attempt.kaCall)
            null -> null
        }
    }

    /**
     * All implementations of KaSingleOrMultiCall are also KaCall
     * */
    @OptIn(ExperimentalContracts::class)
    protected fun KaSingleOrMultiCall.asKaCall(): KaCall {
        contract {
            returns() implies (this@asKaCall is KaCall)
        }

        return this as KaCall
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

    // TODO: remove this workaround after KT-68499
    protected fun resolveDefaultAnnotationArgumentReference(
        reference: KtDefaultAnnotationArgumentReference,
    ): Collection<KaSymbol> = with(analysisSession) {
        val symbol = when (val psi = reference.resolve()) {
            is KtDeclaration -> psi.symbol
            is PsiClass -> psi.namedClassSymbol
            is PsiMember -> psi.callableSymbol
            else -> null
        }

        listOfNotNull(symbol)
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
        is KtCallElement -> true
        is KtConstructorCalleeExpression -> true
        is KtQualifiedExpression -> true
        is KtNameReferenceExpression -> true
        is KtOperationExpression -> true
        is KtArrayAccessExpression -> true
        is KtCallableReferenceExpression -> true
        is KtWhenConditionInRange -> true
        is KtCollectionLiteralExpression -> true
        is KtConstructorDelegationReferenceExpression -> true
        is KtEnumEntrySuperclassReferenceExpression -> true
        is KtDestructuringDeclarationEntry -> true
        else -> false
    }

    private fun KtElement.unwrapResolvableCall(): KtElement? = when (this) {
        // Most likely we will drop call resolution for operators, and only resolveSymbol will be available for them.
        // Call resolution API is available on a parent expression (like binary or unary operator)
        is KtOperationReferenceExpression -> parent as? KtElement
        else -> this
    }?.takeIf(::canBeResolvedAsCall)

    protected companion object {
        private val nonCallBinaryOperator: Set<KtSingleValueToken> = setOf(KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)
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
