/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.LockProvider

internal abstract class FileStructureElementDiagnosticRetriever {
    abstract fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        lockProvider: LockProvider<FirFile>
    ): FileStructureElementDiagnosticList
}

internal class SingleNonLocalDeclarationDiagnosticRetriever(
    private val structureElementDeclaration: FirDeclaration
) : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        lockProvider: LockProvider<FirFile>
    ): FileStructureElementDiagnosticList {
        val sessionHolder = SessionHolderImpl.createWithEmptyScopeSession(firFile.session)
        val context = lockProvider.withReadLock(firFile) {
            PersistenceContextCollector.collectContext(sessionHolder, firFile, structureElementDeclaration)
        }
        return collector.collectForStructureElement(structureElementDeclaration) { components ->
            Visitor(structureElementDeclaration, context, components)
        }
    }

    private class Visitor(
        private val structureElementDeclaration: FirDeclaration,
        context: PersistentCheckerContext,
        components: List<AbstractDiagnosticCollectorComponent>
    ) : FirIdeDiagnosticVisitor(context, components) {
        private var insideAlwaysVisitableDeclarations = 0

        override fun shouldVisitDeclaration(declaration: FirDeclaration): Boolean {
            if (declaration.shouldVisitWithNestedDeclarations()) {
                insideAlwaysVisitableDeclarations++
            }

            if (insideAlwaysVisitableDeclarations > 0) {
                return true
            }

            @Suppress("IntroduceWhenSubject")
            return when {
                structureElementDeclaration !is FirRegularClass -> true
                structureElementDeclaration == declaration -> true
                else -> false
            }
        }

        private fun FirDeclaration.shouldVisitWithNestedDeclarations(): Boolean {
            if (shouldDiagnosticsAlwaysBeCheckedOn(this)) return true
            return when (this) {
                is FirAnonymousInitializer -> true
                is FirEnumEntry -> true
                is FirValueParameter -> true
                is FirConstructor -> isPrimary
                else -> false
            }
        }

        override fun onDeclarationExit(declaration: FirDeclaration) {
            if (declaration.shouldVisitWithNestedDeclarations()) {
                insideAlwaysVisitableDeclarations--
            }
        }
    }

    companion object {
        fun shouldDiagnosticsAlwaysBeCheckedOn(firElement: FirElement) = when (firElement.source?.kind) {
            FirFakeSourceElementKind.PropertyFromParameter -> true
            FirFakeSourceElementKind.ImplicitConstructor -> true
            else -> false
        }
    }
}

internal object FileDiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        lockProvider: LockProvider<FirFile>
    ): FileStructureElementDiagnosticList =
        collector.collectForStructureElement(firFile) { components ->
            Visitor(firFile, components)
        }

    private class Visitor(
        firFile: FirFile,
        components: List<AbstractDiagnosticCollectorComponent>
    ) : FirIdeDiagnosticVisitor(
        PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(SessionHolderImpl.createWithEmptyScopeSession(firFile.session)),
        components,
    ) {
        override fun visitFile(file: FirFile, data: Nothing?) {
            withSuppressedDiagnostics(file) {
                visitWithDeclaration(file) {
                    file.annotations.forEach { it.accept(this, data) }
                    file.imports.forEach { it.accept(this, data) }
                    // do not visit declarations here
                }
            }
        }
    }
}