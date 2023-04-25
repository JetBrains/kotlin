/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.IrBuiltInsOverFir
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile

open class LowLevelFirAnalyzerFacade(
    val firResolveSession: LLFirResolveSession,
    val allFirFiles: Map<TestFile, FirFile>,
    private val diagnosticCheckerFilter: DiagnosticCheckerFilter,
) : AbstractFirAnalyzerFacade() {
    override val scopeSession: ScopeSession
        get() = ScopeSession()

    private var resolved: Boolean = false

    override fun runCheckers(): Map<FirFile, List<KtDiagnostic>> {
        if (!resolved) {
            runResolution()
            resolved = true
        }

        return allFirFiles.values.associateWith { firFile ->
            val ktFile = firFile.psi as KtFile
            val diagnostics = ktFile.collectDiagnosticsForFile(firResolveSession, diagnosticCheckerFilter)
            @Suppress("UNCHECKED_CAST")
            diagnostics.toList() as List<KtDiagnostic>
        }
    }

    override fun runResolution(): List<FirFile> = allFirFiles.values.toList()

    override fun convertToIr(
        fir2IrExtensions: Fir2IrExtensions,
        commonMemberStorage: Fir2IrCommonMemberStorage,
        irBuiltIns: IrBuiltInsOverFir?
    ): Fir2IrResult = shouldNotBeCalled()
}

private fun shouldNotBeCalled(): Nothing = error("Should not be called for LL test")
