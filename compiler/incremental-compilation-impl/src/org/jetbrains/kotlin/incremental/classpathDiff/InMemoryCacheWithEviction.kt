/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.utils.ThreadSafe
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.NotThreadSafe

/**
 * In-memory cache that uses a combination of strong references and [SoftReference]s so that it adapts to memory availability.
 *
 * Cache eviction is performed when a user of this cache calls [evictEntries]. Evicted cache entries' values will be converted from strong
 * references into [SoftReference]s so that they can still be used for as long as the JVM allows them.
 *
 * There are 2 types of cache eviction:
 *   - Least recently used: Oldest entries will be evicted (see [maxTimePeriods])
 *   - Memory usage limit: If memory is limited, all entries will be evicted (see [maxMemoryUsageRatio])
 */
@ThreadSafe
class InMemoryCacheWithEviction<KEY, VALUE>(

    /**
     * Cache entries' values that were not used within [maxTimePeriods] will be converted into [SoftReference]s (they can still be used for
     * some more time until being garbage collected).
     *
     * The time period starts from 0 and will increment by 1 whenever [newTimePeriod] is called.
     */
    private val maxTimePeriods: Int,

    /**
     * If [memoryUsageRatio] > [maxMemoryUsageRatio], all cache entries' values will be converted into [SoftReference]s (they can still be
     * used for some more time until being garbage collected).
     */
    private val maxMemoryUsageRatio: Double,

    /**
     * [TEST-ONLY] Function that returns the current memory usage ratio. It's here only for unit tests to provide custom values.
     * Production code should not provide a value (the default function below will be used).
     */
    private val memoryUsageRatio: () -> Double = {
        Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()).toDouble() / it.maxMemory() }
    }
) {

    /** The current time period, which starts from 0 and will increment by 1 whenever [newTimePeriod] is called. */
    private val currentTimePeriod = AtomicInteger(0)

    private val cache = ConcurrentHashMap<KEY, CacheEntryValue<VALUE>>()

    fun newTimePeriod() {
        currentTimePeriod.incrementAndGet()
    }

    fun computeIfAbsent(key: KEY, valueProvider: (KEY) -> VALUE): VALUE {
        return readLock { // Read lock so that this method can be called concurrently
            val cacheEntryValue = cache.computeIfAbsent(key) { // `cache` is thread-safe
                CacheEntryValue(value = valueProvider(key), currentTimePeriod = currentTimePeriod.get())
            }
            synchronized(cacheEntryValue) { // Needs synchronization as CacheEntryValue is not thread-safe
                val value = cacheEntryValue.get() ?: valueProvider(key)
                cacheEntryValue.setStrongReference(value, currentTimePeriod.get())
                value
            }
        }
    }

    fun evictEntries() {
        writeLock { // Write lock so that other threads don't read/write the cache while this thread is updating it
            // If memory is limited, evict all entries (turn their values into `SoftReference`s)
            val entriesToEvict = if (memoryUsageRatio() > maxMemoryUsageRatio) {
                cache.values
            } else {
                // Otherwise, evict least-recently-used entries
                val lowestTimePeriodToRetain = currentTimePeriod.get() - maxTimePeriods + 1
                if (lowestTimePeriodToRetain > 0) {
                    cache.filterValues { it.lastUsed() < lowestTimePeriodToRetain }.values
                } else emptyList()
            }
            entriesToEvict.forEach { it.updateToSoftReference() }

            // Also remove entries whose values are already garbage collected
            cache.filterValues { it.valueWasGarbageCollected() }.keys.forEach {
                cache.remove(it)
            }
        }
    }

    private val lock = ReentrantReadWriteLock()

    private inline fun writeLock(action: () -> Unit) {
        lock.writeLock().lock()
        try {
            action()
        } finally {
            lock.writeLock().unlock()
        }
    }

    private inline fun <VALUE> readLock(action: () -> VALUE): VALUE {
        lock.readLock().lock()
        try {
            return action()
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * Returns the following numbers for debugging (in order):
     *   - Number of cache entries whose values are strong references
     *   - Number of cache entries whose values are referred to by a [SoftReference] and are not yet garbage collected
     *   - Number of cache entries whose values are referred to by a [SoftReference] and have already been garbage collected
     */
    fun countCacheEntriesForDebug(): Triple<Int, Int, Int> {
        return readLock {
            var strongRefs = 0
            var aliveSoftRefs = 0
            var deadSoftRefs = 0
            // Note: Each iteration of the loop is atomic, but this loop is not atomic (new cache entries may be added or existing cache
            // entries may be updated by `computeIfAbsent` while this loop is running). If we need this loop to be atomic, we can use
            // `writeLock` instead of `readLock`, but we don't need it to be atomic.
            cache.values.forEach {
                synchronized(it) {
                    when {
                        !it.wasEvicted() -> strongRefs++
                        it.get() != null -> aliveSoftRefs++
                        else -> deadSoftRefs++
                    }
                }
            }
            Triple(strongRefs, aliveSoftRefs, deadSoftRefs)
        }
    }

    @VisibleForTesting
    internal fun keyWasEvicted(key: KEY): Boolean {
        return readLock {
            cache[key]?.let {
                synchronized(it) {
                    it.wasEvicted()
                }
            } ?: true
        }
    }

}

@NotThreadSafe // Not thread-safe to improve performance. The caller must take care of synchronization when using this class.
private class CacheEntryValue<VALUE> private constructor(

    private var strongRef: VALUE?,

    private var softRef: SoftReference<VALUE>?, // Not null iff strongRef == null

    /** The most recent time period when this [CacheEntryValue] was used. */
    private var lastUsed: Int
) {

    constructor(value: VALUE, currentTimePeriod: Int) : this(strongRef = value, softRef = null, lastUsed = currentTimePeriod)

    fun get(): VALUE? = strongRef ?: softRef!!.get()

    fun setStrongReference(value: VALUE, currentTimePeriod: Int) {
        strongRef = value
        softRef = null
        lastUsed = currentTimePeriod
    }

    fun updateToSoftReference() {
        if (strongRef != null) {
            softRef = SoftReference(strongRef)
            strongRef = null
        }
    }

    fun wasEvicted(): Boolean = (strongRef == null)

    fun valueWasGarbageCollected(): Boolean = (get() == null)

    fun lastUsed() = lastUsed
}
