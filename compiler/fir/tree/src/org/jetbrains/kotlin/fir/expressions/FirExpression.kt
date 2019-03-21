/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirExpression : FirStatement {
    val typeRef: FirTypeRef

    fun replaceTypeRef(newTypeRef: FirTypeRef)

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super.acceptChildren(visitor, data)
        typeRef.accept(visitor, data)
    }
}

fun FirExpression.toResolvedCallableReference(): FirResolvedCallableReference? {
    return (this as? FirQualifiedAccess)?.calleeReference as? FirResolvedCallableReference
}

fun FirExpression.toResolvedCallableSymbol(): ConeCallableSymbol? {
    return toResolvedCallableReference()?.callableSymbol
}