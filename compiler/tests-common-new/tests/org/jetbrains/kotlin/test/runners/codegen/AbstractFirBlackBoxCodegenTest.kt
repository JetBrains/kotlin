/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.CodegenWithIrFakeOverrideGeneratorSuppressor
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.ENABLE_IR_FAKE_OVERRIDE_GENERATION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.*

abstract class AbstractFirBlackBoxCodegenTestBase(
    val parser: FirParser
) : AbstractJvmBlackBoxCodegenTestBase<FirOutputArtifact, IrBackendInput>(
    FrontendKinds.FIR,
    TargetBackend.JVM_IR
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureFirParser(parser)
            defaultDirectives {
                // See KT-44152
                -USE_PSI_CLASS_FILES_READING
            }

            forTestsMatching("*WithStdLib/*") {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }

            configureFirHandlersStep {
                useHandlersAtFirst(
                    ::FirDumpHandler,
                    ::FirScopeDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirResolvedTypesVerifier,
                )
            }

            configureIrHandlersStep {
                useHandlers(::IrDiagnosticsHandler)
            }

            useAfterAnalysisCheckers(
                ::FirMetaInfoDiffSuppressor
            )

            configureDumpHandlersForCodegenTest()

            forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
                defaultDirectives {
                    LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
                }
            }
        }
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxCodegenTest : AbstractFirBlackBoxCodegenTestBase(FirParser.Psi)

open class AbstractFirLightTreeBlackBoxCodegenWithIrFakeOverrideGeneratorTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ENABLE_IR_FAKE_OVERRIDE_GENERATION
            }

            useAfterAnalysisCheckers(::CodegenWithIrFakeOverrideGeneratorSuppressor)
        }
    }
}
