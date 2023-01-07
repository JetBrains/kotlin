/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Deprecated
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DeprecatedSinceKotlin
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRecord
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.WasExperimental

class FirCompilerRequiredAnnotationsResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {

    override fun process(files: Collection<FirFile>) {
        val computationSession = CompilerRequiredAnnotationsComputationSession()
        val transformer = FirCompilerRequiredAnnotationsResolveTransformer(session, scopeSession, computationSession)
        files.forEach {
            withFileAnalysisExceptionWrapping(it) {
                it.transformSingle(transformer, null)
            }
        }
    }

    @OptIn(FirSymbolProviderInternals::class)
    override fun beforePhase() {
        super.beforePhase()
        session.generatedDeclarationsSymbolProvider?.disable()
    }

    @OptIn(FirSymbolProviderInternals::class)
    override fun afterPhase() {
        super.afterPhase()
        session.generatedDeclarationsSymbolProvider?.enable()
    }
}

abstract class AbstractFirCompilerRequiredAnnotationsResolveTransformer(
    final override val session: FirSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : FirAbstractPhaseTransformer<Nothing?>(COMPILER_REQUIRED_ANNOTATIONS) {
    internal abstract val annotationTransformer: AbstractFirSpecificAnnotationResolveTransformer
    private val importTransformer = FirPartialImportResolveTransformer(session, computationSession)

    val extensionService = session.extensionService
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        checkSessionConsistency(file)
        file.resolveAnnotations()
        return file
    }

    fun <T> withFileAndScopes(file: FirFile, f: () -> T): T {
        annotationTransformer.withFile(file) {
            return annotationTransformer.withFileScopes(file, f)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        return annotationTransformer.transformRegularClass(regularClass, null)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirStatement {
        return annotationTransformer.transformTypeAlias(typeAlias, null)
    }

    private fun FirFile.resolveAnnotations() {
        this.transformImports(importTransformer, null)
        this.transform<FirFile, Nothing?>(annotationTransformer, null)
    }
}

open class FirCompilerRequiredAnnotationsResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : AbstractFirCompilerRequiredAnnotationsResolveTransformer(session, computationSession) {
    override val annotationTransformer: AbstractFirSpecificAnnotationResolveTransformer =
        FirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession)
}

class FirDesignatedCompilerRequiredAnnotationsResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession,
    designation: DesignationState
) : AbstractFirCompilerRequiredAnnotationsResolveTransformer(session, computationSession) {
    override val annotationTransformer: AbstractFirSpecificAnnotationResolveTransformer =
        FirDesignatedSpecificAnnotationResolveTransformer(session, scopeSession, computationSession, designation)
}

class CompilerRequiredAnnotationsComputationSession {
    private val filesWithResolvedImports = mutableSetOf<FirFile>()

    fun importsAreResolved(file: FirFile): Boolean {
        return file in filesWithResolvedImports
    }

    fun recordThatImportsAreResolved(file: FirFile) {
        if (!filesWithResolvedImports.add(file)) {
            error("Imports are resolved twice")
        }
    }

    private val declarationsWithResolvedAnnotations = mutableSetOf<FirDeclaration>()

    fun annotationsAreResolved(declaration: FirDeclaration): Boolean {
        if (declaration is FirFile) return false
        if (declaration.origin != FirDeclarationOrigin.Source) return true
        return declaration in declarationsWithResolvedAnnotations
    }

    fun recordThatAnnotationsAreResolved(declaration: FirDeclaration) {
        if (!declarationsWithResolvedAnnotations.add(declaration)) {
            error("Annotations are resolved twice")
        }
    }
}

private class FirPartialImportResolveTransformer(
    session: FirSession,
    private val computationSession: CompilerRequiredAnnotationsComputationSession
) : FirImportResolveTransformer(session, COMPILER_REQUIRED_ANNOTATIONS) {
    private val acceptableFqNames: Set<FqName> = session.registeredPluginAnnotations.annotations

    override val FqName.isAcceptable: Boolean
        get() = this in acceptableFqNames

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        if (computationSession.importsAreResolved(file)) return file
        return super.transformFile(file, data).also {
            computationSession.recordThatImportsAreResolved(file)
        }
    }
}

private class FirSpecificAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : AbstractFirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession) {
    override fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean {
        return !computationSession.annotationsAreResolved(declaration)
    }
}

private class FirDesignatedSpecificAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession,
    private val designation: DesignationState
) : AbstractFirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession) {
    override fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean {
        return !designation.shouldSkipClass(declaration)
    }
}
