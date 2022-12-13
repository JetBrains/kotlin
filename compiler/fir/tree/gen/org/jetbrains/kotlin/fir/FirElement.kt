/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirElement {
    val source: KtSourceElement?

    fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    @Suppress("UNCHECKED_CAST")
    fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformElement(this, data) as E

    fun accept(visitor: FirVisitorVoid) = accept(visitor, null)

    fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)

    fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)

    fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement
}
