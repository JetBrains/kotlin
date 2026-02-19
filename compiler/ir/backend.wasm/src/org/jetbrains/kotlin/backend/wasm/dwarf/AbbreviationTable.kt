/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.utils.DebugEntityTable

@JvmInline
value class AbbreviationRef(val index: Int)

class AbbreviationTable : DebugEntityTable<Abbreviation, AbbreviationRef>() {
    override fun computeId(index: Int) = AbbreviationRef(index + 1)

    fun write(section: DebuggingSection.DebugAbbreviations) {
        for ((code, abbreviation) in withIndex()) {
            section.writer.writeVarUInt32(code.toUInt() + 1u)
            abbreviation.write(section)
        }
        section.writer.writeUByte(0u)
    }
}