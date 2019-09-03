/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirOperationBasedCall
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// is/!is/as/as?
abstract class FirTypeOperatorCall(
    psi: PsiElement?,
    operation: FirOperation
) : FirOperationBasedCall(psi, operation) {
    val argument: FirExpression get() = arguments.first()

    abstract val conversionTypeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitTypeOperatorCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        conversionTypeRef.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}