/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.fir.util.plusAssign
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.DiagnosticWithKmpCompilationMode
import org.jetbrains.kotlin.test.frontend.fir.handlers.DiagnosticsMap
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.KmpCompilationMode
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.runIf

open class LowLevelFirAnalyzerFacade(
    val resolutionFacade: LLResolutionFacade,
    val allFirFiles: Map<TestFile, FirFile>,
    private val diagnosticCheckerFilter: DiagnosticCheckerFilter,
) : AbstractFirAnalyzerFacade() {
    override val scopeSession: ScopeSession
        get() = ScopeSession()

    override val result: FirResult
        get() {
            val output = ModuleCompilerAnalyzedOutput(resolutionFacade.useSiteFirSession, scopeSession, allFirFiles.values.toList())
            return FirResult(listOf(output))
        }

    private var resolved: Boolean = false

    fun runCheckers(): Map<FirFile, List<KtDiagnostic>> {
        if (!resolved) {
            runResolution()
            resolved = true
        }

        return allFirFiles.values.associateWith { firFile ->
            val ktFile = firFile.psi as KtFile
            val diagnostics = ktFile.collectDiagnosticsForFile(resolutionFacade, diagnosticCheckerFilter)
            @Suppress("UNCHECKED_CAST")
            diagnostics.toList() as List<KtDiagnostic>
        }
    }

    override fun runResolution(): List<FirFile> = allFirFiles.values.toList()
}

class AnalysisApiFirDiagnosticCollectorService(testServices: TestServices) : FirDiagnosticCollectorService(testServices) {
    override fun getFrontendDiagnosticsForModule(info: FirOutputArtifact): DiagnosticsMap {
        val result = listMultimapOf<FirFile, DiagnosticWithKmpCompilationMode>()
        for (part in info.partsForDependsOnModules) {
            val facade = part.firAnalyzerFacade
            require(facade is LowLevelFirAnalyzerFacade)
            result += facade.runCheckers().mapValues { entry ->
                entry.value.mapNotNull {
                    runIf(it.isValid) {
                        DiagnosticWithKmpCompilationMode(it, KmpCompilationMode.LOW_LEVEL_API)
                    }
                }
            }
            collectSyntaxDiagnostics(part, result)
        }
        return result
    }
}
