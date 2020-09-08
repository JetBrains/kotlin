/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

enum class CandidateApplicability {
    RESOLVED, // call success or has uncompleted inference or in other words possible successful candidate
    RESOLVED_WITH_ERROR, // call has error, but it is still successful from resolution perspective
    RESOLVED_NEED_PRESERVE_COMPATIBILITY, // call resolved successfully, but using new features that changes resolve
    RESOLVED_LOW_PRIORITY,
    CONVENTION_ERROR, // missing infix, operator etc
    MAY_THROW_RUNTIME_ERROR, // unsafe call or unstable smart cast
    RUNTIME_ERROR, // problems with visibility
    IMPOSSIBLE_TO_GENERATE, // access to outer class from nested
    INAPPLICABLE, // arguments have wrong types
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, // arguments not mapped to parameters (i.e. different size of arguments and parameters)
    INAPPLICABLE_WRONG_RECEIVER, // receiver not matched
    HIDDEN, // removed from resolve
    RESOLVED_TO_SAM_WITH_VARARG, // migration warning up to 1.5 (when resolve to function with SAM conversion and array without spread as vararg)
}

val CandidateApplicability.isSuccess: Boolean
    get() = this <= CandidateApplicability.RESOLVED_LOW_PRIORITY
