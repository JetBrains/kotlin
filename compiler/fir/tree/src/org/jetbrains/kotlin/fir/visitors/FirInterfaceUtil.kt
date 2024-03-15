/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementInterface
import org.jetbrains.kotlin.fir.MutableOrEmptyList

// This file provides utility methods that allow one to use visitors and transformers API over fir interfaces.
// Since any class implementing FirElementInterface must inherit from FirElement, all raw type conversions are safe.

inline fun <reified TElement : FirElementInterface, D> TElement.transformSingle(transformer: FirTransformer<D>, data: D): TElement {
    return (this as FirElement).transformSingle(transformer, data) as TElement
}

@JvmName("transformInplace1")
inline fun <reified T : FirElementInterface, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, data: D) {
    @Suppress("UNCHECKED_CAST")
    (this as MutableList<FirElement>).transformInplace(transformer, data)
}

@JvmName("transformInplace2")
inline fun <reified T : FirElementInterface, D> MutableOrEmptyList<T>.transformInplace(transformer: FirTransformer<D>, data: D) {
    @Suppress("UNCHECKED_CAST")
    (this as MutableOrEmptyList<FirElement>).transformInplace(transformer, data)
}

@JvmName("transformInplace3")
inline fun <reified T : FirElementInterface, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, dataProducer: (Int) -> TransformData<D>) {
    @Suppress("UNCHECKED_CAST")
    (this as MutableList<FirElement>).transformInplace(transformer, dataProducer)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <R, D> FirElementInterface.accept(visitor: FirVisitor<R, D>, data: D): R {
    return (this as FirElement).accept(visitor, data)
}

@Suppress("NOTHING_TO_INLINE")
inline fun FirElementInterface.accept(visitor: FirVisitorVoid) = accept(visitor, null)

inline fun <reified E : FirElementInterface, D> FirElementInterface.transform(transformer: FirTransformer<D>, data: D): E {
    return (this as FirElement).transform(transformer, data)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <D> FirElementInterface.transformChildren(transformer: FirTransformer<D>, data: D): FirElementInterface {
    return (this as FirElement).transformChildren(transformer, data)
}