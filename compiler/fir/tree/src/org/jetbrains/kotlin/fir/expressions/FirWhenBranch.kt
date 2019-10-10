/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirWhenBranch : FirElement {
    override val psi: PsiElement?
    val condition: FirExpression
    val result: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhenBranch(this, data)

    fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhenBranch

    fun <D> transformResult(transformer: FirTransformer<D>, data: D): FirWhenBranch

    fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenBranch
}
