/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirComponentCall : FirFunctionCall {
    // Starting from 1, not from 0
    val componentIndex: Int

    override val explicitReceiver: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitComponentCall(this, data)
}