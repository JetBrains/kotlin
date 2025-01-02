/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoFir2IrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.configuration.configureTieredFir2IrJvmTest
import org.jetbrains.kotlin.test.configuration.toTieredHandlersAndCheckerOf
import org.jetbrains.kotlin.test.services.TestTierLabel

abstract class AbstractFirJvmIrTextTest(
    protected val parser: FirParser,
) : AbstractJvmIrTextTest<FirOutputArtifact>() {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonConfigurationForK2(parser)
    }
}

open class AbstractFirLightTreeJvmIrTextTest : AbstractFirJvmIrTextTest(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiJvmIrTextTest : AbstractFirJvmIrTextTest(FirParser.Psi)


abstract class AbstractTieredFir2IrJvmTest(
    private val parser: FirParser,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    /**
     * Mimics [AbstractIrTextTest.klibFacades][org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest.klibFacades]
     */
    open val klibFacades: KlibFacades?
        get() = null

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // See: compiler/testData/diagnostics/tests/multiplatform/actualAnnotationsNotMatchExpect/checkDiagnosticFullText.kt
        // It expects `+MultiPlatformProjects` to be present a priori because of its location.
        // Also, it's important to configure the same handlers, otherwise differences with the `.fir.kt` files
        // (the absence of diagnostics) would be considered as FIR tier failure.

        configureTieredFir2IrJvmTest(parser, targetBackend, ::Fir2IrResultsConverter, klibFacades)

        val (handlers, checker) = listOf(::NoFir2IrCompilationErrorsHandler).toTieredHandlersAndCheckerOf(TestTierLabel.FIR2IR)
        configureIrHandlersStep { useHandlers(handlers) }
        useAfterAnalysisCheckers(checker)
    }
}

open class AbstractTieredFir2IrJvmLightTreeTest : AbstractTieredFir2IrJvmTest(FirParser.LightTree)
open class AbstractTieredFir2IrJvmPsiTest : AbstractTieredFir2IrJvmTest(FirParser.Psi)
