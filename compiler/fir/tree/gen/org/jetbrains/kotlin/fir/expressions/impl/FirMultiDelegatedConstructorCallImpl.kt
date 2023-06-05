/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirMultiDelegatedConstructorCallImpl @FirImplementationDetail constructor(
    override val delegatedConstructorCalls: MutableList<FirDelegatedConstructorCall>,
) : FirMultiDelegatedConstructorCall() {
    override val source: KtSourceElement? get() = delegatedConstructorCalls.last().source
    override val annotations: List<FirAnnotation> get() = delegatedConstructorCalls.last().annotations
    override val argumentList: FirArgumentList get() = delegatedConstructorCalls.last().argumentList
    override val contextReceiverArguments: List<FirExpression> get() = delegatedConstructorCalls.last().contextReceiverArguments
    override val constructedTypeRef: FirTypeRef get() = delegatedConstructorCalls.last().constructedTypeRef
    override val dispatchReceiver: FirExpression get() = delegatedConstructorCalls.last().dispatchReceiver
    override val calleeReference: FirReference get() = delegatedConstructorCalls.last().calleeReference
    override val isThis: Boolean get() = delegatedConstructorCalls.last().isThis
    override val isSuper: Boolean get() = !isThis

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegatedConstructorCalls.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCallImpl {
        transformDelegatedConstructorCalls(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCallImpl {
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCallImpl {
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCallImpl {
        return this
    }

    override fun <D> transformDelegatedConstructorCalls(transformer: FirTransformer<D>, data: D): FirMultiDelegatedConstructorCallImpl {
        delegatedConstructorCalls.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {}

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>) {}

    override fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef) {}

    override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression) {}

    override fun replaceCalleeReference(newCalleeReference: FirReference) {}

    override fun replaceDelegatedConstructorCalls(newDelegatedConstructorCalls: List<FirDelegatedConstructorCall>) {
        delegatedConstructorCalls.clear()
        delegatedConstructorCalls.addAll(newDelegatedConstructorCalls)
    }
}
