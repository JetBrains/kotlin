/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile

class LowLevelFirAnalyzerFacade(
    val firResolveSession: LLFirResolveSession,
    val allFirFiles: Map<TestFile, FirFile>,
    private val diagnosticCheckerFilter: DiagnosticCheckerFilter,
) : AbstractFirAnalyzerFacade() {
    override val scopeSession: ScopeSession
        get() = ScopeSession()

    override fun runCheckers(): Map<FirFile, List<KtDiagnostic>> {
        return allFirFiles.values.associateWith { firFile ->
            val ktFile = firFile.psi as KtFile
            val diagnostics = ktFile.collectDiagnosticsForFile(firResolveSession, diagnosticCheckerFilter)
            @Suppress("UNCHECKED_CAST")
            diagnostics.toList() as List<KtDiagnostic>
        }
    }

    override fun runResolution(): List<FirFile> = shouldNotBeCalled()
    override fun convertToIr(
        fir2IrExtensions: Fir2IrExtensions,
        dependentComponents: List<Fir2IrComponents>,
        symbolTable: SymbolTable?
    ): Fir2IrResult = shouldNotBeCalled()
}

private fun shouldNotBeCalled(): Nothing = error("Should not be called for LL test")
