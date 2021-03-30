/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.fir.PrivateForInline

/**
 * [Map] which allows store null values
 */
@OptIn(PrivateForInline::class)
@JvmInline
internal value class NullableMap<KEY, VALUE>(private val map: MutableMap<KEY, Any> = HashMap()) {

    /**
     * Get value if it is present in map
     * Execute [orElse] otherwise and return it result,
     * [orElse] can modify the map inside
     */
    @Suppress("UNCHECKED_CAST")
    inline fun getOrElse(key: KEY, orElse: () -> VALUE): VALUE =
        when (val value = map[key]) {
            null -> orElse()
            NullValue -> null
            else -> value
        } as VALUE

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun set(key: KEY, value: VALUE) {
        map[key] = value ?: NullValue
    }
}

@PrivateForInline
internal object NullValue
