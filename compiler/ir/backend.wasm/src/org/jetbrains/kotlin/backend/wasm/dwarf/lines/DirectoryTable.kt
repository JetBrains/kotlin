/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.lines

import org.jetbrains.kotlin.backend.wasm.dwarf.StringTable
import org.jetbrains.kotlin.backend.wasm.dwarf.utils.IndexedSet

class DirectoryTable : Iterable<StringTable.StringRef> {
    private val set = IndexedSet<StringTable.StringRef>()

    val size: Int get() = set.size

    fun add(path: StringTable.StringRef): DirectoryId = DirectoryId(set.add(path))

    override fun iterator() = set.iterator()

    @JvmInline value class DirectoryId(val index: Int)
}

