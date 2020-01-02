/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirArraySetCallImpl(
    override val source: FirSourceElement?,
    override var rValue: FirExpression,
    override val operation: FirOperation
) : FirArraySetCall(), FirModifiableQualifiedAccess, FirAbstractAnnotatedElement {
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var safe: Boolean = false
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    override lateinit var calleeReference: FirReference
    override val arguments: List<FirExpression> get() = indexes + rValue
    override var lValue: FirReference 
        get() = calleeReference
        set(value) {
            calleeReference = value
        }
    override val indexes: MutableList<FirExpression> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        calleeReference.accept(visitor, data)
        rValue.accept(visitor, data)
        indexes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        annotations.transformInplace(transformer, data)
        transformTypeArguments(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        }
        transformCalleeReference(transformer, data)
        transformRValue(transformer, data)
        transformIndexes(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        return this
    }

    override fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        rValue = rValue.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformIndexes(transformer: FirTransformer<D>, data: D): FirArraySetCallImpl {
        indexes.transformInplace(transformer, data)
        return this
    }
}
