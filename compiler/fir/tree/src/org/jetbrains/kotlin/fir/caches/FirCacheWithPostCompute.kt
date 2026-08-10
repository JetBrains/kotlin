/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import kotlin.reflect.KProperty

@RequiresOptIn("This API exposes FIR cache internals. It should not used in production.")
annotation class FirCacheInternals

/**
 * A cache class with an embedded value computation strategy.
 * It uses key [K] and the passed context [CONTEXT] to compute and cache the value [V].
 *
 * **IMPORTANT**: While this cache uses both the key and the context to compute and cache the value [V],
 * it retrieves the cached value using **only the key**, and **ignores the passed context**.
 *
 * Because of that, you cannot use a single unique key with different non-unique contexts.
 * If you do, one of the contexts will be used to compute the cached value, and the others
 * will effectively be ignored.
 *
 * @see FirCachesFactory
 */
abstract class FirCache<in K : Any, out V, in CONTEXT> {
    abstract fun getValue(key: K, context: CONTEXT): V
    abstract fun getValueIfComputed(key: K): V?

    /**
     * Returns a snapshot of all non-null values in the cache. Changes to the cache do not reflect in the resulting collection.
     */
    @FirCacheInternals
    abstract val cachedValues: Collection<V>
}

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Any, V> FirCache<K, V, Nothing?>.getValue(key: K): V =
    getValue(key, null)

operator fun <K : Any, V> FirCache<K, V, Nothing>.contains(key: K): Boolean {
    return getValueIfComputed(key) != null
}

abstract class FirLazyValue<out V> {
    abstract fun getValue(): V
}

operator fun <V> FirLazyValue<V>.getValue(thisRef: Any?, property: KProperty<*>): V {
    return getValue()
}

/**
 * A lazily computed single value which uses a [CONTEXT] supplied at access time to compute the value [V].
 *
 * Unlike [FirLazyValue], whose computation is captured at construction time, [FirLazyValueWithContext] receives its [CONTEXT] transiently
 * via [getValue], so the context does not have to be retained until the value's computation, nor after. This is useful when the context
 * should not be kept indefinitely.
 *
 * Just like [FirCache]:
 *
 * - The [CONTEXT] is only used for the *first* computation of the value and ignored on subsequent accesses.
 * - The value might be computed more than once when accessed concurrently, but all threads will always observe the same value.
 */
abstract class FirLazyValueWithContext<out V, in CONTEXT> {
    abstract fun getValue(context: CONTEXT): V
}
