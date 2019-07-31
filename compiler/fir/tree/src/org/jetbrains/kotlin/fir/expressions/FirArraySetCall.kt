/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirArraySetCall(
    psi: PsiElement?
) : @VisitedSupertype FirCall(psi), FirAssignment {
    // NB: arguments of this thing are indexes AND rvalue
    abstract val indexes: List<FirExpression>

    override val arguments get() = indexes + rValue

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitArraySetCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCall>.acceptChildren(visitor, data)
        calleeReference.accept(visitor, data)
        rValue.accept(visitor, data)
    }
}