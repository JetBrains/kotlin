/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.lang.ref.Reference
import java.lang.ref.SoftReference

/**
 * Creates a new instance of [Cached] that uses the specified [initializer] function to compute the value
 * and caches the computed value through a [SoftReference].
 *
 * This is an analogue of [kotlin.lazy] (in the [LazyThreadSafetyMode.SYNCHRONIZED] mode) with one major difference:
 * the cached value is only *softly* reachable from the returned [Cached] instance.
 * This means that the garbage collector is allowed to reclaim the value when the JVM is running low on memory
 * and the value is not strongly reachable from anywhere else. If this happens, the next access to [Cached.value]
 * runs the [initializer] once again and caches the newly computed value (again, through a soft reference).
 *
 * This makes [softRefCached] a good fit for memory-sensitive caches of values that are expensive to compute,
 * but can be recomputed at any time, and should not prevent the JVM from reclaiming memory when it is needed.
 *
 * ### Initializer
 * - Unlike [kotlin.lazy], the [initializer] is never released: it is retained by the returned [Cached] instance
 *   for its entire lifetime, because it may be needed to recompute the value.
 *   Thus, everything captured by the [initializer] stays strongly reachable.
 * - The [initializer] may be invoked multiple times during the lifetime of the returned [Cached] instance
 *   (but never concurrently, and never while a previously computed value is still available).
 *   It should be free of side effects that must not be repeated, and should produce an equivalent value each time.
 *   Note that a recomputed value is generally a different instance than the previous one, so callers should not rely
 *   on the identity of the value across accesses unless they hold a strong reference to it themselves.
 * - The [initializer] must not capture the computed value (or anything that strongly references it),
 *   otherwise the value becomes strongly reachable, and the soft reference has no effect.
 * - If the [initializer] throws an exception, the exception is propagated to the caller of [Cached.value],
 *   nothing is cached, and the next access to [Cached.value] will attempt to compute the value once again.
 *
 * ### Thread safety
 * Access to [Cached.value] is thread-safe: the value is computed while holding the monitor of the returned
 * [Cached] instance (like [kotlin.lazy] does by default), so the [initializer] is invoked by at most one thread
 * at a time, and all threads that observe a missing value while it is being computed wait for and normally
 * receive the same computed instance. Reading an already computed (and not yet reclaimed) value does not require
 * acquiring the monitor. Avoid synchronizing on the returned [Cached] instance from outside, as this would block
 * the computation of its value.
 *
 * ### `null` values
 * `null` is a valid value: it is cached like any other value and is never reclaimed by the garbage collector,
 * so an [initializer] returning `null` is invoked only once (unless it throws).
 *
 * ### [toString]
 * [toString] returns the string representation of the value if it is currently available,
 * or a placeholder message otherwise. It does not trigger the computation of the value.
 *
 * ### Serialization
 * Unlike the [Lazy] instances returned by [kotlin.lazy], the returned instance is not [java.io.Serializable].
 *
 * Usage example:
 * ```kotlin
 * class Index(private val file: File) {
 *     // Parsed lazily on the first access; re-parsed if the parsed content was reclaimed by the GC.
 *     val entries: List<Entry> by softRefCached { parseEntries(file) }
 * }
 * ```
 *
 * @param initializer the function that computes the value; may be invoked more than once (see above).
 * @return a thread-safe [Cached] instance that holds its computed value through a [SoftReference].
 */
fun <T> softRefCached(initializer: () -> T): Cached<T> = SoftRefCached(initializer)

/**
 * The implementation of [softRefCached], which delegates the creation of the reference holding the computed value
 * to [createReference].
 *
 * The production implementation is [SoftRefCached]. Tests provide their own implementations to simulate
 * the reclamation of the value by the garbage collector deterministically.
 */
internal abstract class AbstractRefCached<out T>(private val initializer: () -> T) : Cached<T> {
    /**
     * The reference to the cached value, or `null` if the value has never been computed.
     * A `null` value is stored as [NullValue] to distinguish it from a reclaimed referent (`get() == null`).
     */
    @Volatile
    private var reference: Reference<Any>? = null

    final override val value: T
        get() {
            // Fast path: the value is computed and not reclaimed.
            reference?.get()?.let { return unwrap(it) }

            // Lock on `this` (like `kotlin.lazy` does by default) to avoid allocating a separate lock object per instance.
            return synchronized(this) {
                // Re-check: another thread may have computed the value while this thread was waiting for the lock.
                reference?.get()?.let { return@synchronized unwrap(it) }

                val newValue = initializer()
                reference = createReference(wrap(newValue))

                // Return the local strong reference: re-reading it from `reference` could yield an already reclaimed value.
                newValue
            }
        }

    final override fun toString(): String {
        val wrapped = reference?.get() ?: return "Cached value not computed yet or has been garbage collected."
        return unwrap(wrapped).toString()
    }

    /**
     * Creates the reference that holds the computed value.
     *
     * It is a method rather than a constructor parameter to avoid storing an extra field per instance.
     */
    protected abstract fun createReference(value: Any): Reference<Any>

    private fun wrap(value: T): Any = value ?: NullValue

    @Suppress("UNCHECKED_CAST")
    private fun unwrap(wrapped: Any): T = (if (wrapped === NullValue) null else wrapped) as T

    /**
     * Represents a cached `null` value.
     * Being a singleton, it is always strongly reachable and thus is never reclaimed by the garbage collector.
     */
    private object NullValue
}

/**
 * The production implementation of [softRefCached] that holds the computed value through a [SoftReference].
 */
private class SoftRefCached<out T>(initializer: () -> T) : AbstractRefCached<T>(initializer) {
    override fun createReference(value: Any): Reference<Any> = SoftReference(value)
}
