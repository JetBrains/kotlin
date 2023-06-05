/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirMultiDelegatedConstructorCall : FirDelegatedConstructorCall() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract override val constructedTypeRef: FirTypeRef
    abstract override val dispatchReceiver: FirExpression
    abstract override val calleeReference: FirReference
    abstract override val isThis: Boolean
    abstract override val isSuper: Boolean
    abstract val delegatedConstructorCalls: List<FirDelegatedConstructorCall>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitMultiDelegatedConstructorCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformMultiDelegatedConstructorCall(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract override fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract fun replaceDelegatedConstructorCalls(newDelegatedConstructorCalls: List<FirDelegatedConstructorCall>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCall

    abstract override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCall

    abstract fun <D> transformDelegatedConstructorCalls(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCall
}
