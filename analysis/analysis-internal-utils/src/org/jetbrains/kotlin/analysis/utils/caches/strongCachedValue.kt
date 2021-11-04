/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.caches

import com.intellij.openapi.util.ModificationTracker
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

public class StrongRefModificationTrackerBasedCache<T> internal constructor(
    private val dependencies: List<ModificationTracker>,
    private val compute: () -> T,
) {
    private val cached = AtomicReference<CachedValue<T>?>(null)

    public operator fun getValue(thisRef: Any?, property: KProperty<*>): T = cached.updateAndGet { value ->
        when {
            value == null -> createNewCachedValue()
            value.isUpToDate(dependencies) -> value
            else -> createNewCachedValue()
        }
    }!!.value

    private fun createNewCachedValue() = CachedValue(compute(), dependencies.map { it.modificationCount })
}

private class CachedValue<T>(val value: T, val timestamps: List<Long>) {
    fun isUpToDate(dependencies: List<ModificationTracker>): Boolean {
        check(timestamps.size == dependencies.size)
        for (i in timestamps.indices) {
            if (dependencies[i].modificationCount != timestamps[i]) {
                return false
            }
        }
        return true
    }
}

/**
 * Create modification tracker which will be invalidated when dependencies change.
 * The cached value is hold on the strong reference.
 * So, the value will not be garbage collected until modification tracker changes.
 */
public fun <T> strongCachedValue(
    vararg dependencies: ModificationTracker,
    compute: () -> T,
): StrongRefModificationTrackerBasedCache<T> = StrongRefModificationTrackerBasedCache(dependencies.toList(), compute)