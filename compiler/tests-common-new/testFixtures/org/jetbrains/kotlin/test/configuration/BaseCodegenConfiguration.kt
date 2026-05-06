/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.codegen.forTestCompile.JavaForeignAnnotationType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.TestStepBuilder
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ENABLE_DEBUG_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirSpecificParserSuppressor
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

/**
 * Sets up the pipeline for all JVM backend tests
 *
 * Steps:
 * - FIR frontend
 * - FIR2IR
 * - JVM backend
 *
 * There are handler steps after each facade step.
 */
fun TestConfigurationBuilder.setupJvmPipelineSteps(parser: FirParser) {
    commonServicesConfigurationForCodegenAndDebugTest()
    configureFirParser(parser)

    facadeStep(::FirCliJvmFacade)
    firHandlersStep()
    facadeStep(::Fir2IrCliJvmFacade)
    irHandlersStep(init = {})
    facadeStep(::BackendCliJvmFacade)
    jvmArtifactsHandlersStep(init = {})
}

/**
 * Sets up the base test configuration for JVM backend tests, including
 * - global defaults
 * - environment configurators
 * - additional source providers
 */
private fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest() {
    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +RUN_DEX_CHECKER
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmForeignAnnotationsConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
    )

    useMetaTestConfigurators(::FirSpecificParserSuppressor)
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
    configureFirHandlersStep {
        commonFirHandlersForCodegenTest()
    }
    configureJvmArtifactsHandlersStep {
        commonBackendHandlersForCodegenTest()
    }
}

/**
 * Adds a handler which checks that there are no compilation errors reported at the K2 frontend step
 */
fun TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<FirOutputArtifact, FrontendKinds.FIR>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

/**
 * Add JVM artifact handlers usually used in codegen tests
 */
fun TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.commonBackendHandlersForCodegenTest(includeNoCompilationErrorsHandler: Boolean = true) {
    useHandlers(
        ::JvmBackendDiagnosticsHandler,
        ::DxCheckerHandler,
    )
    if (includeNoCompilationErrorsHandler) {
        useHandlers(::NoJvmSpecificCompilationErrorsHandler)
    }
}

/**
 * Setups the bare minimum test configuration for JVM box tests
 */
fun TestConfigurationBuilder.configureBlackBoxTestSettings() {
    defaultDirectives {
        // See KT-44152
        -USE_PSI_CLASS_FILES_READING
    }

    useFailureSuppressors(
        ::FirMetaInfoDiffSuppressor
    )

    baseFirBlackBoxCodegenTestDirectivesConfiguration()
}

/**
 * Setups additional services and directives used in JVM box tests
 */
fun TestConfigurationBuilder.baseFirBlackBoxCodegenTestDirectivesConfiguration() {
    commonCodegenConfiguration()

    forTestsMatching("*WithStdLib/*") {
        defaultDirectives {
            +WITH_STDLIB
        }
    }
}

/**
 * Setups the backend-specific handlers and directives exclusively used by JVM box tests
 */
fun TestConfigurationBuilder.configureJvmBoxCodegenSettings(includeAllDumpHandlers: Boolean, includeBytecodeTextHandler: Boolean = true) {
    configureJvmArtifactsHandlersStep {
        if (includeAllDumpHandlers) {
            useHandlers(::BytecodeListingHandler,)
        }
        if (includeBytecodeTextHandler) {
            useHandlers(::BytecodeTextHandler.bind(true))
        }
    }

    defaultDirectives {
        +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        +WITH_STDLIB
    }

    forTestsNotMatching(
        "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                "compiler/testData/codegen/boxJvm/diagnostics/functions/tailRecursion/*" or
                "compiler/testData/diagnostics/*" or
                "compiler/fir/analysis-tests/testData/*"
    ) {
        defaultDirectives {
            DIAGNOSTICS with "-warnings"
        }
    }

    configureModernJavaWhenNeeded()

    forTestsMatching("compiler/testData/codegen/box(?:Jvm)?/coroutines/varSpilling/debugMode/*") {
        defaultDirectives {
            +ENABLE_DEBUG_MODE
        }
    }

    forTestsMatching("compiler/testData/codegen/box(?:Jvm)?/javaInterop/foreignAnnotationsTests/tests/*") {
        defaultDirectives {
            +ENABLE_FOREIGN_ANNOTATIONS
            ForeignAnnotationsDirectives.ANNOTATIONS_PATH with JavaForeignAnnotationType.Annotations
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
