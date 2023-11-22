/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractCompilerBasedTestForFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithoutPreresolve
import org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.bind

abstract class AbstractDiagnosticCompilerTestDataTest : AbstractCompilerBasedTestForFir() {
    override fun TestConfigurationBuilder.configureTest() {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithoutPreresolve),
            testDataConsistencyHandler = ::ReversedFirIdenticalChecker,
        )
        useAfterAnalysisCheckers(::ContractViolationSuppressor)
        useAfterAnalysisCheckers(::DiagnosticSuppressor)
    }
}

private class ContractViolationSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer> get() = listOf(Companion)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        val filteredExceptions = failedAssertions.filterNot { it.cause is FirLazyResolveContractViolationException }
        return if (filteredExceptions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $IGNORE_CONTRACT_VIOLATIONS directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = IGNORE_CONTRACT_VIOLATIONS in testServices.moduleStructure.allDirectives

    companion object : SimpleDirectivesContainer() {
        val IGNORE_CONTRACT_VIOLATIONS by directive("Temporary disables test with contract violation until the issue is fixed")
    }
}

private class DiagnosticSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer> get() = listOf(Companion)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isDisabled()) {
            return failedAssertions
        }

        return if (failedAssertions.isEmpty()) {
            listOf(
                AssertionError(
                    "Test contains $IGNORE_DIAGNOSTIC_API directive but no errors was reported. Please remove directive",
                ).wrap()
            )
        } else {
            emptyList()
        }
    }

    private fun isDisabled(): Boolean = IGNORE_DIAGNOSTIC_API in testServices.moduleStructure.allDirectives

    companion object : SimpleDirectivesContainer() {
        val IGNORE_DIAGNOSTIC_API by directive("Temporary disables diagnostic api test until the issue is fixed")
    }
}
