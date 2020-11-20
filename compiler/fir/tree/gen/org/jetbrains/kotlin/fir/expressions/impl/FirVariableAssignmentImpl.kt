/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirVariableAssignmentImpl(
    override val source: FirSourceElement?,
    override var calleeReference: FirReference,
    override val annotations: MutableList<FirAnnotationCall>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override var explicitReceiver: FirExpression?,
    override var dispatchReceiver: FirExpression,
    override var extensionReceiver: FirExpression,
    override var rValue: FirExpression,
) : FirVariableAssignment() {
    override var lValue: FirReference 
        get() = calleeReference
        set(value) {
            calleeReference = value
        }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        rValue.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        transformCalleeReference(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        }
        transformRValue(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirVariableAssignmentImpl {
        rValue = rValue.transformSingle(transformer, data)
        return this
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {
        explicitReceiver = newExplicitReceiver
    }
}
