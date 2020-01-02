/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirElement {
    val source: FirSourceElement?

    fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    fun accept(visitor: FirVisitorVoid) = accept(visitor, null)

    fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)

    fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)

    @Suppress("UNCHECKED_CAST")
    fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =
        accept(visitor, data) as CompositeTransformResult<E>

    fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement
}
