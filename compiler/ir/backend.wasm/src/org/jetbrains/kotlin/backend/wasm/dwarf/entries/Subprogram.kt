/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.backend.wasm.dwarf.DW_AT
import org.jetbrains.kotlin.backend.wasm.dwarf.DW_TAG
import org.jetbrains.kotlin.backend.wasm.dwarf.DwAttribute
import org.jetbrains.kotlin.backend.wasm.dwarf.DwTag

@DW_TAG(DwTag.SUBPROGRAM)
data class Subprogram(
    @DW_AT(DwAttribute.LOW_PC)
    val lowProgramCounter: Int,

    @DW_AT(DwAttribute.HIGH_PC)
    var highProgramCounter: Int,

    @DW_AT(DwAttribute.NAME)
    val name: String,

    @DW_AT(DwAttribute.EXTERNAL)
    val isPublic: Boolean,

    @DW_AT(DwAttribute.DECL_FILE)
    val file: Int,

    @DW_AT(DwAttribute.DECL_LINE)
    val line: Int,

    @DW_AT(DwAttribute.DECL_COLUMN)
    val column: Int,
)
