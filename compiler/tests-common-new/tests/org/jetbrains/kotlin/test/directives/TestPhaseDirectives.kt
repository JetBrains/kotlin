/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestPhaseLabel

object TestPhaseDirectives : SimpleDirectivesContainer() {
    /**
     * See: [TestPhaseLabel] for the list of values.
     * If not set, the test will be generated into [FRONTEND][TestPhaseLabel.FRONTEND]
     * phases, but the infrastructure will still complain about the missing directive.
     */
    val RUN_PIPELINE_TILL by enumDirective<TestPhaseLabel>(
        description = "Verify that the test runs successfully until the specified phase (including). See `TestPhaseLabel` for the list of phases"
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
    val LATEST_PHASE_IN_PIPELINE by enumDirective<TestPhaseLabel>(
        description = "The last phase of the pipeline in the test"
    )

    val DISABLE_NEXT_PHASE_SUGGESTION by stringDirective(
        description = "Prevents the phased test runners from complaining that the current phase could be upgraded. Use this directive if the test contains something obviously wrong, like disabling of some language features or cringe error suppression, causing failures on higher phase"
    )
}
