/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.visitScriptDependentElements
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
sealed class FileStructureElementDiagnosticRetriever(
    val declaration: FirDeclaration,
    val file: FirFile,
    val moduleComponents: LLFirModuleResolveComponents,
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
    fun forceBodyResolve() {
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

class ClassDiagnosticRetriever(
    declaration: FirDeclaration,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(
    declaration,
    file,
    moduleComponents,
) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(declaration, context, components)
    }

    class Visitor(
        val structureElementDeclaration: FirDeclaration,
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirDiagnosticVisitor(context, components) {
        override fun shouldVisitDeclaration(declaration: FirDeclaration): Boolean = when {
            declaration === structureElementDeclaration -> true
            insideFakeDeclaration -> true
            declaration.isImplicitConstructor -> true
            declaration is FirValueParameter && declaration.valueParameterKind != FirValueParameterKind.Regular -> true
            else -> false
        }

        var insideFakeDeclaration: Boolean = false

        override fun visitNestedElements(element: FirElement) {
            if (element.isImplicitConstructor) {
                insideFakeDeclaration = true
                try {
                    super.visitNestedElements(element)
                } finally {
                    insideFakeDeclaration = false
                }
            } else {
                super.visitNestedElements(element)
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

val FirElement.isImplicitConstructor: Boolean
    get() = this is FirConstructor && source?.kind == KtFakeSourceElementKind.ImplicitConstructor

class SingleNonLocalDeclarationDiagnosticRetriever(
    declaration: FirDeclaration,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(
    declaration,
    file,
    moduleComponents,
) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(context, components)
    }

    class Visitor(
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

class FileDiagnosticRetriever(
    declaration: FirDeclaration,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(
    declaration,
    file,
    moduleComponents,
) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(context, components)
    }

    class Visitor(
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirDiagnosticVisitor(context, components) {
        override fun visitFile(file: FirFile, data: Nothing?) {
            withAnnotationContainer(file) {
                visitWithFile(file) {
                    file.annotations.forEach { it.accept(this, data) }
                    file.packageDirective.accept(this, data)
                    file.imports.forEach { it.accept(this, data) }
                    // do not visit declarations here
                }
            }
        }
    }
}

class ScriptDiagnosticRetriever(
    declaration: FirDeclaration,
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElementDiagnosticRetriever(
    declaration,
    file,
    moduleComponents,
) {
    override fun createVisitor(context: CheckerContextForProvider, components: DiagnosticCollectorComponents): LLFirDiagnosticVisitor {
        return Visitor(context, components)
    }

    class Visitor(
        context: CheckerContextForProvider,
        components: DiagnosticCollectorComponents,
    ) : LLFirDiagnosticVisitor(context, components) {
        override fun visitScript(script: FirScript, data: Nothing?) {
            withAnnotationContainer(script) {
                checkElement(script)
                withDeclaration(script) {
                    visitScriptDependentElements(script, this, data)
                }
            }
        }
    }
}
