/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrConstCheckerHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.*
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.jvm.JdkKindBoxTestChecker
import org.jetbrains.kotlin.test.services.jvm.PureJvmCodegenBoxTestChecker
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

abstract class AbstractJvmBlackBoxCodegenTestBase(
    val parser: FirParser,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        commonConfigurationForJvmTest(
            FrontendKinds.FIR,
            ::FirCliJvmFacade,
            ::Fir2IrCliJvmFacade,
            ::BackendCliJvmFacade,
            additionalSourceProvider = ::MainFunctionForBlackBoxTestsSourceProvider
        )

        configureFirParser(parser)

        configureFirHandlersStep {
            useHandlersAtFirst(
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirResolvedTypesVerifier,
                ::FirScopeDumpHandler,
                ::FirDiagnosticsHandler
            )
        }

        configureIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler,
                ::IrConstCheckerHandler
            )
        }

        forTestsMatching("compiler/testData/codegen/*") {
            useAfterAnalysisCheckers(::PureJvmCodegenBoxTestChecker)
            useAfterAnalysisCheckers(::JdkKindBoxTestChecker)
        }

        configureCommonHandlersForBoxTest()
        configureDumpHandlersForCodegenTest()
        configureBlackBoxTestSettings()

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
        )

        configureJvmBoxCodegenSettings(includeAllDumpHandlers = true)
        enableMetaInfoHandler()
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTest : AbstractJvmBlackBoxCodegenTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxCodegenTest : AbstractJvmBlackBoxCodegenTestBase(FirParser.Psi)
