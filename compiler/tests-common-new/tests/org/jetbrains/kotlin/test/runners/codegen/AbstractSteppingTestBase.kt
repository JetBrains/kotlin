/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxInlinerCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractSteppingTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade) {
            commonServicesConfigurationForDebugTest(it)
        }

        configureCommonHandlersForSteppingTest()

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor, ::BlackBoxInlinerCodegenSuppressor)

        defaultDirectives {
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            +REQUIRES_SEPARATE_PROCESS
        }

        enableMetaInfoHandler()
    }
}
