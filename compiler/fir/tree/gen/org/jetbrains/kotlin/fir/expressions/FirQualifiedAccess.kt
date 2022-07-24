/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
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


    override fun replaceCalleeReference(newCalleeReference: FirReference)

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)

    @FirImplementationDetail
    fun replaceSource(newSource: KtSourceElement?)
}

inline fun <D> FirQualifiedAccess.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformAnnotations(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceExplicitReceiver(explicitReceiver?.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceDispatchReceiver(dispatchReceiver.transform(transformer, data)) }

inline fun <D> FirQualifiedAccess.transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess  = 
    apply { replaceExtensionReceiver(extensionReceiver.transform(transformer, data)) }
