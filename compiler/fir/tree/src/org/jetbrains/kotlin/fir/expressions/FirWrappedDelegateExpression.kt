/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirWrappedDelegateExpression(psi: PsiElement?) : FirWrappedExpression(psi) {
    abstract val delegateProvider: FirExpression

    override val typeRef: FirTypeRef
        get() = expression.typeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitWrappedDelegateExpression(this, data)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegateProvider.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}