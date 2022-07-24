/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement

fun <T : FirElement, D> T.transformSingle(transformer: FirTransformer<D>, data: D): T {
    return (this as FirPureAbstractElement).transform<T, D>(transformer, data)
}

fun <T : FirElement, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as FirPureAbstractElement
        val result = next.transform<T, D>(transformer, data)
        if (result !== next) {
            iterator.set(result)
        }
    }
}

sealed class TransformData<out D> {
    class Data<D>(val value: D) : TransformData<D>()
    object Nothing : TransformData<kotlin.Nothing>()
}

inline fun <T : FirElement, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, dataProducer: (Int) -> TransformData<D>) {
    val iterator = this.listIterator()
    var index = 0
    while (iterator.hasNext()) {
        val next = iterator.next() as FirPureAbstractElement
        val data = when (val data = dataProducer(index++)) {
            is TransformData.Data<D> -> data.value
            TransformData.Nothing -> continue
        }
        val result = next.transform<T, D>(transformer, data)
        if (result !== next) {
            iterator.set(result)
        }
    }
}

fun <R, D> List<FirElement>.acceptAllElements(visitor: FirVisitor<R, D>, data: D) {
    forEach { it.accept(visitor, data) }
}

fun List<FirElement>.acceptAllElements(visitor: FirVisitorVoid) {
    forEach { it.accept(visitor) }
}

inline fun <R, D> FirElement.accept(visitor: FirVisitor<R, D>, data: D): R {
    return visitor.dispatch(this, data)
}

inline fun FirElement.accept(visitor: FirVisitorVoid) {
    return visitor.dispatch(this, null)
}

inline fun <R, D> FirPureAbstractElement.accept(visitor: FirVisitor<R, D>, data: D): R {
    return visitor.dispatch(this, data)
}

inline fun FirPureAbstractElement.accept(visitor: FirVisitorVoid) {
    return visitor.dispatch(this, null)
}

inline fun <T : FirElement, D> T.transform(transformer: FirTransformer<D>, data: D): T {
    return transformer.dispatch(this, data) as T
}

inline fun <T : FirElement, D> FirPureAbstractElement.transform(transformer: FirTransformer<D>, data: D): T {
    return transformer.dispatch(this, data) as T
}

inline fun <R, D> FirElement.acceptChildren(visitor: FirVisitor<R, D>, data: D) {
    return visitor.dispatchChildren(this, data)
}

inline fun FirElement.acceptChildren(visitor: FirVisitorVoid) {
    return visitor.dispatchChildren(this, null)
}

fun <T : FirElement, D> List<T>.transform(transformer: FirTransformer<D>, data: D): List<T> {
    return this.map { it.transform<T, D>(transformer, data) }
}

inline fun <D> FirElement.transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
    return transformer.dispatchTransformChildren(this, data)
}