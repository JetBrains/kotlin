/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.CustomOutputDiagnosticsConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.TestByDirectiveSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.LowLevelFirAnalyzerFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.getDeclarationsToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind

abstract class AbstractLLPartialDiagnosticsTest : AbstractLLCompilerBasedTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirPartialBodyAnalysisAnalyzerFacadeFactory),
            testDataConsistencyHandler = ::ReversedFirIdenticalChecker,
        )

        useAfterAnalysisCheckers(::LLFirPartialBodyTestSuppressor)
        useMetaTestConfigurators({ testServices -> CustomOutputDiagnosticsConfigurator(".partialBody.", testServices) })
    }
}

private class LLFirPartialBodyTestSuppressor(
    testServices: TestServices,
) : TestByDirectiveSuppressor(
    suppressDirective = Directives.IGNORE_PARTIAL_BODY_ANALYSIS,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_PARTIAL_BODY_ANALYSIS
                by stringDirective("Temporary disables reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}

private object LLFirPartialBodyAnalysisAnalyzerFacadeFactory : LLFirAnalyzerFacadeFactory() {
    override fun createFirFacade(
        resolutionFacade: LLResolutionFacade,
        allFirFiles: Map<TestFile, FirFile>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ): LowLevelFirAnalyzerFacade {
        return object : LowLevelFirAnalyzerFacade(resolutionFacade, allFirFiles, diagnosticCheckerFilter) {
            override fun runResolution(): List<FirFile> {
                // Analyze files and scripts the last to test partial body resolution
                val declarations = allFirFiles.values.getDeclarationsToResolve()
                    .sortedBy { !it.isPartialBodyResolvable }

                for (declaration in declarations) {
                    declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    declaration.checkPhase(FirResolvePhase.BODY_RESOLVE)
                }

                return allFirFiles.values.toList()
            }
        }
    }
}