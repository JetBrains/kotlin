/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrNoExpectSymbolsHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.configureCommonHandlersForBoxTest
import org.jetbrains.kotlin.test.configuration.configureDumpHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.configureJvmBoxCodegenSettings
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HMPP
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataSerializerFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.configuration.MetadataEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase(
    val parser: FirParser
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            FIR_PARSER with parser
            +SEPARATE_KMP_COMPILATION
            +DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
            +WITH_STDLIB
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::MetadataEnvironmentConfiguratorForSeparateKmpCompilation,
            ::JvmEnvironmentConfiguratorForSeparateKmpCompilation,
        )

        useAdditionalSourceProviders(
            ::MainFunctionForBlackBoxTestsSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(::FirCliMetadataFrontendFacade)
        facadeStep(::FirCliJvmFacade)

        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        facadeStep(::FirCliMetadataSerializerFacade)
        facadeStep(::Fir2IrCliJvmFacade)

        irHandlersStep {
            useHandlers(::IrNoExpectSymbolsHandler)
        }

        facadeStep(::BackendCliJvmFacade)

        jvmArtifactsHandlersStep()

        configureCommonHandlersForBoxTest(includeK1Handlers = false)

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor.bind(IGNORE_HMPP, null),
        )

        configureJvmBoxCodegenSettings(includeAllDumpHandlers = true)
        configureDumpHandlersForCodegenTest()
        enableMetaInfoHandler()
    }
}

open class AbstractJvmLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest : AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase(FirParser.LightTree)
