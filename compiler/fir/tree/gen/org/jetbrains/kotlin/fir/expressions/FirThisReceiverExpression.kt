/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirThisReceiverExpression : FirQualifiedAccessExpression() {
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val source: KtSourceElement?
    abstract override val calleeReference: FirThisReference
    abstract val isImplicit: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitThisReceiverExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformThisReceiverExpression(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)

    @FirImplementationDetail
    abstract override fun replaceSource(newSource: KtSourceElement?)

    abstract fun replaceCalleeReference(newCalleeReference: FirThisReference)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    abstract override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression
}
