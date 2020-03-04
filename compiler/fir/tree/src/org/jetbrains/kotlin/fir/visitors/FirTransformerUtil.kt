/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement

fun <T : FirElement, D> T.transformSingle(transformer: FirTransformer<D>, data: D): T {
    return (this as FirPureAbstractElement).transform<T, D>(transformer, data).single
}

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

inline fun <T : FirElement, D : Any> MutableList<T>.transformInplace(transformer: FirTransformer<D>, dataProducer: (Int) -> D?) {
    val iterator = this.listIterator()
    var index = 0
    while (iterator.hasNext()) {
        val next = iterator.next() as FirPureAbstractElement
        val data = dataProducer(index++) ?: continue
        val result = next.transform<T, D>(transformer, data).single
        iterator.set(result)
    }
}
