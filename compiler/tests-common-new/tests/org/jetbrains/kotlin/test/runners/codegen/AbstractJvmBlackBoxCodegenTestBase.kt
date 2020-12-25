/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractJvmBlackBoxCodegenTestBase(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<*, *>>
    abstract val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +USE_PSI_CLASS_FILES_READING
            +REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
            +RUN_DEX_CHECKER
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider,
        )

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontend2BackendConverters(frontendToBackendConverter)
        useBackendFacades(backendFacade)

        useFrontendHandlers(::NoCompilationErrorsHandler)
        useArtifactsHandlers(
            ::JvmBoxRunner,
            ::NoJvmSpecificCompilationErrorsHandler,
            ::BytecodeListingHandler,
            ::DxCheckerHandler,
        )

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}
