/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
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

internal class FirCallableReferenceAccessImpl(
    override var source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotation>,
    override val contextReceiverArguments: MutableList<FirExpression>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override var explicitReceiver: FirExpression?,
    override var dispatchReceiver: FirExpression,
    override var extensionReceiver: FirExpression,
    override var calleeReference: FirNamedReference,
    override var hasQuestionMarkAtLHS: Boolean,
) : FirCallableReferenceAccess() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        contextReceiverArguments.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        typeRef = typeRef.transform(transformer, data)
        transformAnnotations(transformer, data)
        contextReceiverArguments.transformInplace(transformer, data)
        transformTypeArguments(transformer, data)
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transform(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transform(transformer, data)
        }
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        dispatchReceiver = dispatchReceiver.transform(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        extensionReceiver = extensionReceiver.transform(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    @FirImplementationDetail
    override fun replaceSource(newSource: KtSourceElement?) {
        source = newSource
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>) {
        contextReceiverArguments.clear()
        contextReceiverArguments.addAll(newContextReceiverArguments)
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {
        explicitReceiver = newExplicitReceiver
    }

    override fun replaceCalleeReference(newCalleeReference: FirNamedReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        require(newCalleeReference is FirNamedReference)
        replaceCalleeReference(newCalleeReference)
    }

    override fun replaceHasQuestionMarkAtLHS(newHasQuestionMarkAtLHS: Boolean) {
        hasQuestionMarkAtLHS = newHasQuestionMarkAtLHS
    }
}
