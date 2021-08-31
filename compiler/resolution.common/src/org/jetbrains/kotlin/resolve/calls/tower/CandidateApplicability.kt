/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

enum class CandidateApplicability {
    RESOLVED_TO_SAM_WITH_VARARG, // migration warning up to 1.5 (when resolve to function with SAM conversion and array without spread as vararg)
    HIDDEN, // removed from resolve
    UNSUPPORTED, // unsupported feature
    INAPPLICABLE_WRONG_RECEIVER, // receiver not matched
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, // arguments not mapped to parameters (i.e. different size of arguments and parameters)
    INAPPLICABLE, // arguments have wrong types
    INAPPLICABLE_MODIFIER, // no expected modifier (eg infix call on non-infix function)
    NO_COMPANION_OBJECT, // Classifier does not have a companion object
    IMPOSSIBLE_TO_GENERATE, // access to outer class from nested
    RUNTIME_ERROR, // problems with visibility
    UNSAFE_CALL, // receiver or argument nullability doesn't match
    UNSTABLE_SMARTCAST, // unstable smart cast
    CONVENTION_ERROR, // missing infix, operator etc
    RESOLVED_LOW_PRIORITY,
    RESOLVED_NEED_PRESERVE_COMPATIBILITY, // call resolved successfully, but using new features that changes resolve
    RESOLVED_WITH_ERROR, // call has error, but it is still successful from resolution perspective
    RESOLVED, // call success or has uncompleted inference or in other words possible successful candidate
}

val CandidateApplicability.isSuccess: Boolean
    get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY
