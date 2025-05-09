/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.util.openAddressHashTable

/**
 * [Map] which allows store null values
 */
@OptIn(PrivateForInline::class)
@JvmInline
value class NullableMap<K : Any, V>(
    @property:PrivateForInline
    val map: MutableMap<K, Any> = openAddressHashTable()
) {

    /**
     * Get value if it is present in map
     * Execute [orElse] otherwise and return it result,
     * [orElse] can modify the map inside
     */
    @Suppress("UNCHECKED_CAST")
    inline fun getOrElse(key: K, orElse: () -> V): V =
        when (val value = map[key]) {
            null -> orElse()
            NullValue -> null
            else -> value
        } as V

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun set(key: K, value: V) {
        map[key] = value ?: NullValue
    }

    @PrivateForInline
    object NullValue
}

inline fun <K : Any, V> NullableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return getOrElse(key) {
        defaultValue().also {
            set(key, it)
        }
    }
}
