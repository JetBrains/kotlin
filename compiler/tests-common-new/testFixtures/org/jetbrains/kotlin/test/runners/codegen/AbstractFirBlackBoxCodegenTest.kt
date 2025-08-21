/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrConstCheckerHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.configureBlackBoxTestSettings
import org.jetbrains.kotlin.test.configuration.configureDumpHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.*

abstract class AbstractFirBlackBoxCodegenTestBase(
    val parser: FirParser
) : AbstractJvmBlackBoxCodegenTestBase<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJvmFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJvmFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::BackendCliJvmFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureFirParser(parser)

            configureFirHandlersStep {
                useHandlersAtFirst(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirResolvedTypesVerifier,
                )

                useHandlersAtFirst(
                    ::FirScopeDumpHandler,
                )
            }

            configureIrHandlersStep {
                useHandlers(
                    ::IrDiagnosticsHandler,
                    ::IrConstCheckerHandler
                )
            }

            configureBlackBoxTestSettings()
            configureDumpHandlersForCodegenTest()
        }
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.Psi)
