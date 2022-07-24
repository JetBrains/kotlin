/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFunctionCall : FirQualifiedAccessExpression(), FirCall {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val argumentList: FirArgumentList
    abstract override val calleeReference: FirNamedReference
    abstract val origin: FirFunctionCallOrigin


    @FirImplementationDetail
    abstract override fun replaceSource(newSource: KtSourceElement?)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract fun replaceCalleeReference(newCalleeReference: FirNamedReference)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)
}

inline fun <D> FirFunctionCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceExplicitReceiver(explicitReceiver?.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceDispatchReceiver(dispatchReceiver.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceExtensionReceiver(extensionReceiver.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirFunctionCall.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirFunctionCall 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
