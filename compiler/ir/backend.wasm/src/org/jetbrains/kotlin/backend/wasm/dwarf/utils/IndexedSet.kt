/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.utils

class IndexedSet<T> : Iterable<T> {
    private val pool = LinkedHashMap<T, Int>()

    val size: Int get() = pool.size

    fun add(element: T): Int = pool.getOrPut(element) { pool.size }

    override operator fun iterator(): Iterator<T> = pool.keys.iterator()
}