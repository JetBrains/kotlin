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
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer

inline fun <reified T : IrElement> MutableList<T>.transform(transformation: (T) -> IrElement) {
    forEachIndexed { i, item ->
        set(i, transformation(item) as T)
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

        val transformed = transformation(item)

        when (transformed?.size) {
            null -> i++
            0 -> removeAt(i)
            1 -> set(i++, transformed.first())
            else -> {
                addAll(i, transformed)
                i += transformed.size
                removeAt(i)
            }
        }
    }
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
 * Similar to `map`. Return the same List instance if no element instances have changed.
 */
inline fun <reified T : IrElement> List<T>.mapOptimized(transformation: (T) -> IrElement): List<T> {
    var result: ArrayList<T>? = null
    for ((i, item) in withIndex()) {
        val transformed = transformation(item) as T
        if (transformed !== item && result == null) {
            result = ArrayList(this)
        }
        result?.set(i, transformed)
    }
    return result ?: this
}