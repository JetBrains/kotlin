/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirCollectionLiteralEntryPair : FirCollectionLiteralEntry() {
    abstract override val source: FirSourceElement?
    abstract val key: FirExpression
    abstract val value: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCollectionLiteralEntryPair(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformCollectionLiteralEntryPair(this, data) as E

    abstract fun <D> transformKey(transformer: FirTransformer<D>, data: D): FirCollectionLiteralEntryPair

    abstract fun <D> transformValue(transformer: FirTransformer<D>, data: D): FirCollectionLiteralEntryPair
}
