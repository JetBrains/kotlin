/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxInlinerCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ENABLE_DEBUG_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.configuration.*
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    additionalSourceProvider: Constructor<AdditionalSourceProvider>? = null,
) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)
    additionalSourceProvider?.let { useAdditionalSourceProviders(it) }
    facadeStep(frontendFacade)
    classicFrontendHandlersStep()
    firHandlersStep()
    facadeStep(frontendToBackendConverter)
    irHandlersStep(init = {})
    facadeStep(::JvmIrBackendFacade)
    jvmArtifactsHandlersStep(init = {})
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
    )

    commonServicesMinimalSettingsConfigurationForCodegenAndDebugTest(targetFrontend)
}

fun TestConfigurationBuilder.commonServicesMinimalSettingsConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +RUN_DEX_CHECKER
    }

    useConfigurators(
        ::JvmForeignAnnotationsConfigurator,
    )

    useAdditionalSourceProviders(
        ::CodegenHelpersSourceFilesProvider,
    )
}

fun TestConfigurationBuilder.useInlineHandlers() {
    configureJvmArtifactsHandlersStep {
        useHandlers(
            ::BytecodeInliningHandler,
            ::SMAPDumpHandler
        )
    }

    forTestsMatching("compiler/testData/codegen/boxInline/smap/*") {
        defaultDirectives {
            +DUMP_SMAP
        }
    }
}

fun TestConfigurationBuilder.useIrInliner() {
    defaultDirectives {
        +LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER
    }
}

fun TestConfigurationBuilder.useInlineScopesNumbers() {
    defaultDirectives {
        +LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS
    }
}

fun TestConfigurationBuilder.configureDumpHandlersForCodegenTest(includeAllDumpHandlers: Boolean = true) {
    configureIrHandlersStep {
        useHandlers(
            ::IrTreeVerifierHandler,
            ::IrTextDumpHandler,
            ::IrMangledNameAndSignatureDumpHandler,
        )
    }
    configureJvmArtifactsHandlersStep {
        if (includeAllDumpHandlers) {
            useHandlers(::BytecodeListingHandler)
        }
    }
}

fun TestConfigurationBuilder.configureCommonHandlersForBoxTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        useHandlers(::JvmBoxRunner)
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

fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind, jvmTarget: JvmTarget) {
    defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JDK_KIND with jdkKind
        JvmEnvironmentConfigurationDirectives.JVM_TARGET with jvmTarget
        +WITH_STDLIB
        +CodegenTestDirectives.IGNORE_DEXING
    }
}

fun TestConfigurationBuilder.configureBlackBoxTestSettings() {
    defaultDirectives {
        // See KT-44152
        -USE_PSI_CLASS_FILES_READING
    }

    useAfterAnalysisCheckers(
        ::FirMetaInfoDiffSuppressor
    )

    baseFirBlackBoxCodegenTestDirectivesConfiguration()
}

fun TestConfigurationBuilder.baseFirBlackBoxCodegenTestDirectivesConfiguration() {
    forTestsMatching("*WithStdLib/*") {
        defaultDirectives {
            +WITH_STDLIB
        }
    }

    forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
        }
    }
}

fun TestConfigurationBuilder.configureJvmBoxCodegenSettings(includeAllDumpHandlers: Boolean) {
    configureJvmArtifactsHandlersStep {
        if (includeAllDumpHandlers) {
            useHandlers(
                ::BytecodeListingHandler,
            )
        }

        useHandlers(
            ::BytecodeTextHandler.bind(true)
        )
    }

    useAfterAnalysisCheckers(
        ::BlackBoxInlinerCodegenSuppressor,
    )

    defaultDirectives {
        +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
    }

    forTestsNotMatching(
        "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                "compiler/testData/diagnostics/*" or
                "compiler/fir/analysis-tests/testData/*"
    ) {
        defaultDirectives {
            DIAGNOSTICS with "-warnings"
        }
    }

    configureModernJavaWhenNeeded()

    forTestsMatching("compiler/testData/codegen/box/coroutines/varSpilling/debugMode/*") {
        defaultDirectives {
            +ENABLE_DEBUG_MODE
        }
    }

    forTestsMatching("compiler/testData/codegen/box/javaInterop/foreignAnnotationsTests/tests/*") {
        defaultDirectives {
            +ENABLE_FOREIGN_ANNOTATIONS
            ForeignAnnotationsDirectives.ANNOTATIONS_PATH with JavaForeignAnnotationType.Annotations
        }
    }

    forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
        configureJvmArtifactsHandlersStep {
            useHandlers(::JvmIrInterpreterDumpHandler)
        }
    }
}

fun TestConfigurationBuilder.configureModernJavaWhenNeeded() {
    forTestsMatching("compiler/testData/codegen/boxModernJdk/testsWithJava11/*") {
        configureModernJavaTest(TestJdkKind.FULL_JDK_11, JvmTarget.JVM_11)
    }

    forTestsMatching("compiler/testData/codegen/boxModernJdk/testsWithJava17/*") {
        configureModernJavaTest(TestJdkKind.FULL_JDK_17, JvmTarget.JVM_17)
    }

    forTestsMatching("compiler/testData/codegen/boxModernJdk/testsWithJava21/*") {
        configureModernJavaTest(TestJdkKind.FULL_JDK_21, JvmTarget.JVM_21)
    }
}
