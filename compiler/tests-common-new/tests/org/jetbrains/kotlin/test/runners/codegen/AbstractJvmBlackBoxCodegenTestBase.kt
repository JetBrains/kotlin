/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.backend.jvm.constEvaluationPhase
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxInlinerCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.handlers.IrInterpreterDumpHandler
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureClassicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.USE_JAVAC_BASED_ON_JVM_TARGET
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ENABLE_DEBUG_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.JavaForeignAnnotationType
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator

abstract class AbstractJvmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)

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

        configureJvmArtifactsHandlersStep {
            useHandlers(
                ::BytecodeListingHandler,
                ::BytecodeTextHandler.bind(true)
            )
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        if (targetBackend.isIR) {
            useAfterAnalysisCheckers(::BlackBoxInlinerCodegenSuppressor)
        }

        defaultDirectives {
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        }

        forTestsNotMatching("compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*") {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        forTestsMatching("compiler/testData/codegen/boxModernJdk/testsWithJava11/*") {
            configureModernJavaTest(TestJdkKind.FULL_JDK_11, JvmTarget.JVM_11)
        }

        forTestsMatching("compiler/testData/codegen/boxModernJdk/testsWithJava17/*") {
            configureModernJavaTest(TestJdkKind.FULL_JDK_17, JvmTarget.JVM_17)
        }

        forTestsMatching("compiler/testData/codegen/box/coroutines/varSpilling/debugMode/*") {
            defaultDirectives {
                +ENABLE_DEBUG_MODE
            }
        }

        forTestsMatching("compiler/testData/codegen/box/javaInterop/foreignAnnotationsTests/tests/*") {
            defaultDirectives {
                ForeignAnnotationsDirectives.ANNOTATIONS_PATH with JavaForeignAnnotationType.Annotations
            }
            useConfigurators(::JvmForeignAnnotationsConfigurator)
        }

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/dumpIrAndCheck/*") {
            defaultDirectives {
                CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES with constEvaluationPhase
            }
            configureJvmArtifactsHandlersStep {
                useHandlers(::IrInterpreterDumpHandler)
            }
        }

        enableMetaInfoHandler()
    }

    private fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind, jvmTarget: JvmTarget) {
        defaultDirectives {
            JDK_KIND with jdkKind
            JVM_TARGET with jvmTarget
            +WITH_STDLIB
            +USE_JAVAC_BASED_ON_JVM_TARGET
            +IGNORE_DEXING
        }
    }
}
