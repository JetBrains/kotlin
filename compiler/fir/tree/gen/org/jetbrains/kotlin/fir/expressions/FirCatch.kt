/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.catchClause]
 */
abstract class FirCatch : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val parameter: FirProperty
    abstract val block: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformCatch(this, data) as E

    abstract fun <D> transformParameter(transformer: FirTransformer<D>, data: D): FirCatch

    abstract fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCatch

    abstract fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirCatch
}
