/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.fir.caches

import java.util.concurrent.ConcurrentMap

internal object NullValue

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal inline fun <VALUE> Any.nullValueToNull(): VALUE = when (this) {
    NullValue -> null
    else -> this
} as VALUE

internal inline fun <KEY : Any, RESULT> ConcurrentMap<KEY, Any>.getOrPutWithNullableValue(
    key: KEY,
    crossinline compute: (KEY) -> Any?
): RESULT {
    val value = getOrPut(key) { compute(key) ?: NullValue }
    return value.nullValueToNull()
}