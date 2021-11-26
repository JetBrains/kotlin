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
import org.jetbrains.kotlin.test.services.sourceProviders.*

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForCodegenAndDebugTest(
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
) {
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

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForCodegenTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
) {
    commonServicesConfigurationForCodegenTest(targetFrontend)
    commonConfigurationForCodegenAndDebugTest(frontendFacade, frontendToBackendConverter, backendFacade)
}

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForDebugTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
) {
    commonServicesConfigurationForDebugTest(targetFrontend)
    commonConfigurationForCodegenAndDebugTest(frontendFacade, frontendToBackendConverter, backendFacade)
}

private fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
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
        ::CodegenHelpersSourceFilesProvider
    )
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenTest(targetFrontend: FrontendKind<*>) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)
    useAdditionalSourceProviders(
        ::MainFunctionForBlackBoxTestsSourceProvider
    )

}

fun TestConfigurationBuilder.commonServicesConfigurationForDebugTest(targetFrontend: FrontendKind<*>) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)
    useAdditionalSourceProviders(
        ::MainFunctionForDebugTestsSourceProvider
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

fun TestConfigurationBuilder.configureCommonHandlersForSteppingTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        steppingHandlersForBackendStep()
    }
}

fun TestConfigurationBuilder.configureCommonHandlersForLocalVariableTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        localVariableHandlersForBackendStep()
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

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.steppingHandlersForBackendStep() {
    useHandlers(::SteppingDebugRunner)
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm>.localVariableHandlersForBackendStep() {
    useHandlers(::LocalVariableDebugRunner)
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
