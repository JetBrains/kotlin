/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

@DW_TAG(DwTag.COMPILE_UNIT)
data class CompileUnit(
    @DW_AT(DwAttribute.NAME)
    val name: String,
) : DebuggingInformationEntry {
    @DW_AT(DwAttribute.LANGUAGE)
    val language = 0x0026 // DW_LANG_Kotlin - https://dwarfstd.org/languages.html

    @DW_AT(DwAttribute.PRODUCER)
    val producer = "Kotlin/Wasm Compiler"

    @DW_AT(DwAttribute.LOW_PC)
    val lowProgramCounter: Int = 0

    @DW_AT(DwAttribute.HIGH_PC)
    val highProgramCounter: Int by lazy {
        children.lastOrNull()?.highProgramCounter ?: 0
    }

    val children = mutableListOf<SubprogramEntry>()
}