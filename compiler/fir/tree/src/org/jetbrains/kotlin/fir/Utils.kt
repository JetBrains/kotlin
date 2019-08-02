/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

fun <T : FirElement, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as FirPureAbstractElement
        val result = next.transform<T, D>(transformer, data)
        if (result.isSingle) {
            iterator.set(result.single)
        } else {
            val resultIterator = result.list.listIterator()
            if (!resultIterator.hasNext()) {
                iterator.remove()
            } else {
                iterator.set(resultIterator.next())
            }
            while (resultIterator.hasNext()) {
                iterator.add(resultIterator.next())
            }
        }

    }
}

fun <T : FirElement, D> T.transformSingle(transformer: FirTransformer<D>, data: D): T {
    return (this as FirPureAbstractElement).transform<T, D>(transformer, data).single
}

fun ModuleInfo.dependenciesWithoutSelf(): Sequence<ModuleInfo> = dependencies().asSequence().filter { it != this }

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)