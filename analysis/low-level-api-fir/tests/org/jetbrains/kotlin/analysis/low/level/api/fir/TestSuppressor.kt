/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class TestByDirectiveSuppressor(
    val suppressDirective: StringDirective,
    directivesContainer: DirectivesContainer,
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    init {
        require(suppressDirective in directivesContainer)
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(directivesContainer)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $suppressDirective directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = suppressDirective in testServices.moduleStructure.allDirectives
}

class LLFirTestSuppressor(
    testServices: TestServices,
) : TestByDirectiveSuppressor(
    suppressDirective = Directives.MUTE_LL_FIR,
    directivesContainer = Directives,
    testServices
) {

    private object Directives : SimpleDirectivesContainer() {
        val MUTE_LL_FIR by stringDirective("Temporary mute Low Level FIR implementation due to some error. YT ticket must be provided")
    }
}

class LLFirOnlyReversedTestSuppressor(
    testServices: TestServices,
) : TestByDirectiveSuppressor(
    suppressDirective = Directives.IGNORE_REVERSED_RESOLVE,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_REVERSED_RESOLVE by stringDirective("Temporary disables reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}

class LLFirOnlyNonReversedTestSuppressor(
    testServices: TestServices,
) : TestByDirectiveSuppressor(
    suppressDirective = Directives.IGNORE_NON_REVERSED_RESOLVE,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_NON_REVERSED_RESOLVE by stringDirective("Temporary disables non-reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}