/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

interface FirElement {
    val psi: PsiElement?

    val session: FirSession

    fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)

    fun accept(visitor: FirVisitorVoid) =
        accept(visitor, null)

    fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    fun acceptChildren(visitor: FirVisitorVoid) =
        acceptChildren(visitor, null)

    fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =
        CompositeTransformResult(accept(visitor, data))

    fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
}