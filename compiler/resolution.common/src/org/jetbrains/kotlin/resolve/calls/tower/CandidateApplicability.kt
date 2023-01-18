/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

enum class CandidateApplicability {
    /**
     * Special applicability for migration warning up to 1.5.
     * Used when resolved to function with SAM conversion and array without spread as vararg.
     */
    K1_RESOLVED_TO_SAM_WITH_VARARG,

    /**
     * Candidate is removed from resolve due to SinceKotlin with later version or Deprecation with hidden level.
     * Note that SinceKotlin does not filter out classifier symbols and property accessors. Those
     * should lead to API_NOT_AVAILABLE.
     * Provokes UNRESOLVED_REFERENCE.
     */
    HIDDEN,

    /**
     * Candidate could be successful but requires an unsupported feature.
     * Reported for references to local variables in K2.
     * Provokes UNSUPPORTED.
     */
    K2_UNSUPPORTED,

    /**
     * Candidate could be successful but receiver isn't matched
     */
    INAPPLICABLE_WRONG_RECEIVER,

    /**
     * Candidate could be successful but arguments not mapped to parameters (i.e. different size of arguments and parameters)
     */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /**
     * Candidate could be successful but arguments have wrong types (or other general inapplicability)
     */
    INAPPLICABLE,

    /**
     * Candidate could be successful but uses some non-object classifier without companion object as a variable.
     */
    K2_NO_COMPANION_OBJECT,

    /**
     * Candidate could be successful but requires access to outer class from nested (non-inner)
     */
    K1_IMPOSSIBLE_TO_GENERATE,

    // TODO: Consider re-assigning this diagnostics (K1_RUNTIME_ERROR)

    /**
     * This applicability is used in K1 as a catch-all for all other errors.
     */
    K1_RUNTIME_ERROR,

    /**
     * Candidate isn't visible. Provokes INVISIBLE_REFERENCE.
     */
    K2_VISIBILITY_ERROR,

    /**
     * Candidate could be successful but receiver (or argument?) nullability doesn't match
     */
    UNSAFE_CALL,

    /**
     * Candidate could be successful but requires unstable smart cast.
     */
    UNSTABLE_SMARTCAST,

    /**
     * Candidate could be successful but does not obey conventions.
     * E.g. infix / operator / etc are missed (= no expected modifier).
     */
    CONVENTION_ERROR,

    // Everything below has isSuccess = true (RESOLVED_WITH_ERROR is an exception)

    /**
     * Candidate is successful but has low priority.
     * Tower resolve proceeds to next levels.
     */
    RESOLVED_LOW_PRIORITY,

    /**
     * Candidate is successful but uses property of functional type as an operator.
     * Tower resolve proceeds to next levels.
     */
    K2_PROPERTY_AS_OPERATOR,

    /**
     * Candidate is successful but uses new features that change resolve.
     * Tower resolve proceeds to next levels.
     */
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,

    // Everything below has shouldStopResolve = true
    // (Tower resolve does not go to further scopes if candidate with applicability below is found)

    /**
     * Successful but synthetic candidate.
     * Used in K2 for (Java) synthetic discrimination at the same level.
     */
    K2_SYNTHETIC_RESOLVED,

    /**
     * Candidate has some error, but it is still successful from resolution perspective.
     * This means that tower resolve stops with this applicability.
     * However, error will be reported.
     * This is the only applicability that stops resolve but provokes an error.
     */
    RESOLVED_WITH_ERROR,

    /**
     * Candidate is successful or has uncompleted inference (so possibly successful).
     */
    RESOLVED,
}

/**
 * This property determines that the considered candidate is "successful" in terms of having no resolve errors.
 * Note that it does not necessarily mean tower resolve should stop on this candidate.
 */
val CandidateApplicability.isSuccess: Boolean
    get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY && this != CandidateApplicability.RESOLVED_WITH_ERROR

/**
 * This property determines that tower resolve should stop on the candidate/group with this applicability
 * and should not go to further scope levels. Note that candidate can still have error(s).
 */
val CandidateApplicability.shouldStopResolve: Boolean
    get() = this >= CandidateApplicability.K2_SYNTHETIC_RESOLVED
