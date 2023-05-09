/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.calls.KtCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.calls.KtErrorCallInfo
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtUnaryExpression

public abstract class KtCallResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveCall(psi: KtElement): KtCallInfo?

    public abstract fun collectCallCandidates(psi: KtElement): List<KtCallCandidateInfo>

    @KtAnalysisApiInternals
    public open fun unresolvedKtCallError(psi: KtElement): KtErrorCallInfo {
        return KtErrorCallInfo(
            _candidateCalls = emptyList(),
            KtNonBoundToPsiErrorDiagnostic(factoryName = null, "Unresolved call", token),
            token
        )
    }
}

@OptIn(KtAnalysisApiInternals::class)
public interface KtCallResolverMixIn : KtAnalysisSessionMixIn {

    public fun KtElement.resolveCall(): KtCallInfo? =
        withValidityAssertion { analysisSession.callResolver.resolveCall(this) }

    public fun KtCallElement.resolveCall(): KtCallInfo = withValidityAssertion {
        analysisSession.callResolver.resolveCall(this)
            ?: analysisSession.callResolver.unresolvedKtCallError(this)
    }

    public fun KtUnaryExpression.resolveCall(): KtCallInfo = withValidityAssertion {
        analysisSession.callResolver.resolveCall(this)
            ?: analysisSession.callResolver.unresolvedKtCallError(this)
    }


    public fun KtArrayAccessExpression.resolveCall(): KtCallInfo = withValidityAssertion {
        analysisSession.callResolver.resolveCall(this)
            ?: analysisSession.callResolver.unresolvedKtCallError(this)
    }

    /**
     * Returns all the candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * [resolveCall] only returns the final result of overload resolution, i.e., the selected callable after considering candidate
     * applicability and choosing the most specific candidate.
     */
    public fun KtElement.collectCallCandidates(): List<KtCallCandidateInfo> =
        withValidityAssertion { analysisSession.callResolver.collectCallCandidates(this) }
}
