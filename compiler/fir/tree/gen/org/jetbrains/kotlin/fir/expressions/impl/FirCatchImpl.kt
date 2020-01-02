/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirCatchImpl(
    override val source: FirSourceElement?,
    override var parameter: FirValueParameter,
    override var block: FirBlock
) : FirCatch() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        parameter.accept(visitor, data)
        block.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        transformParameter(transformer, data)
        transformBlock(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformParameter(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        parameter = parameter.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        block = block.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        return this
    }
}
