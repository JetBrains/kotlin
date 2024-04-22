/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAbstractArgumentList
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle

abstract class FirResolvedArgumentList : FirAbstractArgumentList() {
    /**
     * Contains the original, unresolved [FirArgumentList] which contains [FirNamedArgumentExpression]s,
     * whereas [FirNamedArgumentExpression]s are removed from `this` resolved argument list.
     */
    abstract val originalArgumentList: FirArgumentList?
    abstract val mapping: LinkedHashMap<FirExpression, FirValueParameter>

    final override val source: KtSourceElement?
        get() = originalArgumentList?.source

    override val arguments: List<FirExpression>
        get() = mapping.keys.toList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
    }

    abstract override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArgumentList

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        transformArguments(transformer, data)
        return this
    }
}


internal class FirResolvedArgumentListImpl(
    override val originalArgumentList: FirArgumentList?,
    mapping: LinkedHashMap<FirExpression, FirValueParameter>,
) : FirResolvedArgumentList() {
    override var mapping: LinkedHashMap<FirExpression, FirValueParameter> = mapping
        private set

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArgumentList {
        mapping = mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<FirExpression, FirValueParameter>
        return this
    }
}

internal class FirResolvedArgumentListForErrorCall(
    override val originalArgumentList: FirArgumentList?,
    private var _mapping: LinkedHashMap<FirExpression, out FirValueParameter?>,
) : FirResolvedArgumentList() {

    override var mapping: LinkedHashMap<FirExpression, FirValueParameter> = computeMapping()
        private set

    private fun computeMapping(): LinkedHashMap<FirExpression, FirValueParameter> {
        @Suppress("UNCHECKED_CAST")
        return _mapping.filterValues { it != null } as LinkedHashMap<FirExpression, FirValueParameter>
    }

    override val arguments: List<FirExpression>
        get() = _mapping.keys.toList()

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirResolvedArgumentListForErrorCall {
        _mapping = _mapping.mapKeys { (k, _) -> k.transformSingle(transformer, data) } as LinkedHashMap<FirExpression, FirValueParameter?>
        mapping = computeMapping()
        return this
    }
}
