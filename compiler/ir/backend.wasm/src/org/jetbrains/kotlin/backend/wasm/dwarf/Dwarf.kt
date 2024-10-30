/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

// The API is inspired by https://github.com/gimli-rs/gimli
class Dwarf(encoding: Encoding = Encoding.Default) {
    val strings = StringTable()
    val lines = LineProgram(encoding)
//    val units = UnitTable()

    fun generate(): DebuggingSections =
        DebuggingSections().apply {
            val stringOffsets = strings.write(debugStrings)
            lines.write(debugLines, stringOffsets)
        }

    enum class Format(val wordSize: Int) {
        DWARF_32(4),
        DWARF_64(8),
    }

    interface Encoding {
        val addressSize: Int
        val format: Format
        val version: Int

        object Default : Encoding {
            override val addressSize = 4
            override val format: Format = Format.DWARF_32
            override val version = 5 // https://dwarfstd.org/doc/DWARF5.pdf
        }
    }

}