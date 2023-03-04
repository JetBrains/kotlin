/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

inline fun <reified T : IrElement> MutableList<T>.transformInPlace(transformation: (T) -> IrElement) {
    for (i in 0 until size) {
        set(i, transformation(get(i)) as T)
    }
}

fun <T : IrElement, D> MutableList<T>.transformInPlace(transformer: IrElementTransformer<D>, data: D) {
    for (i in 0 until size) {
        // Cast to IrElementBase to avoid casting to interface and invokeinterface, both of which are slow.
        @Suppress("UNCHECKED_CAST")
        set(i, (get(i) as IrElementBase).transform(transformer, data) as T)
    }
}

fun <T : IrElement, D> Array<T?>.transformInPlace(transformer: IrElementTransformer<D>, data: D) {
    for (i in indices) {
        // Cast to IrElementBase to avoid casting to interface and invokeinterface, both of which are slow.
        val element = get(i) as IrElementBase?
        if (element != null) {
            @Suppress("UNCHECKED_CAST")
            set(i, element.transform(transformer, data) as T)
        }
    }
}

/**
 * Transforms a mutable list in place.
 * Each element `it` is replaced with a result of `transformation(it)`,
 * `null` means "keep existing element" (to avoid creating excessive singleton lists).
 */
inline fun <T> MutableList<T>.transformFlat(transformation: (T) -> List<T>?) {
    var i = 0
    while (i < size) {
        val item = get(i)

        i = replaceInPlace(transformation(item), i)
    }
}

/**
 * Transforms a subset of a mutable list in place.
 * Each element `it` that has a type S is replaced with a result of `transformation(it)`,
 * `null` means "keep existing element" (to avoid creating excessive singleton lists).
 */
inline fun <T, reified S : T> MutableList<T>.transformSubsetFlat(transformation: (S) -> List<S>?) {
    var i = 0
    while (i < size) {
        val item = get(i)

        if (item !is S) {
            i++
            continue
        }

        i = replaceInPlace(transformation(item), i)
    }
}

@PublishedApi internal fun <T> MutableList<T>.replaceInPlace(transformed: List<T>?, atIndex: Int): Int {
    var i = atIndex
    when (transformed?.size) {
        null -> i++
        0 -> removeAt(i)
        1 -> set(i++, transformed[0])
        else -> {
            addAll(i, transformed)
            i += transformed.size
            removeAt(i)
        }
    }
    return i
}

/**
 * Transforms declarations in declaration container.
 * Behaves similar to like MutableList<T>.transformFlat but also updates
 * parent property for transformed declarations.
 */
fun IrDeclarationContainer.transformDeclarationsFlat(transformation: (IrDeclaration) -> List<IrDeclaration>?) {
    declarations.transformFlat { declaration ->
        val transformed = transformation(declaration)
        transformed?.forEach { it.parent = this }
        transformed
    }
}

/**
 * Transforms the list of elements with the given transformer. Return the same List instance if no element instances have changed.
 */
fun <T : IrElement, D> List<T>.transformIfNeeded(transformer: IrElementTransformer<D>, data: D): List<T> {
    var result: ArrayList<T>? = null
    for ((i, item) in withIndex()) {
        @Suppress("UNCHECKED_CAST")
        val transformed = item.transform(transformer, data) as T
        if (transformed !== item && result == null) {
            result = ArrayList(this)
        }
        result?.set(i, transformed)
    }
    return result ?: this
}
