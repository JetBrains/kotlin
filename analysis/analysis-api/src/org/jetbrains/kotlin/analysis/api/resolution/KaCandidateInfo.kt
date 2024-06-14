/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * A candidate considered for a call. I.e., one of the overload candidates in scope at the call site.
 */
public sealed class KaCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
) : KaLifetimeOwner {
    private val backingCandidate: KaCall = candidate

    override val token: KaLifetimeToken get() = backingCandidate.token
    public val candidate: KaCall get() = withValidityAssertion { backingCandidate }

    /**
     * Returns true if the [candidate] is in the final set of candidates that the call is actually resolved to. There can be multiple
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean by validityAsserted(isInBestCandidates)
}

@Deprecated("Use 'KaCallCandidateInfo' instead", ReplaceWith("KaCallCandidateInfo"))
public typealias KtCallCandidateInfo = KaCallCandidateInfo

/**
 * A candidate that is applicable for a call. A candidate is applicable if the call's arguments are complete and are assignable to the
 * candidate's parameters, AND the call's type arguments are complete and fit all the constraints of the candidate's type parameters.
 */
public class KaApplicableCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
) : KaCallCandidateInfo(candidate, isInBestCandidates)

@Deprecated("Use 'KaApplicableCallCandidateInfo' instead", ReplaceWith("KaApplicableCallCandidateInfo"))
public typealias KtApplicableCallCandidateInfo = KaApplicableCallCandidateInfo

/**
 * A candidate that is NOT applicable for a call. A candidate is inapplicable if a call argument is missing or is not assignable to the
 * candidate's parameters, OR a call type argument is missing or does not fit the constraints of the candidate's type parameters.
 */
public class KaInapplicableCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
    diagnostic: KaDiagnostic,
) : KaCallCandidateInfo(candidate, isInBestCandidates) {
    /**
     * The reason the [candidate] was not applicable for the call (e.g., argument type mismatch, or no value for parameter).
     */
    public val diagnostic: KaDiagnostic by validityAsserted(diagnostic)
}