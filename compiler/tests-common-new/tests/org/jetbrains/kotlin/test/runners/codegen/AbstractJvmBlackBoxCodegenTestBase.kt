/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.USE_JAVAC_BASED_ON_JVM_TARGET
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractJvmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, *>>
    abstract val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForCodegenTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)

        useFrontendHandlers(
            ::ClassicDiagnosticsHandler,
            ::FirDiagnosticsHandler
        )
        commonHandlersForBoxTest()
        useArtifactsHandlers(::BytecodeListingHandler)
        useArtifactsHandlers(::BytecodeTextHandler.bind(true))
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        defaultDirectives {
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        }

        forTestsNotMatching("compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*") {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        forTestsMatching("compiler/testData/codegen/box/testsWithJava9/*") {
            configureModernJavaTest(TestJdkKind.FULL_JDK_9)
        }

        forTestsMatching("compiler/testData/codegen/box/testsWithJava15/*") {
            configureModernJavaTest(TestJdkKind.FULL_JDK_15)
        }

        enableMetaInfoHandler()
    }

    private fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind) {
        defaultDirectives {
            JDK_KIND with jdkKind
            +WITH_STDLIB
            +USE_JAVAC_BASED_ON_JVM_TARGET
            +IGNORE_DEXING
        }
    }
}
