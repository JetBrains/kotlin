/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import org.jetbrains.kotlin.fir.analysis.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirSealedClassInheritorsProcessor
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.resolvedFirToPhase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestFile

class LowLevelFirAnalyzerFacade(
    val resolveState: FirModuleResolveState,
    val allFirFiles: Map<TestFile, FirFile>,
    private val diagnosticCheckerFilter: DiagnosticCheckerFilter,
) : AbstractFirAnalyzerFacade() {
    override val scopeSession: ScopeSession get() = shouldNotBeCalled()

    override fun runCheckers(): Map<FirFile, List<FirDiagnostic>> {
        findSealedInheritors()
        return allFirFiles.values.associateWith { firFile ->
            val ktFile = firFile.psi as KtFile
            val diagnostics = ktFile.collectDiagnosticsForFile(resolveState, diagnosticCheckerFilter)
            @Suppress("UNCHECKED_CAST")
            diagnostics.toList() as List<FirDiagnostic>
        }
    }

    private fun findSealedInheritors() {
        allFirFiles.values.forEach {
            it.resolvedFirToPhase(FirResolvePhase.SUPER_TYPES, resolveState)
        }
        val sealedProcessor = FirSealedClassInheritorsProcessor(allFirFiles.values.first().moduleData.session, ScopeSession())
        sealedProcessor.process(allFirFiles.values)
    }

    override fun runResolution(): List<FirFile> = shouldNotBeCalled()
    override fun convertToIr(extensions: GeneratorExtensions): Fir2IrResult = shouldNotBeCalled()
}

private fun shouldNotBeCalled(): Nothing = error("Should not be called for LL test")
