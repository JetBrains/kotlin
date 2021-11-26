/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal class FirArgumentListImpl(
    override val source: KtSourceElement?,
    override val arguments: MutableList<FirExpression>,
) : FirArgumentList() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirArgumentListImpl {
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArgumentListImpl {
        // Transform all normal arguments first and then lambda to make CFG correct. See KT-46825
        val postponedFunctionArgs = mutableListOf<Pair<Int, FirAnonymousFunctionExpression>>()
        val iterator = arguments.listIterator()
        while (iterator.hasNext()) {
            val index = iterator.nextIndex()
            val next = iterator.next() as FirPureAbstractElement
            if (next is FirAnonymousFunctionExpression) {
                postponedFunctionArgs += (index to next)
                continue
            }
            val result = next.transform<FirExpression, D>(transformer, data)
            iterator.set(result)
        }
        for ((index, lambda) in postponedFunctionArgs) {
            arguments[index] = lambda.transform(transformer, data)
        }
        return this
    }
}
