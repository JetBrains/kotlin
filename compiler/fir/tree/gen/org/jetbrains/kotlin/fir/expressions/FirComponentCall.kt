/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirComponentCall : FirFunctionCall() {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val safe: Boolean
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val arguments: List<FirExpression>
    abstract override val calleeReference: FirNamedReference
    abstract override val explicitReceiver: FirExpression
    abstract val componentIndex: Int

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitComponentCall(this, data)

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirComponentCall

    abstract override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirComponentCall

    abstract override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirComponentCall

    abstract override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirComponentCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirComponentCall

    abstract override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirComponentCall
}
