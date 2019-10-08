/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirQualifiedAccessExpression(
    psi: PsiElement?
) : FirQualifiedAccess, @VisitedSupertype FirUnknownTypeExpression(psi) {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccessExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        dispatchReceiver.accept(visitor, data)
        extensionReceiver.accept(visitor, data)
        super<FirUnknownTypeExpression>.acceptChildren(visitor, data)
    }
}