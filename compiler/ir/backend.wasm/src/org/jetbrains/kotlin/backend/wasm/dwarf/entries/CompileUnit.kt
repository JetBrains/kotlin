/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.backend.wasm.dwarf.DW_AT
import org.jetbrains.kotlin.backend.wasm.dwarf.DW_TAG
import org.jetbrains.kotlin.backend.wasm.dwarf.DwAttribute
import org.jetbrains.kotlin.backend.wasm.dwarf.DwTag

@DW_TAG(DwTag.COMPILE_UNIT)
data class CompileUnit(
    @DW_AT(DwAttribute.LOW_PC)
    val lowProgramCounter: Int,

    @DW_AT(DwAttribute.HIGH_PC)
    var highProgramCounter: Int,

    @DW_AT(DwAttribute.NAME)
    val name: String,
) : DebugInformationEntry {
    @DW_AT(DwAttribute.LANGUAGE)
    val language = 0x0026 // DW_LANG_Kotlin - https://dwarfstd.org/languages.html

    @DW_AT(DwAttribute.PRODUCER)
    val producer = "Kotlin/Wasm Compiler"

    val children = mutableListOf<DebugInformationEntry>()
}