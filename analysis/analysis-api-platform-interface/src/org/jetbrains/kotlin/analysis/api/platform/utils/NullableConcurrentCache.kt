/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@JvmInline
public value class NullableConcurrentCache<K, V>(
    public val map: ConcurrentMap<K, Any> = ConcurrentHashMap(),
) {
    public inline fun getOrPut(
        key: K,
        crossinline compute: (K) -> V?,
    ): V {
        val value = map.getOrPut(key) { compute(key) ?: NullValue }
        return value.nullValueToNull()
    }
}

public object NullValue

@Suppress("UNCHECKED_CAST")
public fun <V> Any.nullValueToNull(): V = when (this) {
    NullValue -> null
    else -> this
} as V
