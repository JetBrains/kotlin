/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.*

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    backendFacade: Constructor<BackendFacade<B, BinaryArtifacts.Jvm>>,
    commonServicesConfiguration: (FrontendKind<*>) -> Unit = { commonServicesConfigurationForCodegenTest(it) }
) {
    commonServicesConfiguration(targetFrontend)
    facadeStep(frontendFacade)
    classicFrontendHandlersStep()
    firHandlersStep()
    facadeStep(frontendToBackendConverter)
    irHandlersStep(init = {})
    facadeStep(backendFacade)
    jvmArtifactsHandlersStep(init = {})
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
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

fun TestConfigurationBuilder.useInlineHandlers() {
    configureJvmArtifactsHandlersStep {
        inlineHandlers()
    }

    applyDumpSmapDirective()
}

fun TestConfigurationBuilder.useIrInliner() {
    defaultDirectives {
        +LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER
    }
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

fun <InputArtifactKind> HandlersStepBuilder<IrBackendInput, InputArtifactKind>.dumpHandlersForConverterStep()
        where InputArtifactKind : BackendKind<IrBackendInput> {
    useHandlers(
        ::IrTreeVerifierHandler,
        ::IrTextDumpHandler,
        ::IrMangledNameAndSignatureDumpHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.dumpHandlersForBackendStep() {
    useHandlers(::BytecodeListingHandler)
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.boxHandlersForBackendStep() {
    useHandlers(
        ::JvmBoxRunner
    )
}

fun HandlersStepBuilder<ClassicFrontendOutputArtifact, FrontendKinds.ClassicFrontend>.commonClassicFrontendHandlersForCodegenTest() {
    useHandlers(
        ::NoCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.commonBackendHandlersForCodegenTest() {
    useHandlers(
        ::JvmBackendDiagnosticsHandler,
        ::NoJvmSpecificCompilationErrorsHandler,
        ::DxCheckerHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.inlineHandlers() {
    useHandlers(
        ::BytecodeInliningHandler,
        ::SMAPDumpHandler
    )
}

fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind, jvmTarget: JvmTarget) {
    defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JDK_KIND with jdkKind
        JvmEnvironmentConfigurationDirectives.JVM_TARGET with jvmTarget
        +ConfigurationDirectives.WITH_STDLIB
        +CodegenTestDirectives.USE_JAVAC_BASED_ON_JVM_TARGET
        +CodegenTestDirectives.IGNORE_DEXING
    }
}
