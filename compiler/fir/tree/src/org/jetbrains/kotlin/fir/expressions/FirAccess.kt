/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirAccess : FirStatement {
    val calleeReference: FirReference

    val safe: Boolean get() = false

    val explicitReceiver: FirExpression? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAccess(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}