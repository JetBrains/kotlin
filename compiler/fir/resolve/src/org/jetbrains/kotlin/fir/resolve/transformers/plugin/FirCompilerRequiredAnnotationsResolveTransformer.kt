/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.descriptors.ClassKind
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
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer.Mode
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
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
        val registeredPluginAnnotations = session.registeredPluginAnnotations
        if (!registeredPluginAnnotations.hasRegisteredAnnotations) {
            files.forEach { it.transformSingle(transformer, Mode.RegularAnnotations) }
            return
        }
        if (registeredPluginAnnotations.metaAnnotations.isNotEmpty()) {
            files.forEach { it.transformSingle(transformer, Mode.MetaAnnotations) }
        }
        files.forEach { it.transformSingle(transformer, Mode.RegularAnnotations) }
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
) : FirAbstractPhaseTransformer<Mode>(COMPILER_REQUIRED_ANNOTATIONS) {
    enum class Mode {
        MetaAnnotations,
        RegularAnnotations
    }

    private val annotationTransformer = FirAnnotationResolveTransformer(session, scopeSession)
    private val importTransformer = FirPartialImportResolveTransformer(session)

    val extensionService = session.extensionService
    override fun <E : FirElement> transformElement(element: E, data: Mode): E {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Mode): FirFile {
        checkSessionConsistency(file)
        val registeredPluginAnnotations = session.registeredPluginAnnotations
        val regularAnnotations: Set<AnnotationFqn>
        val metaAnnotations: Set<AnnotationFqn>
        when (data) {
            Mode.MetaAnnotations -> {
                regularAnnotations = emptySet()
                metaAnnotations = registeredPluginAnnotations.metaAnnotations
            }
            Mode.RegularAnnotations -> {
                regularAnnotations = registeredPluginAnnotations.annotations
                metaAnnotations = emptySet()
            }
        }

        val newAnnotations = file.resolveAnnotations(regularAnnotations, metaAnnotations)
        if (!newAnnotations.isEmpty) {
            for (metaAnnotation in newAnnotations.keySet()) {
                registeredPluginAnnotations.registerUserDefinedAnnotation(metaAnnotation, newAnnotations[metaAnnotation])
            }
        }
        return file
    }

    fun <T> withFileAndScopes(file: FirFile, f: () -> T): T {
        annotationTransformer.withFile(file) {
            return annotationTransformer.withFileScopes(file, f)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Mode): FirStatement {
        return annotationTransformer.transformRegularClass(regularClass, LinkedHashMultimap.create())
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Mode): FirStatement {
        return annotationTransformer.transformTypeAlias(typeAlias, LinkedHashMultimap.create())
    }

    private fun FirFile.resolveAnnotations(
        annotations: Set<AnnotationFqn>,
        metaAnnotations: Set<AnnotationFqn>
    ): Multimap<AnnotationFqn, FirRegularClass> {
        val acceptableNames = annotations + metaAnnotations
        importTransformer.acceptableFqNames = acceptableNames
        this.transformImports(importTransformer, null)

        annotationTransformer.acceptableFqNames = acceptableNames
        annotationTransformer.metaAnnotations = metaAnnotations
        val newAnnotations = LinkedHashMultimap.create<AnnotationFqn, FirRegularClass>()
        this.transform<FirFile, Multimap<AnnotationFqn, FirRegularClass>>(annotationTransformer, newAnnotations)
        return newAnnotations
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
) : FirAbstractAnnotationResolveTransformer<Multimap<AnnotationFqn, FirRegularClass>, PersistentList<FirDeclaration>>(
    session, scopeSession
) {
    companion object {
        private val REQUIRED_ANNOTATIONS: Set<ClassId> = setOf(
            Deprecated, DeprecatedSinceKotlin, WasExperimental, JvmRecord
        )

        private val REQUIRED_ANNOTATION_NAMES: Set<Name> = REQUIRED_ANNOTATIONS.mapTo(mutableSetOf()) { it.shortClassName }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    var acceptableFqNames: Set<AnnotationFqn> = emptySet()
    var metaAnnotations: Set<AnnotationFqn> = emptySet()
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

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Multimap<AnnotationFqn, FirRegularClass>): FirStatement {
        return transformAnnotation(annotationCall, data)
    }

    override fun transformAnnotation(
        annotation: FirAnnotation,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): FirStatement {
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

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): FirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
            return super.transformRegularClass(regularClass, data).also {
                if (regularClass.classKind == ClassKind.ANNOTATION_CLASS && metaAnnotations.isNotEmpty()) {
                    val annotations = regularClass.annotations.mapNotNull { it.fqName(session) }
                    for (annotation in annotations.filter { it in metaAnnotations }) {
                        data.put(annotation, regularClass)
                    }
                }
                calculateDeprecations(regularClass)
            }
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Multimap<AnnotationFqn, FirRegularClass>): FirTypeAlias {
        return super.transformTypeAlias(typeAlias, data).also {
            calculateDeprecations(typeAlias)
        }
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: Multimap<AnnotationFqn, FirRegularClass>): FirDeclaration {
        return super.transformDeclaration(declaration, data).also {
            predicateBasedProvider.registerAnnotatedDeclaration(declaration, owners)
        }
    }

    override fun transformFile(file: FirFile, data: Multimap<AnnotationFqn, FirRegularClass>): FirFile {
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
