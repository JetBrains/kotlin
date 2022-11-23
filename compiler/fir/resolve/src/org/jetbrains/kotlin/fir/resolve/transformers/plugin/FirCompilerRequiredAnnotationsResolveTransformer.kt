/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Deprecated
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DeprecatedSinceKotlin
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRecord
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.WasExperimental

class FirCompilerRequiredAnnotationsResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {

    override fun process(files: Collection<FirFile>) {
        val transformer = FirCompilerRequiredAnnotationsResolveTransformer(session, scopeSession)
        files.forEach {
            withFileAnalysisExceptionWrapping(it) {
                it.transformSingle(transformer, null)
            }
        }
    }

    @OptIn(FirSymbolProviderInternals::class)
    override fun beforePhase() {
        session.generatedDeclarationsSymbolProvider?.disable()
    }

    @OptIn(FirSymbolProviderInternals::class)
    override fun afterPhase() {
        session.generatedDeclarationsSymbolProvider?.enable()
    }
}

open class FirCompilerRequiredAnnotationsResolveTransformer(
    final override val session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractPhaseTransformer<Nothing?>(COMPILER_REQUIRED_ANNOTATIONS) {
    private val annotationTransformer = FirAnnotationResolveTransformer(session, scopeSession)
    private val importTransformer = FirPartialImportResolveTransformer(session)

    val extensionService = session.extensionService
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        checkSessionConsistency(file)
        val registeredPluginAnnotations = session.registeredPluginAnnotations
        val regularAnnotations = registeredPluginAnnotations.annotations

        file.resolveAnnotations(regularAnnotations)
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

    private fun FirFile.resolveAnnotations(annotations: Set<AnnotationFqn>) {
        val acceptableNames = annotations
        importTransformer.acceptableFqNames = acceptableNames
        this.transformImports(importTransformer, null)

        annotationTransformer.acceptableFqNames = acceptableNames
        this.transform<FirFile, Nothing?>(annotationTransformer, null)
    }
}

private class FirPartialImportResolveTransformer(
    session: FirSession
) : FirImportResolveTransformer(session, COMPILER_REQUIRED_ANNOTATIONS) {
    var acceptableFqNames: Set<FqName> = emptySet()

    override val FqName.isAcceptable: Boolean
        get() = this in acceptableFqNames
}

private class FirAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractAnnotationResolveTransformer<Nothing?, PersistentList<FirDeclaration>>(session, scopeSession) {
    companion object {
        private val REQUIRED_ANNOTATIONS: Set<ClassId> = setOf(
            Deprecated, DeprecatedSinceKotlin, WasExperimental, JvmRecord
        )

        private val REQUIRED_ANNOTATION_NAMES: Set<Name> = REQUIRED_ANNOTATIONS.mapTo(mutableSetOf()) { it.shortClassName }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    var acceptableFqNames: Set<AnnotationFqn> = emptySet()

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        session,
        errorTypeAsResolved = false
    )

    private val argumentsTransformer = FirAnnotationArgumentsResolveTransformer(session, scopeSession, COMPILER_REQUIRED_ANNOTATIONS)

    private var owners: PersistentList<FirDeclaration> = persistentListOf()
    private val classDeclarationsStack = ArrayDeque<FirClass>()

    override fun beforeTransformingChildren(parentDeclaration: FirDeclaration): PersistentList<FirDeclaration> {
        val current = owners
        owners = owners.add(parentDeclaration)
        return current
    }

    override fun afterTransformingChildren(state: PersistentList<FirDeclaration>?) {
        requireNotNull(state)
        owners = state
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): FirStatement {
        return transformAnnotation(annotationCall, data)
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Nothing?): FirStatement {
        val annotationTypeRef = annotation.annotationTypeRef
        if (annotationTypeRef !is FirUserTypeRef) return annotation
        val name = annotationTypeRef.qualifier.last().name
        if (name !in REQUIRED_ANNOTATION_NAMES && acceptableFqNames.none { it.shortName() == name }) return annotation

        val transformedAnnotation = annotation.transformAnnotationTypeRef(
            typeResolverTransformer,
            ScopeClassDeclaration(scopes.asReversed(), classDeclarationsStack)
        )
        // TODO: what if we have type alias here?
        if (transformedAnnotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == Deprecated) {
            argumentsTransformer.transformAnnotation(transformedAnnotation, ResolutionMode.ContextDependent)
        }
        return transformedAnnotation
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
            return super.transformRegularClass(regularClass, data).also {
                calculateDeprecations(regularClass)
            }
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirTypeAlias {
        return super.transformTypeAlias(typeAlias, data).also {
            calculateDeprecations(typeAlias)
        }
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: Nothing?): FirDeclaration {
        return super.transformDeclaration(declaration, data).also {
            predicateBasedProvider.registerAnnotatedDeclaration(declaration, owners)
        }
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        withFile(file) {
            return super.transformFile(file, data)
        }
    }

    inline fun <T> withFile(file: FirFile, f: () -> T): T {
        typeResolverTransformer.withFile(file) {
            argumentsTransformer.context.withFile(file, argumentsTransformer.components) {
                return f()
            }
        }
    }

    private fun calculateDeprecations(classLikeDeclaration: FirClassLikeDeclaration) {
        if (classLikeDeclaration.deprecationsProvider == UnresolvedDeprecationProvider) {
            classLikeDeclaration.replaceDeprecationsProvider(classLikeDeclaration.getDeprecationsProvider(session.firCachesFactory))
        }
    }
}
