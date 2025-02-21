/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyNonReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.ReversedFirIdenticalChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithoutPreresolve
import org.jetbrains.kotlin.konan.test.diagnostics.baseFirNativeDiagnosticTestConfiguration
import org.jetbrains.kotlin.konan.test.diagnostics.baseNativeDiagnosticTestConfiguration
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.utils.bind

abstract class AbstractLLNativeDiagnosticsTestBase : AbstractLLCompilerBasedTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            globalDefaults {
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
            }

            baseFirNativeDiagnosticTestConfiguration()
            configurationForClassicAndFirTestsAlongside(::ReversedFirIdenticalChecker)
        }
    }
}

abstract class AbstractLLFirNativeTest : AbstractLLNativeDiagnosticsTestBase() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            baseNativeDiagnosticTestConfiguration(::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithoutPreresolve))
            super.configure(builder)
            useAfterAnalysisCheckers(::LLFirOnlyNonReversedTestSuppressor)
        }
    }
}

abstract class AbstractLLFirReversedNativeTest : AbstractLLNativeDiagnosticsTestBase() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            baseNativeDiagnosticTestConfiguration(::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder))
            super.configure(builder)
            useAfterAnalysisCheckers(::LLFirOnlyReversedTestSuppressor)
        }
    }
}
