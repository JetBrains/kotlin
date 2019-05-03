/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirAssignment : FirQualifiedAccess {
    val lValue: FirReference get() = calleeReference

    val rValue: FirExpression

    val operation: FirOperation

    fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirAssignment

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAssignment(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        rValue.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}