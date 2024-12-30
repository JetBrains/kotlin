/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxInlinerCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.LocalVariableDebugRunner
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForDebugTestsSourceProvider

abstract class AbstractLocalVariableTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>

    final override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter, ::MainFunctionForDebugTestsSourceProvider)

        commonHandlersForCodegenTest()
        configureJvmArtifactsHandlersStep {
            useHandlers(::LocalVariableDebugRunner)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor, ::BlackBoxInlinerCodegenSuppressor)

        defaultDirectives {
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            +REQUIRES_SEPARATE_PROCESS
        }

        enableMetaInfoHandler()
    }
}
