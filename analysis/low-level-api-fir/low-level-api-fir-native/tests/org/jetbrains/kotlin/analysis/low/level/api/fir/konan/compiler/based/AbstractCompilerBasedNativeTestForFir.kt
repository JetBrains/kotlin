/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyNonReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirOnlyReversedTestSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLowLevelCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.ReversedFirIdenticalChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactoryWithoutPreresolve
import org.jetbrains.kotlin.konan.test.diagnostics.baseFirNativeDiagnosticTestConfiguration
import org.jetbrains.kotlin.konan.test.diagnostics.baseNativeDiagnosticTestConfiguration
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.utils.bind

abstract class AbstractLLFirNativeTestBase : AbstractLowLevelCompilerBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        with(builder) {
            globalDefaults {
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
            }

            baseFirNativeDiagnosticTestConfiguration()
            configurationForClassicAndFirTestsAlongside(::ReversedFirIdenticalChecker)
        }
    }
}

abstract class AbstractLLFirNativeTest : AbstractLLFirNativeTestBase() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        with(builder) {
            baseNativeDiagnosticTestConfiguration(::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithoutPreresolve))
            super.configureTest(builder)
            useAfterAnalysisCheckers(::LLFirOnlyNonReversedTestSuppressor)
        }
    }
}

abstract class AbstractLLFirReversedNativeTest : AbstractLLFirNativeTestBase() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        with(builder) {
            baseNativeDiagnosticTestConfiguration(::LowLevelFirFrontendFacade.bind(LLFirAnalyzerFacadeFactoryWithPreresolveInReversedOrder))
            super.configureTest(builder)
            useAfterAnalysisCheckers(::LLFirOnlyReversedTestSuppressor)
        }
    }
}