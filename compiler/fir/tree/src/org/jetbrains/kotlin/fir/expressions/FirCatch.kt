/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
interface FirCatch : FirElement {
    val parameter: FirValueParameter

    val block: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        parameter.accept(visitor, data)
        block.accept(visitor, data)
    }

    fun <D> transformParameter(transformer: FirTransformer<D>, data: D): FirCatch

    fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCatch
}