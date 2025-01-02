/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoLightTreeParsingErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoPsiParsingErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.configuration.configurationForTestWithLatestLanguageVersion
import org.jetbrains.kotlin.test.configuration.configureDiagnosticTest
import org.jetbrains.kotlin.test.configuration.configureIrActualizerDiagnosticsTest
import org.jetbrains.kotlin.test.configuration.configureTieredFrontendJvmTest
import org.jetbrains.kotlin.test.configuration.toTieredHandlersAndCheckerOf
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.PartialTestTierChecker
import org.jetbrains.kotlin.test.services.PlatformModuleProvider
import org.jetbrains.kotlin.test.services.TestTierLabel
import org.jetbrains.kotlin.test.services.fir.FirWithoutAliasExpansionTestSuppressor
import org.jetbrains.kotlin.test.services.fir.LightTreeSyntaxDiagnosticsReporterHolder

abstract class AbstractFirDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureDiagnosticTest(parser)

        forTestsMatching(
            "compiler/testData/diagnostics/tests/*" or
                    "compiler/testData/diagnostics/testsWithStdLib/*" or
                    "compiler/fir/analysis-tests/testData/resolve/*" or
                    "compiler/fir/analysis-tests/testData/resolveWithStdlib/*" or
                    "compiler/fir/analysis-tests/testData/resolveFreezesIDE/*"
        ) {
            useAfterAnalysisCheckers(::PartialTestTierChecker)
        }
    }
}

abstract class AbstractFirPsiDiagnosticTest : AbstractFirDiagnosticTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeDiagnosticsTest : AbstractFirDiagnosticTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useAdditionalService { LightTreeSyntaxDiagnosticsReporterHolder() }
    }
}

abstract class AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest : AbstractFirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForTestWithLatestLanguageVersion()
    }
}

abstract class AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest : AbstractFirLightTreeDiagnosticsTest() {
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

abstract class AbstractTieredFrontendJvmTest(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureTieredFrontendJvmTest(parser)

        val (handlers, checker) = listOfNotNull(
            // Makes the FIR tier fail if there are errors; otherwise, it would fail on meta-infos mismatch.
            // But it's important to continue processing next modules in diagnostic tests, otherwise
            // we won't collect their meta-infos and see a difference.
            { NoFirCompilationErrorsHandler(it, failureDisablesNextSteps = false) },
            // `<SYNTAX>` is reported separately
            when (parser) {
                FirParser.LightTree -> ::NoLightTreeParsingErrorsHandler
                FirParser.Psi -> ::NoPsiParsingErrorsHandler
            },
        ).toTieredHandlersAndCheckerOf(TestTierLabel.FRONTEND)

        configureFirHandlersStep { useHandlers(handlers) }
        useAfterAnalysisCheckers(checker)
    }
}

open class AbstractTieredFrontendJvmLightTreeTest : AbstractTieredFrontendJvmTest(FirParser.LightTree)
open class AbstractTieredFrontendJvmPsiTest : AbstractTieredFrontendJvmTest(FirParser.Psi)

abstract class AbstractFirWithActualizerDiagnosticsTest(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS
        }

        configureFirParser(parser)
        baseFirDiagnosticTestConfiguration()

        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(::Fir2IrResultsConverter)
        useAdditionalService(::LibraryProvider)

        @OptIn(TestInfrastructureInternals::class)
        useModuleStructureTransformers(PlatformModuleProvider)

        configureIrActualizerDiagnosticsTest()
    }
}

open class AbstractFirLightTreeWithActualizerDiagnosticsTest : AbstractFirWithActualizerDiagnosticsTest(FirParser.LightTree)
open class AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest : AbstractFirLightTreeWithActualizerDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForTestWithLatestLanguageVersion()
    }
}
