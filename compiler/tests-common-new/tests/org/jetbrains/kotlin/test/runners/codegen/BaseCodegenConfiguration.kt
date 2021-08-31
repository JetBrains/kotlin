/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForCodegenTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
) {
    commonServicesConfigurationForCodegenTest(targetFrontend)
    facadeStep(frontendFacade)
    classicFrontendHandlersStep()
    firHandlersStep()
    commonBackendStepsConfiguration(
        frontendToBackendConverter,
        irHandlersInit = {},
        backendFacade,
        jvmHandlersInit = {}
    )
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenTest(targetFrontend: FrontendKind<*>) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +RUN_DEX_CHECKER
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
        ::CodegenHelpersSourceFilesProvider,
        ::MainFunctionForBlackBoxTestsSourceProvider,
    )
}

inline fun <B : ResultingArtifact.BackendInput<B>, F : ResultingArtifact.FrontendOutput<F>> TestConfigurationBuilder.commonBackendStepsConfiguration(
    noinline frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    irHandlersInit: HandlersStepBuilder<IrBackendInput>.() -> Unit,
    noinline backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
    jvmHandlersInit: HandlersStepBuilder<BinaryArtifacts.Jvm>.() -> Unit,
) {
    facadeStep(frontendToBackendConverter)
    irHandlersStep(irHandlersInit)
    facadeStep(backendFacade)
    jvmArtifactsHandlersStep(jvmHandlersInit)
}

fun TestConfigurationBuilder.useInlineHandlers() {
    configureJvmArtifactsHandlersStep {
        inlineHandlers()
    }

    applyDumpSmapDirective()
}

fun TestConfigurationBuilder.applyDumpSmapDirective() {
    forTestsMatching("compiler/testData/codegen/boxInline/smap/*") {
        defaultDirectives {
            +DUMP_SMAP
        }
    }
}

fun TestConfigurationBuilder.configureDumpHandlersForCodegenTest() {
    configureIrHandlersStep {
        dumpHandlersForConverterStep()
    }
    configureJvmArtifactsHandlersStep {
        dumpHandlersForBackendStep()
    }
}

fun TestConfigurationBuilder.configureCommonHandlersForBoxTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        boxHandlersForBackendStep()
    }
}

fun TestConfigurationBuilder.commonHandlersForCodegenTest() {
    configureClassicFrontendHandlersStep {
        commonClassicFrontendHandlersForCodegenTest()
    }

    configureFirHandlersStep {
        commonFirHandlersForCodegenTest()
    }
    configureJvmArtifactsHandlersStep {
        commonBackendHandlersForCodegenTest()
    }
}

fun HandlersStepBuilder<IrBackendInput>.dumpHandlersForConverterStep() {
    useHandlers(::IrTreeVerifierHandler, ::IrTextDumpHandler)
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.dumpHandlersForBackendStep() {
    useHandlers(::BytecodeListingHandler)
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.boxHandlersForBackendStep() {
    useHandlers(::JvmBoxRunner)
}

fun HandlersStepBuilder<ClassicFrontendOutputArtifact>.commonClassicFrontendHandlersForCodegenTest() {
    useHandlers(
        ::NoCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<FirOutputArtifact>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.commonBackendHandlersForCodegenTest() {
    useHandlers(
        ::NoJvmSpecificCompilationErrorsHandler,
        ::DxCheckerHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.inlineHandlers() {
    useHandlers(
        ::BytecodeInliningHandler,
        ::SMAPDumpHandler
    )
}
