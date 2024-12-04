/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.incremental.classpathDiff.InMemoryCacheWithEviction.EntryState.*
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
 * references to [SoftReference]s first. After that, they will either become strong references again if they are used again or get
 * garbage collected/removed from this cache at some point.
 *
 * There are 2 types of cache eviction:
 *   - Least recently used: Oldest entries will be evicted
 *   - Memory usage limit: If memory is limited, all entries will be evicted
 */
@ThreadSafe
class InMemoryCacheWithEviction<KEY, VALUE>(

    /**
     * Cache entries' values that were not used within [maxTimePeriodsToKeepStrongReferences] will be converted to [SoftReference]s.
     *
     * The time period starts from 0 and will increment by 1 whenever [newTimePeriod] is called.
     */
    private val maxTimePeriodsToKeepStrongReferences: Int,

    /**
     * Cache entries' values that were not used within [maxTimePeriodsToKeepStrongReferences] + [maxTimePeriodsToKeepSoftReferences] will be
     * removed from this cache.
     */
    private val maxTimePeriodsToKeepSoftReferences: Int,

    /**
     * If [memoryUsageRatio] > [maxMemoryUsageRatioToKeepStrongReferences], all cache entries' values will be converted to [SoftReference]s.
     */
    private val maxMemoryUsageRatioToKeepStrongReferences: Double,

    /**
     * Function that returns the current memory usage ratio. NOTE: Production code should not provide this function (the default function
     * below will be used). This parameter is here only to allow writing unit tests.
     */
    private val memoryUsageRatio: () -> Double = {
        // Note: In the following formula, memory usage ratio = used memory / total memory. In practice, the JVM may be able to increase
        // total memory to Runtime.maxMemory(), which means that the effective memory usage ratio could be smaller. However, it's also
        // possible that the JVM won't be able to do that (e.g., if Runtime.maxMemory() is too high or not set), so we can't rely on that.
        1.0 - Runtime.getRuntime().let { it.freeMemory().toDouble() / it.totalMemory() }
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
            val lowestTimePeriodToKeepStrongRefs = currentTimePeriod.get() - maxTimePeriodsToKeepStrongReferences + 1
            val lowestTimePeriodToKeepSoftRefs = lowestTimePeriodToKeepStrongRefs - maxTimePeriodsToKeepSoftReferences

            // If memory is limited, convert all entries' values to `SoftReference`s
            if (memoryUsageRatio() > maxMemoryUsageRatioToKeepStrongReferences) {
                cache.values.forEach { it.updateToSoftReference() }
            } else {
                // Otherwise, convert least-recently-used entries' values to `SoftReference`s
                cache.filterValues { it.lastUsed() < lowestTimePeriodToKeepStrongRefs }.values.forEach {
                    it.updateToSoftReference()
                }
            }

            // Remove soft-reference entries that are least recently used or are already garbage collected
            cache.filterValues { it.lastUsed() < lowestTimePeriodToKeepSoftRefs || it.valueWasGarbageCollected() }.keys.forEach {
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

    @VisibleForTesting
    internal enum class EntryState { STRONG_REF, SOFT_REF, ABSENT }

    @VisibleForTesting
    internal fun getEntryState(key: KEY): EntryState {
        return readLock {
            cache[key]?.let {
                synchronized(it) {
                    if (it.valueIsAStrongReference()) STRONG_REF else SOFT_REF
                }
            } ?: ABSENT
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

    fun valueIsAStrongReference(): Boolean = (strongRef != null)

    fun valueWasGarbageCollected(): Boolean = (get() == null)

    fun lastUsed() = lastUsed
}
