/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.org.jetbrains.kotlin.analysis.utils.caches

import com.intellij.openapi.util.ModificationTracker
import com.intellij.reference.SoftReference

/**
 * [FlexibleCachedValue] caches a value of type [T] which has a dependency on some [ModificationTracker]. Initially, the value is `null` and
 * only computed once requested via [value]. A new value is only computed if either the current value is out of date, or the value has been
 * collected by the GC after [soften] has been called on this instance. [FlexibleCachedValue] guarantees that [compute] will only be called
 * once per necessary re-computation. Any other threads requesting a value will wait for the new value to be computed.
 *
 * The value is initially held in a hard and soft reference. The cached value can be [soften]ed, which removes the hard reference and leaves
 * only the [SoftReference] that might be collected by the GC. The advantage of [FlexibleCachedValue] over a [SoftReference] is that the
 * developer can decide when the value should be available for collection. This is more robust than relying on the preserving behavior of
 * the GC, which is not guaranteed and differs between JVMs.
 */
public class FlexibleCachedValue<T : Any>(private val compute: () -> Pair<T, ModificationTracker>) {
    @Volatile
    private var softReference: SoftReference<T?> = SoftReference(null)
    private var hardReference: T? = null

    @Volatile
    private var dependency: ModificationTracker? = null

    @Volatile
    private var timestamp: Long? = null

    public val value: T
        get() {
            val value: T? = softReference.get()
            if (value != null && isUpToDate()) {
                // Because `timestamp` isn't synchronized in `isUpToDate`, we have to keep the following scenario in mind: Thread A finds
                // that the value is outdated via `isUpToDate`, thread B gets an outdated `softReference.get()` and stores it in `value`,
                // thread A enters the synchronized block below and updates `timestamp`, thread B calculates `isUpToDate` with the new
                // timestamp, which results in `true`, but thread B returns the old `value` it first got from the old `softReference`. We
                // can avoid this scenario by checking that the `softReference` after `isUpToDate` is still referentially equal to the
                // initially fetched value.
                if (softReference.get() === value) {
                    // While `isUpToDate` is checked, `softReference` cannot be garbage collected because it's referenced on the stack, so
                    // `value` should still be valid at this point.
                    return value
                }
            }

            // The synchronization guarantees that at any point in time, `value` does not return an old value while a new value is being
            // computed. Because a new value is only computed if `isUpToDate` is `false`, and `isUpToDate` stays `false` until `timestamp`
            // has been updated, a competing thread won't return `result` from the fast path above. The synchronization also guarantees that
            // a value is only computed once after invalidation.
            return synchronized(this) {
                // We have to check `isUpToDate` again, because another thread might have already computed the new value, and the current
                // thread might have subsequently acquired the lock and just needs to return the up-to-date value.
                var result = softReference.get()
                if (result == null || !isUpToDate()) {
                    val (computedValue, computedDependency) = compute()
                    result = computedValue
                    hardReference = result
                    softReference = SoftReference(result)
                    dependency = computedDependency
                    timestamp = computedDependency.modificationCount
                }

                // The code above sets `hardReference` to `value` before `softReference` is overwritten, and `hardReference` cannot have
                // been softened at this point due to the synchronization, so the GC cannot have collected `softReference` yet and `result`
                // is valid.
                result
            }
        }

    /**
     * Softens the [FlexibleCachedValue], allowing the GC to collect the value if necessary.
     */
    public fun soften() = synchronized(this) {
        hardReference = null
    }

    private fun isUpToDate(): Boolean {
        val dependency = this.dependency ?: return false
        val timestamp = this.timestamp ?: return false
        return dependency.modificationCount == timestamp
    }
}
