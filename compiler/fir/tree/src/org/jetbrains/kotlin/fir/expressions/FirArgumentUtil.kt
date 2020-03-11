/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

fun buildUnaryArgumentList(argument: FirExpression): FirArgumentList = buildArgumentList {
    arguments += argument
}

fun buildBinaryArgumentList(left: FirExpression, right: FirExpression): FirArgumentList = buildArgumentList {
    arguments += left
    arguments += right
}

abstract class FirAbstractArgumentList : FirArgumentList() {
    override val source: FirSourceElement?
        get() = null

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArgumentList {
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        // DO NOTHING
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}

object FirEmptyArgumentList : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = emptyList()
}

class FirArraySetArgumentList(
    private val rValue: FirExpression,
    private val indexes: List<FirExpression>
) : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = indexes + rValue
}