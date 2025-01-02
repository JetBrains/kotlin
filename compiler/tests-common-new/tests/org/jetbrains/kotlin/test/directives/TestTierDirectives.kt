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
     * Essentially, determines the tiered test runner this test must be generated into by specifying the corresponding tier.
     * Overrides/takes precedence over [RUN_PIPELINE_TILL] when both are present.
     *
     * Exists for __debugging__ purposes only.
     */
    val TARGET_RUNNER_TIER by stringDirective(
        description = "Exists for debugging tiered runners. Instructs the test generator to dump the test into the specified tier runners (but $RUN_PIPELINE_TILL is still used for the actual checks)"
    )

    val DISABLE_NEXT_TIER_SUGGESTION by stringDirective(
        description = "Prevents the tiered test runners from complaining that the current tier could be upgraded. Use this directive if the test contains something obviously wrong, like disabling of some language features or cringe error suppression, causing failures on higher tiers"
    )
}
