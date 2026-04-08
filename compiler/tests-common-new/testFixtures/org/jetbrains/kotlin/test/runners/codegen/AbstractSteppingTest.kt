/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.SteppingDebugRunner
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.configureDumpHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForDebugTestsSourceProvider

abstract class AbstractSteppingTestBase(
    val parser: FirParser
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJvmTest(
            FrontendKinds.FIR,
            ::FirCliJvmFacade,
            ::Fir2IrCliJvmFacade,
            ::BackendCliJvmFacade,
            additionalSourceProvider = ::MainFunctionForDebugTestsSourceProvider
        )

        configureFirParser(parser)
        commonHandlersForCodegenTest()
        configureDumpHandlersForCodegenTest()
        configureJvmArtifactsHandlersStep {
            useHandlers(::SteppingDebugRunner)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        defaultDirectives {
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            +REQUIRES_SEPARATE_PROCESS
        }

        enableMetaInfoHandler()
    }
}

open class AbstractFirLightTreeSteppingTest : AbstractSteppingTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiSteppingTest : AbstractSteppingTestBase(FirParser.Psi)
