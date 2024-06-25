/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * A candidate considered for a call. I.e., one of the overload candidates in scope at the call site.
 */
public sealed interface KaCallCandidateInfo : KaLifetimeOwner {
    public val candidate: KaCall

    /**
     * Returns true if the [candidate] is in the final set of candidates that the call is actually resolved to. There can be multiple
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean
}

@Deprecated("Use 'KaCallCandidateInfo' instead", ReplaceWith("KaCallCandidateInfo"))
public typealias KtCallCandidateInfo = KaCallCandidateInfo

/**
 * A candidate that is applicable for a call. A candidate is applicable if the call's arguments are complete and are assignable to the
 * candidate's parameters, AND the call's type arguments are complete and fit all the constraints of the candidate's type parameters.
 */
public interface KaApplicableCallCandidateInfo : KaCallCandidateInfo

@Deprecated("Use 'KaApplicableCallCandidateInfo' instead", ReplaceWith("KaApplicableCallCandidateInfo"))
public typealias KtApplicableCallCandidateInfo = KaApplicableCallCandidateInfo

/**
 * A candidate that is NOT applicable for a call. A candidate is inapplicable if a call argument is missing or is not assignable to the
 * candidate's parameters, OR a call type argument is missing or does not fit the constraints of the candidate's type parameters.
 */
public interface KaInapplicableCallCandidateInfo : KaCallCandidateInfo {
    /**
     * The reason the [candidate] was not applicable for the call (e.g., argument type mismatch, or no value for parameter).
     */
    public val diagnostic: KaDiagnostic
}
