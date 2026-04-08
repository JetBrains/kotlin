/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractBytecodeListingTestBase(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +CodegenTestDirectives.CHECK_BYTECODE_LISTING
        }

        commonConfigurationForJvmTest(
            FrontendKinds.FIR,
            ::FirCliJvmFacade,
            ::Fir2IrCliJvmFacade,
            ::BackendCliJvmFacade,
        )

        configureFirParser(parser)
        commonHandlersForCodegenTest()

        configureIrHandlersStep {
            useHandlers(::IrTextDumpHandler)
        }

        configureJvmArtifactsHandlersStep {
            useHandlers(::BytecodeListingHandler)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

open class AbstractFirLightTreeBytecodeListingTest : AbstractBytecodeListingTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBytecodeListingTest : AbstractBytecodeListingTestBase(FirParser.Psi)
