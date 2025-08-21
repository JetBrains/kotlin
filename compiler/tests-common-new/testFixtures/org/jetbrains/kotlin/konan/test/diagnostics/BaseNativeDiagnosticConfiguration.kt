/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.diagnostics

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

fun <R : ResultingArtifact.FrontendOutput<R>> TestConfigurationBuilder.baseNativeDiagnosticTestConfiguration(
    frontendFacade: Constructor<FrontendFacade<R>>,
) {
    defaultDirectives {
        +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        +ConfigurationDirectives.WITH_STDLIB
    }

    enableMetaInfoHandler()

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::NativeEnvironmentConfigurator,
    )

    useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
    )

    facadeStep(frontendFacade)

    forTestsMatching("testData/diagnostics/nativeTests/*") {
        defaultDirectives {
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }
    }
}

fun TestConfigurationBuilder.baseFirNativeDiagnosticTestConfiguration() {
    firHandlersStep {
        useHandlers(
            ::FirDiagnosticsHandler,
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
            ::FirScopeDumpHandler,
        )
    }
}
