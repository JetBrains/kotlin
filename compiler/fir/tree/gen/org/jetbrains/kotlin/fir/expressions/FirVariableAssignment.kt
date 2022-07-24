/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirVariableAssignment : FirPureAbstractElement(), FirQualifiedAccess {
    abstract override val calleeReference: FirReference
    abstract override val annotations: List<FirAnnotation>
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val source: KtSourceElement?
    abstract val lValue: FirReference
    abstract val lValueTypeRef: FirTypeRef
    abstract val rValue: FirExpression


    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression)

    @FirImplementationDetail
    abstract override fun replaceSource(newSource: KtSourceElement?)

    abstract fun replaceLValue(newLValue: FirReference)

    abstract fun replaceLValueTypeRef(newLValueTypeRef: FirTypeRef)

    abstract fun replaceRValue(newRValue: FirExpression)
}

inline fun <D> FirVariableAssignment.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformAnnotations(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceExplicitReceiver(explicitReceiver?.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceDispatchReceiver(dispatchReceiver.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceExtensionReceiver(extensionReceiver.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformLValue(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceLValue(lValue.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformLValueTypeRef(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceLValueTypeRef(lValueTypeRef.transform(transformer, data)) }

inline fun <D> FirVariableAssignment.transformRValue(transformer: FirTransformer<D>, data: D): FirVariableAssignment  = 
    apply { replaceRValue(rValue.transform(transformer, data)) }
