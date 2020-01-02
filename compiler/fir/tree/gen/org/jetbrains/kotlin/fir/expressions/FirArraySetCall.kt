/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirArraySetCall : FirPureAbstractElement(), FirQualifiedAccess, FirCall {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val safe: Boolean
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val calleeReference: FirReference
    abstract override val arguments: List<FirExpression>
    abstract val rValue: FirExpression
    abstract val operation: FirOperation
    abstract val lValue: FirReference
    abstract val indexes: List<FirExpression>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitArraySetCall(this, data)

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirArraySetCall

    abstract fun <D> transformIndexes(transformer: FirTransformer<D>, data: D): FirArraySetCall
}
