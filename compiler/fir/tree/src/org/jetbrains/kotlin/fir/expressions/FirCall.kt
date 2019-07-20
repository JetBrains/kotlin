/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
abstract class FirCall(psi: PsiElement?) : FirExpression(psi) {
    abstract val arguments: List<FirExpression>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }

    abstract fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCall
}