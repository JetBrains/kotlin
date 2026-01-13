/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor.SuppressionChecker
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonBackendHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.NonSourceErrorMessagesHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.PsiLightTreeMetaInfoProcessor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJavaUsingAstTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
            LANGUAGE + "+EnableDfaWarningsInK2"
        }

        commonConfigurationForJvmTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirCliJvmFacade,
            frontendToBackendConverter = ::Fir2IrCliJvmFacade,
            backendFacade = ::BackendCliJvmFacade
        )
        configureFirParser(FirParser.LightTree)
        configureCommonDiagnosticTestPaths()

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::NoFirCompilationErrorsHandler, ::TagsGeneratorChecker)
        }

        configureIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler,
                ::NoIrCompilationErrorsHandler,
            )
        }

        configureJvmArtifactsHandlersStep {
            commonBackendHandlersForCodegenTest(includeNoCompilationErrorsHandler = false)
        }

        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
        useAfterAnalysisCheckers(::PhasedPipelineChecker, ::NonSourceErrorMessagesHandler)
        enableMetaInfoHandler()
        useAdditionalService<BlackBoxCodegenSuppressor.SuppressionChecker>(::SuppressionChecker.bind(null, null))
    }
}
