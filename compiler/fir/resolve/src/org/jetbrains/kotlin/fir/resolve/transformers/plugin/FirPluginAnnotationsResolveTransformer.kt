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
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.name.FqName

class FirPluginAnnotationsResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirPluginAnnotationsResolveTransformer(session, scopeSession)
}

class FirPluginAnnotationsResolveTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractPhaseTransformer<Any?>(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS) {
    private val annotationTransformer = FirAnnotationResolveTransformer(session, scopeSession)
    private val importTransformer = FirPartialImportResolveTransformer(session)

    val extensionService = session.extensionService
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        if (!extensionService.hasPredicateBasedExtensions) return file
        val registeredPluginAnnotations = session.registeredPluginAnnotations
        file.replaceResolvePhase(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS)
        val newAnnotations = file.resolveAnnotations(registeredPluginAnnotations.annotations, registeredPluginAnnotations.metaAnnotations)
        if (!newAnnotations.isEmpty) {
            for (metaAnnotation in newAnnotations.keySet()) {
                registeredPluginAnnotations.registerUserDefinedAnnotation(metaAnnotation, newAnnotations[metaAnnotation])
            }
            val newAnnotationsFqns = newAnnotations.values().mapTo(mutableSetOf()) { it.symbol.classId.asSingleFqName() }
            file.resolveAnnotations(newAnnotationsFqns, emptySet())
        }
        return file
    }

    private fun FirFile.resolveAnnotations(
        annotations: Set<AnnotationFqn>,
        metaAnnotations: Set<AnnotationFqn>
    ): Multimap<AnnotationFqn, FirRegularClass> {
        importTransformer.acceptableFqNames = annotations
        this.transformImports(importTransformer, null)

        annotationTransformer.metaAnnotations = metaAnnotations
        val newAnnotations = LinkedHashMultimap.create<AnnotationFqn, FirRegularClass>()
        this.transform<FirFile, Multimap<AnnotationFqn, FirRegularClass>>(annotationTransformer, newAnnotations)
        return newAnnotations
    }
}

private class FirPartialImportResolveTransformer(
    session: FirSession
) : FirImportResolveTransformer(session, FirResolvePhase.ANNOTATIONS_FOR_PLUGINS) {
    var acceptableFqNames: Set<FqName> = emptySet()

    override val FqName.isAcceptable: Boolean
        get() = this in acceptableFqNames
}

private class FirAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractAnnotationResolveTransformer<Multimap<AnnotationFqn, FirRegularClass>, PersistentList<FirAnnotatedDeclaration<*>>>(session, scopeSession) {
    var metaAnnotations: Set<AnnotationFqn> = emptySet()
    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        session,
        errorTypeAsResolved = false
    )

    private var owners: PersistentList<FirAnnotatedDeclaration<*>> = persistentListOf()

    override fun beforeChildren(declaration: FirAnnotatedDeclaration<*>): PersistentList<FirAnnotatedDeclaration<*>> {
        val current = owners
        owners = owners.add(declaration)
        return current
    }

    override fun afterChildren(state: PersistentList<FirAnnotatedDeclaration<*>>?) {
        requireNotNull(state)
        owners = state
    }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): FirStatement {
        return annotationCall.transformAnnotationTypeRef(typeResolverTransformer, scope)
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): FirStatement {
        return super.transformRegularClass(regularClass, data).also {
            if (regularClass.classKind == ClassKind.ANNOTATION_CLASS && metaAnnotations.isNotEmpty()) {
                val annotations = regularClass.annotations.mapNotNull { it.fqName(session) }
                for (annotation in annotations.filter { it in metaAnnotations }) {
                    data.put(annotation, regularClass)
                }
            }
        }
    }

    override fun <T : FirAnnotatedDeclaration<T>> transformAnnotatedDeclaration(
        annotatedDeclaration: FirAnnotatedDeclaration<T>,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): FirAnnotatedDeclaration<T> {
        return super.transformAnnotatedDeclaration(annotatedDeclaration, data).also {
            session.predicateBasedProvider.registerAnnotatedDeclaration(annotatedDeclaration, owners)
        }
    }
}
