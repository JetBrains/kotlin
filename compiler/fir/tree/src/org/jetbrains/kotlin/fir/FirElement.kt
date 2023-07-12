/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

abstract class FirElement : FirElementInterface {
    abstract fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R // = visitor.visitElementInterface(this, data)

    @Suppress("UNCHECKED_CAST")
    abstract fun <E : FirElementInterface, D> transform(transformer: FirTransformer<D>, data: D): E // = transformer.transformElementInterface(this, data) as E

    fun accept(visitor: FirVisitorVoid) = accept(visitor, null)

    abstract fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)

    fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)

    abstract fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElementInterface // TODO: make FirElement instead
}