/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

@Suppress("NOTHING_TO_INLINE")
internal inline fun <K, V> MutableMap<K, MutableList<V>>.addValueFor(element: K, value: V) {
    getOrPut(element) { mutableListOf() } += value
}

internal fun <T> MutableList<T>.replaceFirst(from: T, to: T) {
    val index = indexOf(from)
    if (index < 0) {
        error("$from was not found in $this")
    }
    set(index, to)
}