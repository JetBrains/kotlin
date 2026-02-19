/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.declarationsToIgnore
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.util.withSourceCodeAnalysisExceptionUnwrapping

/**
 * Collects [FileStructureElementDiagnosticList] for specific [declaration].
 *
 * @see FileStructureElementDiagnostics
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement
 */
internal sealed class FileStructureElementDiagnosticRetriever(
    val declaration: FirDeclaration,
    private val file: FirFile,
    private val moduleComponents: LLFirModuleResolveComponents,
) {
    fun retrieve(filter: DiagnosticCheckerFilter): FileStructureElementDiagnosticList {
        forceBodyResolve()

        val sessionHolder = SessionHolderImpl(moduleComponents.session, moduleComponents.scopeSessionProvider.getScopeSession())
        val context = if (declaration is FirFile) {
            PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
        } else {
            PersistenceContextCollector.collectContext(sessionHolder, file, declaration)
        }

        return withSourceCodeAnalysisExceptionUnwrapping {
            collectForStructureElement(declaration, filter) { components ->
                createVisitor(context, components)
            }
        }
    }

    abstract fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor

    /**
     * Declarations-containers may analyze its members, so we have to resole them explicitly as
     * not all of them are pre-resolved during [declaration] resolution.
     * For instance, functions and classes are not a part of the container body resolution.
     */
    private fun forceBodyResolve() {
        ProgressManager.checkCanceled()

        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val declarationContainer = when (declaration) {
            is FirFile -> declaration.declarations.singleOrNull() as? FirScript ?: declaration
            is FirScript, is FirRegularClass -> declaration
            else -> return
        }

        declarationContainer.forEachDeclaration {
            it.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        }
    }
}

/**
 * The visitor is supposed to check the container itself and all declarations that belong to its structure element.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementContainerRecorder
 */
private abstract class LLFirContainerDiagnosticVisitor(
    private val declarationsToIgnore: Set<FirDeclaration>,
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : LLFirDiagnosticVisitor(context, components) {
    override fun shouldVisitDeclaration(declaration: FirDeclaration): Boolean {
        return declaration !in declarationsToIgnore
    }
}

internal class ClassDiagnosticRetriever(
    declaration: FirRegularClass,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(declaration as FirRegularClass, context, components)
    }

    private class Visitor(
        regularClass: FirRegularClass,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirContainerDiagnosticVisitor(
        declarationsToIgnore = regularClass.declarationsToIgnore,
        context = context,
        components = components,
    )

    companion object {
        fun shouldDiagnosticsAlwaysBeCheckedOn(firElement: FirElement) = when (firElement.source?.kind) {
            KtFakeSourceElementKind.PropertyFromParameter -> true
            KtFakeSourceElementKind.ImplicitConstructor -> true
            else -> false
        }
    }
}

internal class SingleNonLocalDeclarationDiagnosticRetriever(
    declaration: FirDeclaration,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(context, components)
    }

    private class Visitor(
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirDiagnosticVisitor(context, components) {
        override fun visitConstructor(constructor: FirConstructor, data: Nothing?) {
            super.visitConstructor(constructor, data)

            if (constructor is FirPrimaryConstructor) {
                for (valueParameter in constructor.valueParameters) {
                    valueParameter.correspondingProperty?.let {
                        visitProperty(it, data)
                    }
                }
            }
        }
    }
}

internal class FileDiagnosticRetriever(
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(file, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(declaration as FirFile, context, components)
    }

    private class Visitor(
        file: FirFile,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirContainerDiagnosticVisitor(
        declarationsToIgnore = file.declarationsToIgnore,
        context = context,
        components = components,
    )
}

internal class ScriptDiagnosticRetriever(
    declaration: FirScript,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(declaration, file, moduleComponents) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(declaration as FirScript, context, components)
    }

    private class Visitor(
        script: FirScript,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirContainerDiagnosticVisitor(
        declarationsToIgnore = script.declarationsToIgnore,
        context = context,
        components = components,
    )
}
