/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestTierLabel

object TestTierDirectives : SimpleDirectivesContainer() {
    /**
     * See: [TestTierLabel] for the list of values.
     * If not set, the test will be generated into [FRONTEND][TestTierLabel.FRONTEND]
     * tiers, but the infrastructure will still complain about the missing directive.
     */
    val RUN_PIPELINE_TILL by enumDirective<TestTierLabel>(
        description = "Verify that the test runs successfully until the specified tier (including). See `TestTiers` for the list of tiers"
    )

    /**
     * [LATEST_PHASE_IN_PIPELINE] determines at which maximum phase the test pipeline theoretically could execute.
     * [RUN_PIPELINE_TILL] determines at which phase test might fail at leaf module, so it won't count as the test failure.
     *
     * Example:
     * // LATEST_PHASE_IN_PIPELINE: BACKEND
     * // RUN_PIPELINE_TILL: FIR2IR
     *
     * This combination means that the pipeline for all modules in the test will be executed until BACKEND phase, but
     * it's expected the leaf module will fail at FIR2IR stage.
     *
     * Important: [LATEST_PHASE_IN_PIPELINE] should be defined once per test runner and shouldn't be additionally set in any
     * particular test.
     */
    val LATEST_PHASE_IN_PIPELINE by enumDirective<TestTierLabel>(
        description = "The last phase of the pipeline in the test"
    )

    val DISABLE_NEXT_TIER_SUGGESTION by stringDirective(
        description = "Prevents the tiered test runners from complaining that the current tier could be upgraded. Use this directive if the test contains something obviously wrong, like disabling of some language features or cringe error suppression, causing failures on higher tiers"
    )
}
