/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.AsmLikeInstructionListingHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.CHECK_ASM_LIKE_INSTRUCTIONS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractAsmLikeInstructionListingTestBase(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +CHECK_ASM_LIKE_INSTRUCTIONS
        }

        setupJvmPipelineSteps(parser)

        configureFirHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        commonHandlersForCodegenTest()

        configureJvmArtifactsHandlersStep {
            useHandlers(
                ::AsmLikeInstructionListingHandler
            )
        }

        defaultDirectives {
            DIAGNOSTICS with "-warnings"
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        enableMetaInfoHandler()
    }
}

open class AbstractFirLightTreeAsmLikeInstructionListingTest : AbstractAsmLikeInstructionListingTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiAsmLikeInstructionListingTest : AbstractAsmLikeInstructionListingTestBase(FirParser.Psi)
