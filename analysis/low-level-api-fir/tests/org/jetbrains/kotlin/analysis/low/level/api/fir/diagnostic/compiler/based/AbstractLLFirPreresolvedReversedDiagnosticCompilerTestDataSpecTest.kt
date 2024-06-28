/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLowLevelCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.utils.bind

abstract class AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataSpecTest : AbstractLowLevelCompilerBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        with(builder) {
            baseFirDiagnosticTestConfiguration(
                frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder),
            )

            baseFirSpecDiagnosticTestConfigurationForIde()
            useAfterAnalysisCheckers(::LLFirOnlyReversedTestSuppressor)
            useMetaTestConfigurators(::ReversedDiagnosticsConfigurator)
            useAfterAnalysisCheckers(::ReversedFirIdenticalChecker)
        }
    }
}
