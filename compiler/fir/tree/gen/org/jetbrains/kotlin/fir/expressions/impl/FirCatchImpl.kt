/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirCatchImpl(
    override val source: KtSourceElement?,
    override var parameter: FirProperty,
    override var block: FirBlock,
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
        parameter = parameter.transform(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        block = block.transform(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirCatchImpl {
        return this
    }
}
