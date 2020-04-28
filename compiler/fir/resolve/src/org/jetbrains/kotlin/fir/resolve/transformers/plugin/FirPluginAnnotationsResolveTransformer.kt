/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.extensionPointService
import org.jetbrains.kotlin.fir.extensions.fqName
import org.jetbrains.kotlin.fir.extensions.hasExtensions
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.FqName

class FirPluginAnnotationsResolveTransformer(private val scopeSession: ScopeSession) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        throw IllegalStateException("Should not be here")
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val extensionPointService = file.session.extensionPointService
        if (!extensionPointService.hasExtensions) return file.compose()
        file.replaceResolvePhase(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS)
        val newAnnotations = file.resolveAnnotations(extensionPointService.annotations, extensionPointService.metaAnnotations)
        if (newAnnotations.isNotEmpty()) {
            for (annotationClass in newAnnotations) {
                extensionPointService.registerUserDefinedAnnotation(annotationClass)
            }
            val newAnnotationsFqns = newAnnotations.mapTo(mutableSetOf()) { it.symbol.classId.asSingleFqName() }
            file.resolveAnnotations(newAnnotationsFqns, emptySet())
        }
        return file.compose()
    }

    private fun FirFile.resolveAnnotations(
        annotations: Set<AnnotationFqn>,
        metaAnnotations: Set<AnnotationFqn>
    ): Set<FirRegularClass> {
        val importTransformer = FirPartialImportResolveTransformer(annotations)
        this.transform<FirFile, Nothing?>(importTransformer, null)

        val annotationTransformer = FirAnnotationResolveTransformer(metaAnnotations, session, scopeSession)
        val newAnnotations = mutableSetOf<FirRegularClass>()
        this.transform<FirFile, MutableSet<FirRegularClass>>(annotationTransformer, newAnnotations)
        return newAnnotations
    }
}

private class FirPartialImportResolveTransformer(
    private val acceptableFqNames: Set<FqName>
) : FirImportResolveTransformer(FirResolvePhase.ANNOTATIONS_FOR_PLUGINS) {
    override val FqName.isAcceptable: Boolean
        get() = this in acceptableFqNames
}

private class FirAnnotationResolveTransformer(
    private val metaAnnotations: Set<AnnotationFqn>,
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractAnnotationResolveTransformer<MutableSet<FirRegularClass>>(session, scopeSession) {
    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(towerScope, session)

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: MutableSet<FirRegularClass>
    ): CompositeTransformResult<FirStatement> {
        return annotationCall.transformAnnotationTypeRef(typeResolverTransformer, null).compose()
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: MutableSet<FirRegularClass>
    ): CompositeTransformResult<FirStatement> {
        return super.transformRegularClass(regularClass, data).also {
            if (regularClass.classKind == ClassKind.ANNOTATION_CLASS && metaAnnotations.isNotEmpty()) {
                val annotations = regularClass.annotations.mapNotNull { it.fqName(session) }
                if (annotations.any { it in metaAnnotations }) {
                    data += regularClass
                }
            }
        }
    }
}