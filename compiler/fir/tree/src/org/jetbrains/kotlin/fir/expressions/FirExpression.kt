/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotatedStatement
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirExpression(
    psi: PsiElement?
) : FirAnnotatedStatement(psi) {
    abstract val typeRef: FirTypeRef

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)

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
    return toResolvedCallableReference()?.coneSymbol as ConeCallableSymbol?
}