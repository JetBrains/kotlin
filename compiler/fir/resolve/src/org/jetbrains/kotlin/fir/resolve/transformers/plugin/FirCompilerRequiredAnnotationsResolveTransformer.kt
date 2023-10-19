/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.resolve.transformers.DesignationState
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.FqName


/**
 * Little explanation the logic of this phase
 *
 * - this transformer visits all declarations and tries to resolve each annotation that looks like compiler-required by short name
 *   - if there are any meta-annotations registered by plugins, then each annotation is considered as "potentially compiler-required"
 * - transformer resolves:
 *   - annotation types
 *   - annotation call arguments, if annotation classId mentioned in FirAnnotationsPlatformSpecificSupportComponent.requiredAnnotationsWithArguments
 * - if annotation is considered as compiler-required after the resolution, the resolved type is saved in the annotation with
 *     FirAnnotationResolvePhase.CompilerRequiredAnnotations phase
 * - if annotation potentially can be annotated with meta-annotation, the phase performs a designated jump to corresponding annotation class
 *     to resolve annotations on it. This jump resolves annotations only on last declaration from designation path
 *
 * Example:
 * ```
 * // FILE: plugin.kt
 * annotation class MetaAnnotation
 *
 * // FILE: a.kt
 * @A // (1)
 * class Some
 *
 * // FILE: b.kt
 * @A.B // (2)
 * class Other
 *
 * // FILE: SinceKotlin.kt
 * @MetaAnnotation // (3)
 * annotation class A {
 *     @MetaAnnotation // (4)
 *     annotation class B
 *
 *     @MetaAnnotation // (5)
 *     annotation class C
 * }
 * ```
 *
 * 1. visit class `Some`, resolve (1)
 * 2. there are meta-annotations -> jump to class `A` and resolve (3)
 * 3. visit class `Other`, resolved (2)
 * 4. there are meta-annotations -> jump to class `A.B` and resolve (4)
 * 5. visit class `A`, skip annotations on `A` since they are already resolved
 * 6. visit class `A.B`, skip annotations on `A.B` since they are already resolved
 * 7. visit class `A.C`, resolve (5)
 */
class FirCompilerRequiredAnnotationsResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession, COMPILER_REQUIRED_ANNOTATIONS) {

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

        val generatedDeclarationsSymbolProvider = session.generatedDeclarationsSymbolProvider
        generatedDeclarationsSymbolProvider?.enable()

        // This part is a bit hacky way to clear the caches in FirCachingCompositeSymbolProvider when there are plugins that may generate new entities.
        // It's necessary because otherwise, when symbol provider is being queried on the stage of compiler-required annotations resolution
        // we record incorrect (incomplete) results to its cache, so after the phase is completed we just start from the scratch
        val symbolProvider = session.symbolProvider
        if (generatedDeclarationsSymbolProvider != null && symbolProvider is FirCachingCompositeSymbolProvider) {
            @OptIn(SessionConfiguration::class)
            session.register(FirSymbolProvider::class, symbolProvider.createCopyWithCleanCaches())
        }
    }
}

abstract class AbstractFirCompilerRequiredAnnotationsResolveTransformer(
    final override val session: FirSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : FirAbstractPhaseTransformer<Nothing?>(COMPILER_REQUIRED_ANNOTATIONS) {
    abstract val annotationTransformer: AbstractFirSpecificAnnotationResolveTransformer
    private val importTransformer = FirPartialImportResolveTransformer(session, computationSession)

    val extensionService = session.extensionService
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        withFileAnalysisExceptionWrapping(file) {
            checkSessionConsistency(file)
            file.resolveAnnotations()
        }
        return file
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

open class CompilerRequiredAnnotationsComputationSession {
    private val filesWithResolvedImports = mutableSetOf<FirFile>()

    fun importsAreResolved(file: FirFile): Boolean {
        return file in filesWithResolvedImports
    }

    open val useCacheForImportScope: Boolean get() = false

    fun recordThatImportsAreResolved(file: FirFile) {
        if (!filesWithResolvedImports.add(file)) {
            error("Imports are resolved twice")
        }
    }

    private val declarationsWithResolvedAnnotations = mutableSetOf<FirAnnotationContainer>()

    fun annotationsAreResolved(declaration: FirAnnotationContainer, treatNonSourceDeclarationsAsResolved: Boolean): Boolean {
        if (declaration is FirFile) return false
        if (treatNonSourceDeclarationsAsResolved && declaration is FirDeclaration && declaration.origin != FirDeclarationOrigin.Source) {
            return true
        }

        return declaration in declarationsWithResolvedAnnotations
    }

    fun recordThatAnnotationsAreResolved(declaration: FirAnnotationContainer) {
        declarationsWithResolvedAnnotations.add(declaration)
    }

    fun resolveAnnotationsOnAnnotationIfNeeded(symbol: FirRegularClassSymbol, scopeSession: ScopeSession) {
        val regularClass = symbol.fir
        if (annotationsAreResolved(regularClass, treatNonSourceDeclarationsAsResolved = true)) return

        resolveAnnotationSymbol(symbol, scopeSession)
    }

    open fun resolveAnnotationSymbol(symbol: FirRegularClassSymbol, scopeSession: ScopeSession) {
        val designation = DesignationState.create(symbol, emptyMap(), includeFile = true) ?: return
        val transformer = FirDesignatedCompilerRequiredAnnotationsResolveTransformer(
            designation.firstDeclaration.moduleData.session,
            scopeSession,
            this,
            designation,
        )

        designation.firstDeclaration.transformSingle(transformer, null)
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

class FirSpecificAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : AbstractFirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession) {
    override fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean {
        /*
         * Even if annotations on class are resolved, annotations on nested declarations might be not resolved yet
         * It may happen if we visited a top-level class with designated transformer with this class as target of designation
         */
        if (declaration is FirRegularClass) return true
        @OptIn(PrivateForInline::class)
        return !computationSession.annotationsAreResolved(declaration, treatNonSourceDeclarationsAsResolved = true)
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

fun <F : FirClassLikeDeclaration> F.runCompilerRequiredAnnotationsResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    useSiteFile: FirFile,
    containingDeclarations: List<FirDeclaration>,
): F {
    val computationSession = CompilerRequiredAnnotationsComputationSession()
    val annotationsResolveTransformer = FirSpecificAnnotationForLocalClassesResolveTransformer(
        session,
        scopeSession,
        computationSession,
        containingDeclarations,
        localClassesNavigationInfo
    )
    return annotationsResolveTransformer.withFileScopes(useSiteFile) {
        this.transformSingle(annotationsResolveTransformer, null)
    }
}

private class FirSpecificAnnotationForLocalClassesResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession,
    containingDeclarations: List<FirDeclaration>,
    private val localClassesNavigationInfo: LocalClassesNavigationInfo
) : AbstractFirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession, containingDeclarations) {
    override fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean {
        return when (declaration) {
            is FirClassLikeDeclaration -> declaration in localClassesNavigationInfo.parentForClass
            else -> true
        }
    }

    override val shouldRecordIntoPredicateBasedProvider: Boolean
        get() = false
}
