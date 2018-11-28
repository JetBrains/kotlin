/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.serialization

class Interner<T>(private val parent: Interner<T>? = null) {
    private val firstIndex: Int = parent?.run { interned.size + firstIndex } ?: 0
    private val interned = hashMapOf<T, Int>()

    val allInternedObjects: List<T>
        get() = interned.keys.sortedBy(interned::get)

    val isEmpty: Boolean
        get() = interned.isEmpty() && parent?.isEmpty != false

    private fun find(obj: T): Int? {
        assert(parent == null || parent.interned.size + parent.firstIndex == firstIndex) {
            "Parent changed in parallel with child: indexes will be wrong"
        }
        return parent?.find(obj) ?: interned[obj]
    }

    fun intern(obj: T): Int =
        find(obj) ?: (firstIndex + interned.size).also {
            interned[obj] = it
        }
}
