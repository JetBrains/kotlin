/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirQualifiedAccess : FirResolvable {
    val safe: Boolean get() = false

    val explicitReceiver: FirExpression? get() = null

    val dispatchReceiver: FirExpression get() = FirNoReceiverExpression

    val extensionReceiver: FirExpression get() = FirNoReceiverExpression

    fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccess(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        if (dispatchReceiver !== explicitReceiver) {
            dispatchReceiver.accept(visitor, data)
        }
        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
            extensionReceiver.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }

    fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess
}