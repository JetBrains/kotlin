/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.utils

abstract class DebugEntityTable<E, I> : Iterable<E> {
    val size: Int get() = set.size
    private val set = IndexedSet<E>()

    fun add(entity: E): I = computeId(set.add(entity))
    override fun iterator() = set.iterator()

    protected abstract fun computeId(index: Int): I
}