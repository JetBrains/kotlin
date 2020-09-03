/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureElementDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement

internal class FirIdeStructureElementDiagnosticsCollector private constructor(
    session: FirSession,
    private val onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction,
    private val onDeclarationExit: (FirDeclaration) -> Unit
) : AbstractFirIdeDiagnosticsCollector(
    session,
) {
    private val result = mutableMapOf<KtElement, MutableList<Diagnostic>>()

    override fun onDiagnostic(diagnostic: Diagnostic) {
        (diagnostic.psiElement as? KtElement)?.let { ktElement ->
            result.addValueFor(ktElement, diagnostic)
        }
    }

    override fun onDeclarationEnter(
        declaration: FirDeclaration,
    ): DiagnosticCollectorDeclarationAction =
        onDeclarationEnter.invoke(declaration)

    override fun onDeclarationExit(declaration: FirDeclaration) {
        onDeclarationExit.invoke(declaration)
    }


    companion object {
        fun collectForStructureElement(
            firFile: FirFile,
            onDeclarationExit: (FirDeclaration) -> Unit = {},
            onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction,
        ): FileStructureElementDiagnostics =
            FirIdeStructureElementDiagnosticsCollector(firFile.session, onDeclarationEnter, onDeclarationExit).let { collector ->
                collector.collectDiagnostics(firFile)
                FileStructureElementDiagnostics(collector.result)
            }
    }
}
