/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyNonReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithoutPreresolve
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFailureSuppressorBySingleDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind

/**
 * Checks diagnostics in the test data with regular resolution order.
 * Example:
 * ```kotlin
 * fun one() = 0
 * fun two() = true
 * ```
 * The function `one` will be resolved first.
 * The function `two` will be resolved after that.
 *
 * A counterpart for [AbstractLLReversedDiagnosticsTest].
 *
 * @see AbstractLLReversedDiagnosticsTest
 */
abstract class AbstractLLDiagnosticsTest : AbstractLLCompilerBasedTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            baseFirDiagnosticTestConfiguration(
                frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithoutPreresolve),
                testDataConsistencyHandler = ::ReversedFirIdenticalChecker,
            )

            useFailureSuppressors(::ContractViolationSuppressor)
            useFailureSuppressors(::LLFirOnlyNonReversedTestSuppressor)
        }
    }
}

private class ContractViolationSuppressor(testServices: TestServices) : TestFailureSuppressorBySingleDirective(
    suppressDirective = IGNORE_CONTRACT_VIOLATIONS,
    directivesContainer = Companion,
    testServices
) {
    companion object : SimpleDirectivesContainer() {
        val IGNORE_CONTRACT_VIOLATIONS by directive("Temporary disables test with contract violation until the issue is fixed")
    }
}
