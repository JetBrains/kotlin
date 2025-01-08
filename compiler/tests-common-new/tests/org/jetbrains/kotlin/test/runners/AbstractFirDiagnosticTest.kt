/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.configuration.configurationForTestWithLatestLanguageVersion
import org.jetbrains.kotlin.test.configuration.configureDiagnosticTest
import org.jetbrains.kotlin.test.configuration.configureIrActualizerDiagnosticsTest
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.PlatformModuleProvider
import org.jetbrains.kotlin.test.services.fir.FirWithoutAliasExpansionTestSuppressor

abstract class AbstractFirDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureDiagnosticTest(parser)
    }
}

abstract class AbstractFirPsiDiagnosticTest : AbstractFirDiagnosticTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeDiagnosticsTest : AbstractFirDiagnosticTestBase(FirParser.LightTree)

abstract class AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest : AbstractFirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForTestWithLatestLanguageVersion()
    }
}

open class AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest : AbstractFirPhasedDiagnosticTest(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DISABLE_TYPEALIAS_EXPANSION
            }

            useAfterAnalysisCheckers(::FirWithoutAliasExpansionTestSuppressor)
        }
    }
}

open class AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest : AbstractKotlinCompilerWithTargetBackendTest(
    targetBackend = TargetBackend.JVM_IR
) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS
        }

        configureFirParser(FirParser.LightTree)
        baseFirDiagnosticTestConfiguration()

        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(::Fir2IrResultsConverter)
        useAdditionalService(::LibraryProvider)

        @OptIn(TestInfrastructureInternals::class)
        useModuleStructureTransformers(PlatformModuleProvider)

        configureIrActualizerDiagnosticsTest()
        configurationForTestWithLatestLanguageVersion()
    }
}
