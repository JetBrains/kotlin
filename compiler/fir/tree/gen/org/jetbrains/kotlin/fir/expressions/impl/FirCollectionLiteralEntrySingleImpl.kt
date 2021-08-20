/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteralEntrySingle
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirCollectionLiteralEntrySingleImpl(
    override val source: FirSourceElement?,
    override var expression: FirExpression,
) : FirCollectionLiteralEntrySingle() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCollectionLiteralEntrySingleImpl {
        transformExpression(transformer, data)
        return this
    }

    override fun <D> transformExpression(transformer: FirTransformer<D>, data: D): FirCollectionLiteralEntrySingleImpl {
        expression = expression.transform(transformer, data)
        return this
    }
}
