/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirCallableReferenceAccessImpl(
    override val source: FirSourceElement?
) : FirCallableReferenceAccess(), FirModifiableQualifiedAccess, FirAbstractAnnotatedElement {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var safe: Boolean = false
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    override lateinit var calleeReference: FirNamedReference

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
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        typeRef = typeRef.transformSingle(transformer, data)
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
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        dispatchReceiver = dispatchReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        extensionReceiver = extensionReceiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirCallableReferenceAccessImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
