/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirWhenBranchImpl(
    override val source: FirSourceElement?,
    override var condition: FirExpression,
    override var result: FirBlock
) : FirWhenBranch() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWhenBranchImpl {
        transformCondition(transformer, data)
        transformResult(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhenBranchImpl {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformResult(transformer: FirTransformer<D>, data: D): FirWhenBranchImpl {
        result = result.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenBranchImpl {
        return this
    }
}
