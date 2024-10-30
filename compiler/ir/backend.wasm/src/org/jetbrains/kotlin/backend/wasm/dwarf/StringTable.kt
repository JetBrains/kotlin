/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.utils.IndexedSet

class StringTable {
    private val set = IndexedSet<String>()

    fun add(string: String): StringRef = StringRef(set.add(string))

    fun write(section: DebuggingSection): List<Int> {
        val offsets = ArrayList<Int>()

        for (string in set) {
            offsets.add(section.offset)
            section.writer.writeBytes(string.toByteArray())
            section.writer.writeByte(0)
        }

        return offsets
    }

    @JvmInline value class StringRef(val index: Int)
}

