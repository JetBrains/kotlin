/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor

internal class FileStructureElementDiagnosticsCollector private constructor(private val useExtendedCheckers: Boolean) {
    companion object {
        val USUAL_COLLECTOR = FileStructureElementDiagnosticsCollector(useExtendedCheckers = false)
        val EXTENDED_COLLECTOR = FileStructureElementDiagnosticsCollector(useExtendedCheckers = true)
    }

    fun collectForStructureElement(
        firDeclaration: FirDeclaration,
        initialContext: PersistentCheckerContext?,
        onDeclarationExit: (FirDeclaration) -> Unit = {},
        onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction = {
            DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
        },
    ): FileStructureElementDiagnosticList =
        FirIdeStructureElementDiagnosticsCollector(
            firDeclaration.session,
            initialContext,
            useExtendedCheckers,
            onDeclarationEnter,
            onDeclarationExit
        ).let { collector ->
            collector.collectDiagnostics(firDeclaration)
            FileStructureElementDiagnosticList(collector.result)
        }

    private class FirIdeStructureElementDiagnosticsCollector(
        session: FirSession,
        initialContext: PersistentCheckerContext?,
        useExtendedCheckers: Boolean,
        private val onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction,
        private val onDeclarationExit: (FirDeclaration) -> Unit
    ) : AbstractFirIdeDiagnosticsCollector(
        session,
        useExtendedCheckers,
    ) {
        val result = mutableMapOf<PsiElement, MutableList<FirPsiDiagnostic<*>>>()

        override fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>) {
            result.addValueFor(diagnostic.psiElement, diagnostic)
        }

        override val visitor = object : FirIdeDiagnosticVisitor(
            initialContext ?: PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(this),
            components
        ) {
            override fun getDeclarationActionOnDeclarationEnter(
                declaration: FirDeclaration,
            ): DiagnosticCollectorDeclarationAction =
                onDeclarationEnter.invoke(declaration)

            override fun onDeclarationExit(declaration: FirDeclaration) {
                onDeclarationExit.invoke(declaration)
            }
        }
    }
}