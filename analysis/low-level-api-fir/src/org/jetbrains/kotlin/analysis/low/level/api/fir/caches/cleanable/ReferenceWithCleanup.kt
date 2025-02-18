/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable

interface ReferenceWithCleanup<K, V> {
    val key: K
    val cleaner: ValueReferenceCleaner<V>
    fun get(): V?
}

fun <K, V> ReferenceWithCleanup<K, V>.equalsImpl(other: Any?): Boolean {
    // When the referent is collected, equality should be identity-based (for `processQueue` to remove this very same reference).
    // Hence, we skip the value equality check if the referent has been collected and `get()` returns `null`. If the reference is still
    // valid, this is just a canonical equals on referents for `replace(K,V,V)`.
    //
    // The `cleaner` is not part of equality, because `value` equality implies `cleaner` equivalence.
    if (this === other) return true
    if (other == null || other !is ReferenceWithCleanup<*, *>) return false
    if (key != other.key) return false

    val value = get() ?: return false
    return value == other.get()
}

fun <K, V> ReferenceWithCleanup<K, V>.hashKeyImpl(): Int = key.hashCode()
