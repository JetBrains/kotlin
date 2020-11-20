/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirFunctionCallImpl @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotationCall>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override var explicitReceiver: FirExpression?,
    override var dispatchReceiver: FirExpression,
    override var extensionReceiver: FirExpression,
    override var argumentList: FirArgumentList,
    override var calleeReference: FirNamedReference,
) : FirFunctionCall() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        }
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirFunctionCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {
        explicitReceiver = newExplicitReceiver
    }

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: FirNamedReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        require(newCalleeReference is FirNamedReference)
        replaceCalleeReference(newCalleeReference)
    }
}
