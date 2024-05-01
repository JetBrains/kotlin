/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.calls.KaCallResolutionAttempt
import org.jetbrains.kotlin.analysis.api.calls.KaCallResolutionError
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.calls.KtErrorCallInfo
import org.jetbrains.kotlin.analysis.api.calls.KtSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.KtResolvable
import org.jetbrains.kotlin.resolve.KtResolvableCall

public abstract class KaResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveCallElementToSymbol(callElement: KtCallElement): KtCallableSymbol?
}

public interface KaResolverMixIn : KtAnalysisSessionMixIn {
    // ------------------------------ resolveSymbol ------------------------------

    public fun KtResolvable.resolveSymbol(): KtSymbol? = withValidityAssertion {
        when (this) {
            is KtCallElement -> analysisSession.resolver.resolveCallElementToSymbol(this)
            is KtReference -> analysisSession.referenceResolveProvider.resolveToSymbols(this).singleOrNull()
            else -> null
        }
    }

    // ------------------------------ resolveCall --------------------------------

    public fun KtResolvableCall.attemptResolveCall(): KaCallResolutionAttempt? = withValidityAssertion {
        if (this !is KtElement) return@withValidityAssertion null

        when (val callInfo = analysisSession.callResolver.resolveCall(this)) {
            is KtErrorCallInfo -> KaCallResolutionError(callInfo.candidateCalls, callInfo.diagnostic, callInfo.token)
            is KtSuccessCallInfo -> callInfo.call
            null -> null
        }
    }

    public fun KtResolvableCall.resolveCall(): KtCall? = attemptResolveCall() as? KtCall

    // ------------------------------ collectCallCandidates ----------------------

    public fun KtResolvableCall.collectCallCandidates(): List<KtCallCandidateInfo> = withValidityAssertion {
        if (this !is KtElement) return@withValidityAssertion emptyList()

        analysisSession.callResolver.collectCallCandidates(this)
    }
}
