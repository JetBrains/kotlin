/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class LLFirCompiledBasedTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer> get() = listOf(Companion)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (MUTE_LL_FIR !in testServices.moduleStructure.allDirectives) return failedAssertions

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $MUTE_LL_FIR directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private companion object : SimpleDirectivesContainer() {
        val MUTE_LL_FIR by stringDirective(
            "Temporary mute Low Level FIR implementation due to some error. YT ticket must be provided"
        )
    }
}
