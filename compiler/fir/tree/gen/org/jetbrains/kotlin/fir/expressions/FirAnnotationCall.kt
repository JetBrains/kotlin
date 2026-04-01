/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * An extended representation of an annotation in Kotlin. See more general [FirAnnotation].
 *
 * [FirAnnotationCall] is a [FirCall], so it differs from [FirAnnotation] as it includes more detailed description,
 * despite representing generally the same `@Ann(1, 2)` or something similar.
 * [FirAnnotation] is a more light-weight, so it's used when providing [FirCall] properties is problematic,
 * e.g. in serialization, in Java interop, or in plugins.
 * [FirAnnotationCall] is used mainly for source-based annotation that require resolve.
 *
 * Notable inherited properties from [FirAnnotation]:
 * - [argumentMapping] — the map "name to expression" for annotation arguments
 * - [typeArguments] — annotation type arguments with projection (in/out) if needed
 * - [annotationTypeRef] — type reference bound to this annotation (maybe used e.g. to find a corresponding [FirRegularClass] for the annotation)
 * - [useSiteTarget] — annotation use-site target like GET (`@get:Ann`) or PARAMETER (`@param:Ann`), if any;
 * normally annotation should be moved to corresponding element during raw FIR building phase or, in non-obvious cases,
 * during type resolving phase. Sometimes, e.g. for [AnnotationUseSiteTarget.ALL] or for constructor properties annotation,
 * it's copied to multiple elements. Targets [AnnotationUseSiteTarget.FIELD] and [AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD]
 * are indistinguishable this way, as both occupy a backing field.
 *
 * Notable inherited properties from [FirCall]:
 * - [argumentList] — list of annotation arguments to be resolved. After resolve, they are represented as [FirResolvedArgumentList].
 * - [calleeReference] — reference to an annotation class symbol, either unresolved [FirSimpleNamedReference] or resolved [FirResolvedNamedReference]
 *
 * Note: a declaration of an annotation class, like `annotation class Ann`, is represented by [FirRegularClass].
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.annotationCall]
 */
abstract class FirAnnotationCall : FirAnnotation(), FirCall, FirResolvable {
    abstract override val source: KtSourceElement?
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val useSiteTarget: AnnotationUseSiteTarget?
    abstract override val annotationTypeRef: FirTypeRef
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val argumentList: FirArgumentList
    abstract override val calleeReference: FirReference
    abstract override val argumentMapping: FirAnnotationArgumentMapping
    abstract val annotationResolvePhase: FirAnnotationResolvePhase
    abstract val containingDeclarationSymbol: FirBasedSymbol<*>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformAnnotationCall(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceUseSiteTarget(newUseSiteTarget: AnnotationUseSiteTarget?)

    abstract override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping)

    abstract fun replaceAnnotationResolvePhase(newAnnotationResolvePhase: FirAnnotationResolvePhase)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotationCall

    abstract override fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotationCall

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirAnnotationCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirAnnotationCall
}
