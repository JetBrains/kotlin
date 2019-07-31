/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeCallWithArgumentList
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirArrayOfCall(psi: PsiElement?) : FirUnknownTypeCallWithArgumentList(psi) {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitArrayOfCall(this, data)
}