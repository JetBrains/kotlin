/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirWrappedExpression(psi: PsiElement?) : FirExpression(psi) {
    abstract val expression: FirExpression

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        throw AssertionError("We should not try to replace type reference in ${this::class}")
    }

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitWrappedExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        expression.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}