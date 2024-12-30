/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxInlinerCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.handlers.JvmIrInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureClassicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ENABLE_DEBUG_MODE
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.JavaForeignAnnotationType
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJvmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>

    final override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter, ::MainFunctionForBlackBoxTestsSourceProvider)

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
