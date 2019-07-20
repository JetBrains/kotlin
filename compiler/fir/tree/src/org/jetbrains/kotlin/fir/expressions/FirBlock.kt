/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirBlock(psi: PsiElement?) : FirUnknownTypeExpression(psi) {
    abstract val statements: List<FirStatement>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (statement in statements) {
            statement.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}