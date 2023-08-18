/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.intellij.openapi.application.ApplicationManager
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/**
 * [SoftValueCleaner] performs a cleaning operation after its associated value has been removed from the cache or garbage-collected. The
 * cleaner will be strongly referenced from the soft references held by the cache.
 *
 * You **must not** store a reference to the associated value [V] in its [SoftValueCleaner]. Otherwise, the cached values will never become
 * softly reachable.
 *
 * The cleaner may be invoked multiple times by the cache, in any thread. Implementations of [SoftValueCleaner] must ensure that the
 * operation is repeatable and thread-safe.
 */
internal fun interface SoftValueCleaner<V> {
    /**
     * Cleans up after [value] has been removed from the cache or garbage-collected.
     *
     * [value] is non-null if it was removed from the cache and is still referable, or `null` if it has already been garbage-collected.
     */
    fun cleanUp(value: V?)
}

/**
 * A cache with hard references to its keys [K] and soft references to its values [V], which will be cleaned up after manual removal and
 * garbage collection. The cache should only be used in read/write actions, as specified by the individual functions.
 *
 * Each value of the cache has a [SoftValueCleaner] associated with it. The cache ensures that this cleaner is invoked when the value is
 * removed from or replaced in the cache, or when the value has been garbage-collected. Already collected values from the cache's reference
 * queue are guaranteed to be processed on mutating operations (such as `put`, `remove`, and so on). The [SoftValueCleaner] will be strongly
 * referenced from the cache until collected values have been processed.
 *
 * `null` keys or values are not allowed.
 *
 * @param getCleaner Returns the [SoftValueCleaner] that should be invoked after [V] has been collected or removed from the cache. The
 *  function will be invoked once when the value is added to the cache.
 */
internal class CleanableSoftValueCache<K : Any, V : Any>(
    private val getCleaner: (V) -> SoftValueCleaner<V>,
) {
    private val backingMap = ConcurrentHashMap<K, SoftReferenceWithCleanup<K, V>>()

    private val referenceQueue = ReferenceQueue<V>()

    private fun processQueue() {
        while (true) {
            val ref = referenceQueue.poll() ?: break
            check(ref is SoftReferenceWithCleanup<*, *>)

            @Suppress("UNCHECKED_CAST")
            ref as SoftReferenceWithCleanup<K, V>

            backingMap.remove(ref.key, ref)
            ref.performCleanup()
        }
    }

    /**
     * Returns a value for the given [key] if it exists in the map. Must be called from a read action.
     */
    operator fun get(key: K): V? = backingMap[key]?.get()

    /**
     * If [key] is currently absent, attempts to add a value computed by [f] to the cache. [f] will not be invoked if [key] is present. Must
     * be called from a read action.
     *
     * The implementation is not atomic with respect to [f], i.e. the value computation may be run concurrently on multiple threads if more
     * than one thread calls [computeIfAbsent] for the same [key]. The result of [f] may also be ignored. However, the implementation
     * guarantees that the value eventually returned from [computeIfAbsent] for a given [key] is consistent across all calling threads.
     *
     * @return The already present or newly computed value associated with [key].
     */
    fun computeIfAbsent(key: K, f: (K) -> V): V {
        get(key)?.let { return it }

        val newValue = f(key)
        putIfAbsent(key, newValue)?.let { return it }

        return newValue
    }

    /**
     * Adds or replaces [value] to/in the cache at the given [key]. Must be called in a read action.
     *
     * @return The old value that has been replaced, if any. As replacement constitutes removal, the cleaner associated with the value will
     * be invoked by [put].
     */
    fun put(key: K, value: V): V? {
        val oldRef = backingMap.put(key, createSoftReference(key, value))
        oldRef?.performCleanup()

        processQueue()
        return oldRef?.get()
    }

    /**
     * Removes the value associated with [key] from the cache, performs cleanup on it, and returns it if it exists. Must be called in a read
     * action.
     */
    fun remove(key: K): V? {
        val ref = backingMap.remove(key)
        ref?.performCleanup()

        processQueue()
        return ref?.get()
    }

    /**
     * Adds [value] to the cache at the given [key] if no value exists. Must be called in a read action.
     *
     * @return The present value associated with [key], or `null` if it was absent.
     */
    fun putIfAbsent(key: K, value: V): V? {
        val newRef = createSoftReference(key, value)
        while (true) {
            val currentRef = backingMap.putIfAbsent(key, newRef)
            processQueue()
            if (currentRef == null) return null

            // If `currentRef` exists but its value has already been collected, to the outside it should look like no value existed in the
            // cache and `putIfAbsent` should succeed.
            val currentValue = currentRef.get()
            if (currentValue == null) {
                val wasReplaced = backingMap.replace(key, currentRef, newRef)
                if (wasReplaced) {
                    // In most cases, `processQueue` will probably already have invoked the ref's cleaner. However, if the referent is
                    // collected between `processQueue()` and `currentRef.get()`, it won't have been cleaned yet, and we can invoke the
                    // cleaner here. The reference will later be processed by `processQueue`, but that is fine because cleaners can be
                    // invoked multiple times.
                    currentRef.performCleanup()
                    return null
                }
            } else {
                return currentValue
            }
        }
    }

    /**
     * Removes all values from the cache and performs cleanup on them. Must be called in a *write* action.
     *
     * The write action requirement is due to the complexity associated with atomically clearing a concurrent cache while also performing
     * cleanup on exactly the cleared values. Because this cache implementation is used by components which operate in read and write
     * actions, requiring a write action is more economical than synchronizing on some cache-wide lock.
     */
    fun clear() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        // The backing map will not be modified by other threads during `clean` because it is executed in a write action.
        backingMap.values.forEach { it.performCleanup() }
        backingMap.clear()

        processQueue()
    }

    /**
     * Returns the number of elements in the cache. Must be called in a read action.
     */
    val size: Int
        get() {
            // Process the reference queue first to remove values which have already been garbage-collected to get a more accurate size.
            // Still, an accurate size is not fully guaranteed, as additional garbage collection may occur between `processQueue` and the
            // end of the function.
            processQueue()
            return backingMap.size
        }

    /**
     * Returns whether the cache is empty. Must be called in a read action.
     */
    fun isEmpty(): Boolean {
        // Process the reference queue first to remove values which have already been garbage-collected to get a more accurate answer.
        // Still, accuracy is not fully guaranteed, as additional garbage collection may occur between `processQueue` and the end of the
        // function.
        processQueue()
        return backingMap.isEmpty()
    }

    /**
     * Returns a snapshot of all keys in the cache. Changes to the cache do not reflect in the resulting set. Must be called in a read
     * action.
     */
    val keys: Set<K>
        get() {
            // Process the reference queue first to avoid returning keys whose values have already been garbage-collected. Still, this is
            // not fully guaranteed, as additional garbage collection may occur between `processQueue` and the end of the function.
            processQueue()
            return backingMap.keys.toSet()
        }

    override fun toString(): String = "${this::class.simpleName} size:$size"

    private fun createSoftReference(key: K, value: V) = SoftReferenceWithCleanup(key, value, getCleaner(value), referenceQueue)

    private fun SoftReferenceWithCleanup<K, V>.performCleanup() {
        cleaner.cleanUp(get())
    }
}

private class SoftReferenceWithCleanup<K, V>(
    val key: K,
    value: V,
    val cleaner: SoftValueCleaner<V>,
    referenceQueue: ReferenceQueue<V>,
) : SoftReference<V>(value, referenceQueue) {
    override fun equals(other: Any?): Boolean {
        // When the referent is collected, equality should be identity-based (for `processQueue` to remove this very same soft value).
        // Hence, we skip the value equality check if the referent has been collected and `get()` returns `null`. If the reference is still
        // valid, this is just a canonical equals on referents for `replace(K,V,V)`.
        //
        // The `cleaner` is not part of equality, because `value` equality implies `cleaner` equivalence.
        if (this === other) return true
        if (other == null || other !is SoftReferenceWithCleanup<*, *>) return false
        if (key != other.key) return false

        val value = get() ?: return false
        return value == other.get()
    }

    override fun hashCode(): Int = key.hashCode()
}
