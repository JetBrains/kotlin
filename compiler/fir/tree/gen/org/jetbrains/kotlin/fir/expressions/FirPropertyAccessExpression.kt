/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirPropertyAccessExpression : FirQualifiedAccessExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val calleeReference: FirReference
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>


    @FirImplementationDetail
    abstract override fun replaceSource(newSource: KtSourceElement?)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)
}

inline fun <D> FirPropertyAccessExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceExplicitReceiver(explicitReceiver?.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceDispatchReceiver(dispatchReceiver.transform(transformer, data)) }

inline fun <D> FirPropertyAccessExpression.transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirPropertyAccessExpression 
     = apply { replaceExtensionReceiver(extensionReceiver.transform(transformer, data)) }
