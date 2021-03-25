/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistentCheckerContextFactory

internal abstract class FileStructureElementDiagnosticRetriever {
    abstract fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList
}

internal class SingleNonLocalDeclarationDiagnosticRetriever(
    private val declaration: FirDeclaration
) : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList {
        val sessionHolder = SessionHolderImpl.createWithEmptyScopeSession(firFile.session)
        val context = PersistenceContextCollector.collectContext(sessionHolder, firFile, declaration)
        return collector.collectForStructureElement(declaration) { components ->
            FirIdeDiagnosticVisitor(context, components)
        }
    }
}

internal object FileDiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList =
        collector.collectForStructureElement(firFile) { components ->
            Visitor(firFile, components)
        }

    private class Visitor(
        firFile: FirFile,
        components: List<AbstractDiagnosticCollectorComponent>
    ) : FirIdeDiagnosticVisitor(
        PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(SessionHolderImpl.createWithEmptyScopeSession(firFile.session)),
        components
    ) {
        override fun goToNestedDeclarations(element: FirElement) {
            val goNested = when (element) {
                is FirFile -> true
                is FirDeclaration -> false
                else -> true
            }
            if (goNested) {
                super.goToNestedDeclarations(element)
            }
        }
    }
}