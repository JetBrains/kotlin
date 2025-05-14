/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals

/**
 * [ValueReferenceCleaner] performs a cleaning operation after its associated value has been removed from a [CleanableValueReferenceCache]
 * or was garbage-collected. The cleaner will be strongly referenced from the value reference held by the cache.
 *
 * You **must not** store a reference to the associated value [V] in its [ValueReferenceCleaner]. Otherwise, the cached values will never
 * become non-strongly reachable.
 *
 * The cleaner may be invoked multiple times by the cache, in any thread. Implementations of [ValueReferenceCleaner] must ensure that the
 * operation is repeatable and thread-safe.
 */
@LLFirInternals
fun interface ValueReferenceCleaner<V> {
    /**
     * Cleans up after [value] has been removed from the [CleanableValueReferenceCache] or was garbage-collected.
     *
     * [value] is non-null if it was removed from the cache and is still referable, or `null` if it has already been garbage-collected.
     */
    fun cleanUp(value: V?)
}
