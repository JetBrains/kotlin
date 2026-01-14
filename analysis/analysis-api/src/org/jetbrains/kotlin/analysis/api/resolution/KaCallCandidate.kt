/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * [KaCallCandidate] represents one of the candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
 * of a call.
 *
 * In contrast to [KaCallResolutionError.candidateCalls], retrieving a call candidate represented by [KaCallCandidate] doesn't imply that the
 * call is erroneous or ambiguous. Rather, resolving all call candidates is helpful when analyzing all possible options at the call site.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaResolver.collectCallCandidates
 */
@KaExperimentalApi
public sealed interface KaCallCandidate : KaLifetimeOwner {
    /**
     * The [KaSingleCall] representing the call candidate.
     */
    public val candidate: KaSingleCall<*, *>

    /**
     * Whether the [candidate] is in the final set of candidates considered during the call's resolution. There can be multiple best
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean
}

/**
 * A [call candidate][KaCallCandidate] that is applicable for a call.
 *
 * A candidate is applicable if:
 *
 * - Its arguments are complete and assignable to its parameters.
 * - Its type arguments are complete and fit all the constraints of its type parameters.
 *
 * In other words, given the arguments and type arguments, the candidate *could* be called at that specific call site.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaApplicableCallCandidate : KaCallCandidate

/**
 * A [call candidate][KaCallCandidate] that is *not* applicable for a call.
 *
 * A candidate is inapplicable if either:
 *
 * - An argument is missing or is not assignable to the corresponding parameter.
 * - A type argument is missing or does not fit the constraints of the corresponding type parameter.
 *
 * In other words, given the arguments and type arguments, the candidate *could not* be called at that specific call site.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInapplicableCallCandidate : KaCallCandidate {
    /**
     * The reason for the [candidate]'s missing applicability (e.g. argument type mismatch or no value for a parameter).
     */
    public val diagnostic: KaDiagnostic
}
