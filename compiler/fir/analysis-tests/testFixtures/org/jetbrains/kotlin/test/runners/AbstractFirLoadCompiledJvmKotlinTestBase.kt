/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirMetadataLoadingTestSuppressor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.JvmLoadedMetadataDumpHandler
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB

open class AbstractFirLoadK2CompiledJvmKotlinTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        setupJvmPipelineSteps(FirParser.LightTree)

        configureFirHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }
        configureJvmArtifactsHandlersStep {
            useHandlers(::JvmLoadedMetadataDumpHandler)
        }

        forTestsMatching("compiler/testData/loadJava/compiledKotlinWithStdlib/*") {
            defaultDirectives {
                +WITH_STDLIB
            }
        }

        useAfterAnalysisCheckers(
            { testServices -> FirMetadataLoadingTestSuppressor(testServices, CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2) }
        )
    }
}
