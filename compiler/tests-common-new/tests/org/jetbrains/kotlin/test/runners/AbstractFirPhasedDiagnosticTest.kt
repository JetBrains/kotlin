/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoFir2IrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.TestTierDirectives.LATEST_EXPECTED_TIER
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.PsiLightTreeMetaInfoProcessor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestTierLabel

abstract class AbstractFirPhasedDiagnosticTest(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_EXPECTED_TIER with TestTierLabel.BACKEND
        }

        commonConfigurationForTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirFrontendFacade,
            frontendToBackendConverter = ::Fir2IrResultsConverter
        )
        configureFirParser(FirParser.LightTree)
        configureCommonDiagnosticTestPaths()

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        configureIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler,
                ::NoFir2IrCompilationErrorsHandler,
            )
        }
        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
        useAfterAnalysisCheckers(::PhasedPipelineChecker)
    }
}

open class AbstractPhasedJvmDiagnosticLightTreeTest : AbstractFirPhasedDiagnosticTest(FirParser.LightTree)
open class AbstractPhasedJvmDiagnosticPsiTest : AbstractFirPhasedDiagnosticTest(FirParser.Psi)
