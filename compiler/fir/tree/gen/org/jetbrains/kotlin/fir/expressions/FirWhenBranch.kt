/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhenBranch : FirPureAbstractElement(), FirElement {
    abstract override val source: FirSourceElement?
    abstract val condition: FirExpression
    abstract val result: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhenBranch(this, data)

    abstract fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhenBranch

    abstract fun <D> transformResult(transformer: FirTransformer<D>, data: D): FirWhenBranch

    abstract fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenBranch
}
