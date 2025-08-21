/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

enum class ConstraintSystemCompletionMode(
    val allPostponedAtomsShouldBeAnalyzed: Boolean,
    // Actually, it's related to all ConeFunctionTypeRelatedPostponedResolvedAtom including callable references
    val allLambdasShouldBeAnalyzed: Boolean = allPostponedAtomsShouldBeAnalyzed,
    val shouldForkPointConstraintsBeResolved: Boolean,
    val fixNotInferredTypeVariablesToErrorType: Boolean,
) {
    FULL(
        allPostponedAtomsShouldBeAnalyzed = true,
        shouldForkPointConstraintsBeResolved = true,
        fixNotInferredTypeVariablesToErrorType = true,
    ),
    PCLA_POSTPONED_CALL(
        allPostponedAtomsShouldBeAnalyzed = false,
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
        allPostponedAtomsShouldBeAnalyzed = false,
        allLambdasShouldBeAnalyzed = false,
        shouldForkPointConstraintsBeResolved = false,
        fixNotInferredTypeVariablesToErrorType = false,
    ),

    @ExclusiveForOverloadResolutionByLambdaReturnType
    UNTIL_FIRST_LAMBDA(
        allPostponedAtomsShouldBeAnalyzed = false,
        allLambdasShouldBeAnalyzed = false,
        /* See testData/diagnostics/tests/inference/inferenceForkRegressionSimple.kt */
        shouldForkPointConstraintsBeResolved = true,
        // We shouldn't do it here because of input type semi-fixing
        fixNotInferredTypeVariablesToErrorType = false,
    );

    @OptIn(ExclusiveForOverloadResolutionByLambdaReturnType::class)
    fun isUntilFirstLambda(): Boolean = this == UNTIL_FIRST_LAMBDA

    init {
        // allPostponedAtomsShouldBeAnalyzed => allLambdasShouldBeAnalyzed
        assert(!allPostponedAtomsShouldBeAnalyzed || allLambdasShouldBeAnalyzed)
    }

    @RequiresOptIn(
        "This mode should be used only for OverloadResolutionByLambdaReturnTypeResolver. " +
                "Consider using isUntilFirstLambda() if you need just to check this mode."
    )
    annotation class ExclusiveForOverloadResolutionByLambdaReturnType
}
