/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirQualifiedAccess : FirResolvable, FirStatement, FirContextReceiverArgumentListOwner {
    override val calleeReference: FirReference
    override val annotations: List<FirAnnotation>
    override val contextReceiverArguments: List<FirExpression>
    val typeArguments: List<FirTypeProjection>
    val explicitReceiver: FirExpression?
    val dispatchReceiver: FirExpression
    val extensionReceiver: FirExpression
    override val source: KtSourceElement?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformQualifiedAccess(this, data) as E

    override fun replaceCalleeReference(newCalleeReference: FirReference)

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)

    @FirImplementationDetail
    fun replaceSource(newSource: KtSourceElement?)

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess
}
