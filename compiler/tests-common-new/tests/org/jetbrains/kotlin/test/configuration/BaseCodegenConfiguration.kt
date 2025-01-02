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

/**
 * Setups the pipeline for all JVM backend tests
 *
 * Steps:
 * - FIR frontend
 * - FIR2IR
 * - JVM backend
 *
 * There are handler steps after each facade step.
 */
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

/**
 * Setups the base test configuration for JVM backend tests, including
 * - global defaults
 * - environment configurators
 * - additional source providers
 */
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

/**
 * Setups the bare minimum test configuration for JVM backend tests, including
 * - global defaults
 * - environment configurators
 * - additional source providers
 *
 * This method is used as an implementation detail for [commonServicesConfigurationForCodegenAndDebugTest] and
 * [configureTieredBackendJvmTest], so consider using them instead of this method.
 */
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

/**
 * Adds inline handlers to the test
 */
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

/**
 * Enables IR inliner for JVM backend
 */
fun TestConfigurationBuilder.useIrInliner() {
    defaultDirectives {
        +LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER
    }
}

/**
 * Enables inline scope numbers for debugger-related tests
 */
fun TestConfigurationBuilder.useInlineScopesNumbers() {
    defaultDirectives {
        +LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS
    }
}

/**
 * Adds IR and Bytecode dump handlers to the test
 */
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

/**
 * Add all handlers usually used in codegen tests and the [JvmBoxRunner] handler
 */
fun TestConfigurationBuilder.configureCommonHandlersForBoxTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        useHandlers(::JvmBoxRunner)
    }
}

/**
 * Add all handlers usually used in codegen tests
 */
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

/**
 * Adds a handler which checks that there are no compilation errors reported at the K1 frontend step
 */
fun HandlersStepBuilder<ClassicFrontendOutputArtifact, FrontendKinds.ClassicFrontend>.commonClassicFrontendHandlersForCodegenTest() {
    useHandlers(
        ::NoCompilationErrorsHandler,
    )
}

/**
 * Adds a handler which checks that there are no compilation errors reported at the K2 frontend step
 */
fun HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

/**
 * Add JVM artifact handlers usually used in codegen tests
 */
fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.commonBackendHandlersForCodegenTest() {
    useHandlers(
        ::JvmBackendDiagnosticsHandler,
        ::NoJvmSpecificCompilationErrorsHandler,
        ::DxCheckerHandler,
    )
}

/**
 * Setups the bare minimum test configuration for JVM box tests.
 *
 * This method is used as an implementation detail for [baseFirBlackBoxCodegenTestDirectivesConfiguration] and
 * [configureTieredBackendJvmTest], so consider using them instead of this method.
 */
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

/**
 * Setups additional services and directives used in JVM box tests
 */
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

/**
 * Setups the backend-specific handlers and directives exclusively used by JVM box tests
 */
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

/**
 * Enables specific JVM versions for tests inside `compiler/testData/codegen/boxModernJdk`
 */
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

/**
 * Utility for setting up the target JVM version
 */
fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind, jvmTarget: JvmTarget) {
    defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JDK_KIND with jdkKind
        JvmEnvironmentConfigurationDirectives.JVM_TARGET with jvmTarget
        +WITH_STDLIB
        +CodegenTestDirectives.IGNORE_DEXING
    }
}
