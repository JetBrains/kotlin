/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder.Companion.baseFirElement]
 */
interface FirElement {
    val source: KtSourceElement?

    fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)

    @Suppress("UNCHECKED_CAST")
    fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformElement(this, data) as E

    fun accept(visitor: FirVisitorVoid) = accept(visitor, null)

    fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D)

    fun acceptChildren(visitor: FirVisitorVoid) = acceptChildren(visitor, null)

    fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement
}
