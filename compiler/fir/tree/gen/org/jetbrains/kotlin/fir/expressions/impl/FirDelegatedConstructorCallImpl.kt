/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirDelegatedConstructorCallImpl(
    override val source: KtSourceElement?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var argumentList: FirArgumentList,
    override var contextReceiverArguments: MutableOrEmptyList<FirExpression>,
    override var constructedTypeRef: FirTypeRef,
    override var dispatchReceiver: FirExpression,
    override var calleeReference: FirReference,
    override val isThis: Boolean,
) : FirDelegatedConstructorCall() {
    override val isSuper: Boolean get() = !isThis

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        contextReceiverArguments.forEach { it.accept(visitor, data) }
        constructedTypeRef.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transform(transformer, data)
        contextReceiverArguments.transformInplace(transformer, data)
        constructedTypeRef = constructedTypeRef.transform(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        dispatchReceiver = dispatchReceiver.transform(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>) {
        contextReceiverArguments = newContextReceiverArguments.toMutableOrEmpty()
    }

    override fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef) {
        constructedTypeRef = newConstructedTypeRef
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }
}
