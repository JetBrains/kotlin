/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.withSourceCodeAnalysisExceptionUnwrapping

internal abstract class FileStructureElementDiagnosticRetriever {
    abstract fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElementDiagnosticList
}

internal class SingleNonLocalDeclarationDiagnosticRetriever(
    private val structureElementDeclaration: FirDeclaration
) : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElementDiagnosticList {
        val sessionHolder = SessionHolderImpl(moduleComponents.session, moduleComponents.scopeSessionProvider.getScopeSession())
        val context = moduleComponents.globalResolveComponents.lockProvider.withLock(firFile) {
            PersistenceContextCollector.collectContext(sessionHolder, firFile, structureElementDeclaration)
        }
        return withSourceCodeAnalysisExceptionUnwrapping {
            collector.collectForStructureElement(structureElementDeclaration) { components ->
                Visitor(structureElementDeclaration, context, components)
            }
        }
    }

    private class Visitor(
        private val structureElementDeclaration: FirDeclaration,
        context: CheckerContext,
        components: DiagnosticCollectorComponents
    ) : LLFirDiagnosticVisitor(context, components) {
        private var insideAlwaysVisitableDeclarations = 0

        override fun shouldVisitDeclaration(declaration: FirDeclaration): Boolean {
            if (declaration.shouldVisitWithNestedDeclarations()) {
                insideAlwaysVisitableDeclarations++
            }

            if (insideAlwaysVisitableDeclarations > 0) {
                return true
            }

            return when {
                structureElementDeclaration !is FirRegularClass -> true
                structureElementDeclaration == declaration -> true
                declaration.hasAnnotation(StandardClassIds.Annotations.Suppress, context.session) -> {
                    useRegularComponents = false
                    true
                }
                else -> false
            }
        }

        private fun FirDeclaration.shouldVisitWithNestedDeclarations(): Boolean {
            if (shouldDiagnosticsAlwaysBeCheckedOn(this)) return true
            return when (this) {
                is FirAnonymousInitializer -> true
                is FirEnumEntry -> false
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
            KtFakeSourceElementKind.PropertyFromParameter -> true
            KtFakeSourceElementKind.ImplicitConstructor -> true
            else -> false
        }
    }
}

internal object FileDiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
    override fun retrieve(
        firFile: FirFile,
        collector: FileStructureElementDiagnosticsCollector,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElementDiagnosticList =
        withSourceCodeAnalysisExceptionUnwrapping {
            collector.collectForStructureElement(firFile) { components ->
                Visitor(components, moduleComponents)
            }
        }

    private class Visitor(
        components: DiagnosticCollectorComponents,
        moduleComponents: LLFirModuleResolveComponents,
    ) : LLFirDiagnosticVisitor(
        PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(
            SessionHolderImpl(moduleComponents.session, moduleComponents.scopeSessionProvider.getScopeSession())
        ),
        components,
    ) {
        override fun visitFile(file: FirFile, data: Nothing?) {
            withAnnotationContainer(file) {
                visitWithDeclaration(file) {
                    file.annotations.forEach { it.accept(this, data) }
                    file.packageDirective.accept(this, data)
                    file.imports.forEach { it.accept(this, data) }
                    // do not visit declarations here
                }
            }
        }
    }
}
