/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.extensionsService
import org.jetbrains.kotlin.fir.extensions.fqName
import org.jetbrains.kotlin.fir.extensions.hasExtensions
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.FqName

class FirPluginAnnotationsResolveTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractPhaseTransformer<Nothing?>(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS) {
    private val annotationTransformer = FirAnnotationResolveTransformer(session, scopeSession)
    private val importTransformer = FirPartialImportResolveTransformer(session)

    val extensionPointService = session.oldExtensionsService

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        checkSessionConsistency(file)
        if (!extensionPointService.hasExtensions) return file.compose()
        file.replaceResolvePhase(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS)
        val newAnnotations = file.resolveAnnotations(extensionPointService.annotations, extensionPointService.metaAnnotations)
        if (!newAnnotations.isEmpty) {
            for (metaAnnotation in newAnnotations.keySet()) {
                extensionPointService.registerUserDefinedAnnotation(metaAnnotation, newAnnotations[metaAnnotation])
            }
            val newAnnotationsFqns = newAnnotations.values().mapTo(mutableSetOf()) { it.symbol.classId.asSingleFqName() }
            file.resolveAnnotations(newAnnotationsFqns, emptySet())
        }
        return file.compose()
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
) : FirAbstractAnnotationResolveTransformer<Multimap<AnnotationFqn, FirRegularClass>>(session, scopeSession) {
    var metaAnnotations: Set<AnnotationFqn> = emptySet()

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        towerScope,
        session,
        errorTypeAsResolved = false
    )

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): CompositeTransformResult<FirStatement> {
        return annotationCall.transformAnnotationTypeRef(typeResolverTransformer, null).compose()
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: Multimap<AnnotationFqn, FirRegularClass>
    ): CompositeTransformResult<FirStatement> {
        return super.transformRegularClass(regularClass, data).also {
            if (regularClass.classKind == ClassKind.ANNOTATION_CLASS && metaAnnotations.isNotEmpty()) {
                val annotations = regularClass.annotations.mapNotNull { it.fqName(session) }
                for (annotation in annotations.filter { it in metaAnnotations }) {
                    data.put(annotation, regularClass)
                }
            }
        }
    }
}