/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.entries.CompileUnit
import org.jetbrains.kotlin.backend.wasm.dwarf.entries.Subprogram

// The API is inspired a lot by https://github.com/gimli-rs/gimli
class Dwarf(encoding: Encoding = Encoding.Default) {
    val strings = DebugStringTable()
    val lineStrings = DebugLinesStringTable()
    val lines = LineProgram(encoding)
    val abbreviations = AbbreviationTable()
    val mainCompileUnit = CompileUnit(
        strings.add("main"),
        strings.add("Kotlin/Wasm Compiler"),
        strings.add("."),
        encoding
    )

    fun generate(): DebuggingSections =
        DebuggingSections().apply {
            val compileUnitAbbreviation = abbreviations.add(CompileUnit.abbreviation)
            val subprogramAbbreviation = abbreviations.add(Subprogram.abbreviation)

            val stringOffsets = strings.write(debugStrings)
            val debugLinesStringOffsets = lineStrings.write(debugLinesStrings)

            lines.write(debugLines, debugLinesStringOffsets)
            mainCompileUnit.write(compileUnitAbbreviation, subprogramAbbreviation, debugInfo, stringOffsets)
            abbreviations.write(debugAbbreviations)
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