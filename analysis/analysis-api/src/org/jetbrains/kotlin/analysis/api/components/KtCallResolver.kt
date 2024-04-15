/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.calls.KtCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement

public abstract class KtCallResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveCall(psi: KtElement): KtCallInfo?

    public abstract fun collectCallCandidates(psi: KtElement): List<KtCallCandidateInfo>
}

@OptIn(KtAnalysisApiInternals::class)
public interface KtCallResolverMixIn : KtAnalysisSessionMixIn {
    /**
     * @see KaResolverMixIn.resolveSymbol
     * @see KaResolverMixIn.resolveCall
     * @see KaResolverMixIn.attemptResolveCall
     */
//    @Deprecated(
//        "The API is deprecated and will be removed soon",
//        ReplaceWith(
//            expression = "(this as? KtResolvableCall)?.attemptResolveCall()",
//            imports = ["org.jetbrains.kotlin.resolve.KtResolvableCall"],
//        ),
//    )
    public fun KtElement.resolveCallOld(): KtCallInfo? = withValidityAssertion {
        analysisSession.callResolver.resolveCall(this)
    }

    /**
     * Returns all the candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * [resolveCallOld] only returns the final result of overload resolution, i.e., the selected callable after considering candidate
     * applicability and choosing the most specific candidate.
     *
     * @see KaResolverMixIn.collectCallCandidates
     */
//    @Deprecated(
//        "The API is deprecated and will be removed soon",
//        ReplaceWith(
//            expression = "(this as? KtResolvableCall)?.collectCallCandidates()",
//            imports = ["org.jetbrains.kotlin.resolve.KtResolvableCall"],
//        ),
//    )
    public fun KtElement.collectCallCandidatesOld(): List<KtCallCandidateInfo> = withValidityAssertion {
        analysisSession.callResolver.collectCallCandidates(this)
    }
}
