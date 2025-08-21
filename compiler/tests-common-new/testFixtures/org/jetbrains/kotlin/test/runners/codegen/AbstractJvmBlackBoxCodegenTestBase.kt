/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureClassicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.configureCommonHandlersForBoxTest
import org.jetbrains.kotlin.test.configuration.configureJvmBoxCodegenSettings
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

abstract class AbstractJvmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJvmTest(
            targetFrontend,
            frontendFacade,
            frontendToBackendConverter,
            backendFacade,
            additionalSourceProvider = ::MainFunctionForBlackBoxTestsSourceProvider
        )

        configureClassicFrontendHandlersStep {
            useHandlers(
                ::ClassicDiagnosticsHandler
            )
        }

        configureFirHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        configureCommonHandlersForBoxTest()

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
        )

        configureJvmBoxCodegenSettings(includeAllDumpHandlers = true)
        enableMetaInfoHandler()
    }
}
