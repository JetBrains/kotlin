/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseErrorCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall

@KaImplementationDetail
@OptIn(KtExperimentalApi::class)
abstract class KaBaseResolver<T : KaSession> : KaBaseSessionComponent<T>(), KaResolver {
    protected abstract fun performSymbolResolution(psi: KtElement): KaSymbolResolutionAttempt?

    final override fun KtResolvable.tryResolveSymbol(): KaSymbolResolutionAttempt? = withValidityAssertion {
        if (this is KtElement) {
            checkValidity()
            performSymbolResolution(this)
        } else {
            null
        }
    }

    final override fun KtResolvable.resolveSymbols(): Collection<KaSymbol> = withValidityAssertion {
        when (val attempt = tryResolveSymbol()) {
            is KaSingleSymbolResolutionSuccess -> listOf(attempt.symbol)
            is KaMultiSymbolResolutionSuccess -> attempt.symbols
            else -> emptyList()
        }
    }

    final override fun KtResolvable.resolveSymbol(): KaSymbol? = withValidityAssertion {
        when (val attempt = tryResolveSymbol()) {
            is KaSingleSymbolResolutionSuccess -> attempt.symbol
            else -> null
        }
    }

    private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(): R? = resolveSymbol() as? R

    final override fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe()
    final override fun KtCallElement.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()
    final override fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe()

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

    final override fun KtResolvableCall.resolveCall(): KaCall? = tryResolveCall() as? KaCall

    private inline fun <reified R : KaCall> KtResolvableCall.resolveCallSafe(): R? = resolveCall() as? R

    final override fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = resolveCallSafe()
    final override fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveCallSafe()
    final override fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = resolveCallSafe()
    final override fun KtCallElement.resolveCall(): KaCallableMemberCall<*, *>? = resolveCallSafe()
    final override fun KtCallableReferenceExpression.resolveCall(): KaCallableMemberCall<*, *>? = resolveCallSafe()

    final override fun KtElement.resolveToCall(): KaCallInfo? = withPsiValidityAssertion {
        when (val attempt = tryResolveCallImpl()) {
            is KaCallResolutionError -> KaBaseErrorCallInfo(attempt.candidateCalls, attempt.diagnostic)
            is KaCall -> KaBaseSuccessCallInfo(attempt)
            null -> null
        }
    }

    private fun KtElement.collectCallCandidatesImpl(): List<KaCallCandidateInfo> {
        val unwrappedElement = unwrapResolvableCall()
        return unwrappedElement?.let(::performCallCandidatesCollection).orEmpty()
    }

    protected abstract fun performCallCandidatesCollection(psi: KtElement): List<KaCallCandidateInfo>

    final override fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidateInfo> = withValidityAssertion {
        if (this is KtElement) {
            checkValidity()
            collectCallCandidatesImpl()
        } else {
            emptyList()
        }
    }

    final override fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> = withPsiValidityAssertion {
        collectCallCandidatesImpl()
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
