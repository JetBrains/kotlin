/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionError
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedConstructorCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionSuccess
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall

@KaAnalysisApiInternals
public abstract class KaResolver : KaSessionComponent() {
    public abstract fun resolveToSymbols(reference: KtReference): Collection<KaSymbol>

    public abstract fun attemptResolveSymbol(psi: KtElement): KaSymbolResolutionAttempt?
    public abstract fun attemptResolveCall(psi: KtElement): KaCallResolutionAttempt?
    public abstract fun collectCallCandidates(psi: KtElement): List<KaCallCandidateInfo>
}

@OptIn(KaAnalysisApiInternals::class)
public typealias KtCallResolver = KaResolver

@OptIn(KaAnalysisApiInternals::class)
public interface KaResolverMixIn : KaSessionMixIn {
    public fun KtResolvable.attemptResolveSymbol(): KaSymbolResolutionAttempt? = withValidityAssertion {
        if (this !is KtElement) return@withValidityAssertion null

        analysisSession.resolver.attemptResolveSymbol(this)
    }

    public fun KtResolvable.resolveSymbol(): KaSymbol? = when (val attempt = attemptResolveSymbol()) {
        is KaSymbolResolutionSuccess -> attempt.symbol
        else -> null
    }

    public fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe(analysisSession)
    public fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe(analysisSession)
    public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? = resolveSymbolSafe(analysisSession)
    public fun KtCallExpression.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe(analysisSession)
    public fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? = resolveSymbolSafe(analysisSession)

    public fun KtResolvableCall.attemptResolveCall(): KaCallResolutionAttempt? = withValidityAssertion {
        if (this !is KtElement) return@withValidityAssertion null

        analysisSession.resolver.attemptResolveCall(this)
    }

    public fun KtResolvableCall.resolveCall(): KaCall? = attemptResolveCall() as? KaCall

    public fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = resolveCallSafe(analysisSession)
    public fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveCallSafe(analysisSession)
    public fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = resolveCallSafe(analysisSession)
    public fun KtCallExpression.resolveCall(): KaCallableMemberCall<*, *>? = resolveCallSafe(analysisSession)
    public fun KtCallableReferenceExpression.resolveCall(): KaCallableMemberCall<*, *>? = resolveCallSafe(analysisSession)

    public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidateInfo> = withValidityAssertion {
        if (this !is KtElement) return@withValidityAssertion emptyList()

        analysisSession.resolver.collectCallCandidates(this)
    }

    @Deprecated(
        message = "The API will be changed soon. Use 'resolveCallOld()' in a transit period",
        replaceWith = ReplaceWith("resolveCallOld()"),
        level = DeprecationLevel.HIDDEN,
    )
    public fun KtElement.resolveCall(): KaCallInfo? = resolveCallOld()

    public fun KtElement.resolveCallOld(): KaCallInfo? = withValidityAssertion {
        when (val attempt = analysisSession.resolver.attemptResolveCall(this)) {
            is KaCallResolutionError -> KaErrorCallInfo(attempt.candidateCalls, attempt.diagnostic)
            is KaCall -> KaSuccessCallInfo(attempt)
            null -> null
        }
    }

    @Deprecated(
        message = "The API will be changed soon. Use 'collectCallCandidatesOld()' in a transit period",
        replaceWith = ReplaceWith("collectCallCandidatesOld()"),
        level = DeprecationLevel.HIDDEN,
    )
    public fun KtElement.collectCallCandidates(): List<KaCallCandidateInfo> = collectCallCandidatesOld()

    /**
     * Returns all the candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * [resolveCallOld] only returns the final result of overload resolution, i.e., the selected callable after considering candidate
     * applicability and choosing the most specific candidate.
     */
    public fun KtElement.collectCallCandidatesOld(): List<KaCallCandidateInfo> = withValidityAssertion {
        analysisSession.resolver.collectCallCandidates(this)
    }
}

private inline fun <reified R : KaSymbol> KtResolvable.resolveSymbolSafe(session: KaSession): R? = with(session) {
    resolveSymbol() as? R
}

private inline fun <reified R : KaCall> KtResolvableCall.resolveCallSafe(session: KaSession): R? = with(session) {
    resolveCall() as? R
}

public typealias KtCallResolverMixIn = KaResolverMixIn