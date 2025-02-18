/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * A [CleanableValueReferenceCache] with a [WeakReference] to values [V].
 *
 * @param getCleaner Returns the [ValueReferenceCleaner] that should be invoked after [V] has been collected or removed from the cache. The
 *  function will be invoked once when the value is added to the cache.
 */
@LLFirInternals
class CleanableWeakValueReferenceCache<K : Any, V : Any>(
    val getCleaner: (V) -> ValueReferenceCleaner<V>,
) : CleanableValueReferenceCache<K, V>() {
    override fun createReference(key: K, value: V): ReferenceWithCleanup<K, V> {
        return WeakReferenceWithCleanup(key, value, getCleaner(value), referenceQueue)
    }
}

class WeakReferenceWithCleanup<K, V>(
    override val key: K,
    value: V,
    override val cleaner: ValueReferenceCleaner<V>,
    referenceQueue: ReferenceQueue<V>,
) : WeakReference<V>(value, referenceQueue), ReferenceWithCleanup<K, V> {
    override fun equals(other: Any?): Boolean = equalsImpl(other)
    override fun hashCode(): Int = hashKeyImpl()
}
