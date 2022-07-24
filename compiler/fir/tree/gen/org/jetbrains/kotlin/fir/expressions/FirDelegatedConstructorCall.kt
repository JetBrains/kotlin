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
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDelegatedConstructorCall : FirPureAbstractElement(), FirResolvable, FirCall, FirContextReceiverArgumentListOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract val constructedTypeRef: FirTypeRef
    abstract val dispatchReceiver: FirExpression
    abstract override val calleeReference: FirReference
    abstract val isThis: Boolean
    abstract val isSuper: Boolean


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef)

    abstract fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)
}

inline fun <D> FirDelegatedConstructorCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirDelegatedConstructorCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirDelegatedConstructorCall.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }

inline fun <D> FirDelegatedConstructorCall.transformConstructedTypeRef(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceConstructedTypeRef(constructedTypeRef.transform(transformer, data)) }

inline fun <D> FirDelegatedConstructorCall.transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceDispatchReceiver(dispatchReceiver.transform(transformer, data)) }

inline fun <D> FirDelegatedConstructorCall.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall  = 
    apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
