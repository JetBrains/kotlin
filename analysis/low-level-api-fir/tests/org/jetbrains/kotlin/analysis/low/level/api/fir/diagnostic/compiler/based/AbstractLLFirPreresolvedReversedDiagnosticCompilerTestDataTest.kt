/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractCompilerBasedTestForFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest : AbstractCompilerBasedTestForFir() {
    override fun TestConfigurationBuilder.configureTest() {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder)
        )

        useAfterAnalysisCheckers(::FirReversedSuppressor)
    }
}

internal class FirReversedSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer> get() = listOf(Companion)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $IGNORE_REVERSED_RESOLVE directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = IGNORE_REVERSED_RESOLVE in testServices.moduleStructure.allDirectives

    companion object : SimpleDirectivesContainer() {
        val IGNORE_REVERSED_RESOLVE by directive("Temporary disables reversed resolve checks until the issue is fixed")
    }
}
