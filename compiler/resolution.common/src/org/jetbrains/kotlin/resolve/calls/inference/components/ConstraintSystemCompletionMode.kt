/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

enum class ConstraintSystemCompletionMode(
    val allLambdasShouldBeAnalyzed: Boolean,
    val shouldForkPointConstraintsBeResolved: Boolean,
    val fixNotInferredTypeVariablesToErrorType: Boolean,
) {
    FULL(
        allLambdasShouldBeAnalyzed = true,
        shouldForkPointConstraintsBeResolved = true,
        fixNotInferredTypeVariablesToErrorType = true,
    ),
    PCLA_POSTPONED_CALL(
        allLambdasShouldBeAnalyzed = true,
        shouldForkPointConstraintsBeResolved = false,
        fixNotInferredTypeVariablesToErrorType = false,
    ),

    /**
     * This mode allows us to infer variables in calls, which have enough type-info to be completed right-away
     * It can also trigger analysis of some postponed arguments
     * We can't treat it as a plain optimization, because it affects the overload resolution in some cases
     * e.g:
     * ```kotlin
     * val x: Int = 1
     * x.plus(run { x }) // Here, to select plus overload we need to analyze lambda
     * ```
     */
    PARTIAL(
        allLambdasShouldBeAnalyzed = false,
        shouldForkPointConstraintsBeResolved = false,
        fixNotInferredTypeVariablesToErrorType = false,
    ),
    UNTIL_FIRST_LAMBDA(
        allLambdasShouldBeAnalyzed = false,
        /* See testData/diagnostics/tests/inference/inferenceForkRegressionSimple.kt */
        shouldForkPointConstraintsBeResolved = true,
        // This one is quite questionable, but should not matter too much
        // because anyway overload ambiguity would be reported for error candidates
        fixNotInferredTypeVariablesToErrorType = true,
    ),
}
