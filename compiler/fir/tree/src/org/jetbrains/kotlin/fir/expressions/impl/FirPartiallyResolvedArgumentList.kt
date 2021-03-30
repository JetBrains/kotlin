/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirPartiallyResolvedArgumentList internal constructor(
    override var source: FirSourceElement?,
    private var _mapping: LinkedHashMap<FirExpression, FirValueParameter?>
) : FirArgumentList() {

    @Suppress("UNCHECKED_CAST")
    val mapping: LinkedHashMap<FirExpression, FirValueParameter> =
        _mapping.filterValues { it != null } as LinkedHashMap<FirExpression, FirValueParameter>


    override val arguments: List<FirExpression>
        get() = _mapping.keys.toList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        _mapping.forEach { (k, _) -> k.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPartiallyResolvedArgumentList {
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirPartiallyResolvedArgumentList {
        _mapping = _mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<FirExpression, FirValueParameter?>
        return this
    }
}
