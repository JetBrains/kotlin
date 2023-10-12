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

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirDeclarationContainer
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer

inline fun <reified T : BirElement> MutableList<T>.transformInPlace(transformation: (T) -> BirElement) {
    for (i in 0 until size) {
        set(i, transformation(get(i)) as T)
    }
}

fun <T : BirElement, D> MutableList<T>.transformInPlace(transformer: BirElementTransformer<D>, data: D) {
    for (i in 0 until size) {
        // Cast to BirElementBase to avoid casting to interface and invokeinterface, both of which are slow.
        @Suppress("UNCHECKED_CAST")
        set(i, (get(i) as BirElementBase).transform(transformer, data) as T)
    }
}

fun <T : BirElement, D> Array<T?>.transformInPlace(transformer: BirElementTransformer<D>, data: D) {
    for (i in indices) {
        // Cast to BirElementBase to avoid casting to interface and invokeinterface, both of which are slow.
        val element = get(i) as BirElementBase?
        if (element != null) {
            @Suppress("UNCHECKED_CAST")
            set(i, element.transform(transformer, data) as T)
        }
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
 * Transforms the list of elements with the given transformer. Return the same List instance if no element instances have changed.
 */
fun <T : BirElement, D> List<T>.transformIfNeeded(transformer: BirElementTransformer<D>, data: D): List<T> {
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
