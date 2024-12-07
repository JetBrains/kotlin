/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.fir.caches.FirCache

internal interface FirCacheWithInvalidation<K : Any, V, CONTEXT> {
    /**
     * Drops the incorrect value from the cache and add a new value instead.
     */
    fun fixInconsistentValue(
        key: K,
        context: CONTEXT & Any,
        inconsistencyMessage: String,
        mapping: (oldValue: V, newValue: V & Any) -> V & Any,
    ): V & Any
}

/**
 * Return cached value or created a new one from [context].
 * This method assumes that we can't return null for not-null context.
 * Logs inconsistency error if it is present.
 *
 * @return not-null [VALUE] in case of [FirCacheWithInvalidation] cache.
 */
internal fun <KEY : Any, VALUE, CONTEXT> FirCache<KEY, VALUE, CONTEXT>.getNotNullValueForNotNullContext(
    key: KEY,
    context: CONTEXT,
): VALUE {
    val value = getValue(key, context)
    @Suppress("CANNOT_CHECK_FOR_ERASED")
    return if (value != null ||
        context == null ||
        this !is FirCacheWithInvalidation<KEY, VALUE, CONTEXT>
    ) {
        value
    } else {
        fixInconsistentValue(
            key = key,
            context = context,
            inconsistencyMessage = "Inconsistency in the cache. Someone without context put a null value in the cache",
            mapping = { old, new -> old ?: new },
        )
    }
}
