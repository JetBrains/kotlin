/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

@DW_TAG(DwTag.SUBPROGRAM)
data class SubprogramEntry(
    @DW_AT(DwAttribute.NAME)
    val name: String,

    @DW_AT(DwAttribute.EXTERNAL)
    val isPublic: Boolean,

    val startGeneratedLocation: SourceLocationMapping
) : DebuggingInformationEntry {
    lateinit var endGeneratedLocation: SourceLocationMapping

    private val sourceLocation = startGeneratedLocation.sourceLocation as SourceLocation.Location

    @DW_AT(DwAttribute.LOW_PC)
    val lowProgramCounter: Int by lazy { startGeneratedLocation.generatedLocation.column }

    @DW_AT(DwAttribute.HIGH_PC)
    val highProgramCounter: Int by lazy { endGeneratedLocation.generatedLocation.column }

    @DW_AT(DwAttribute.DECL_FILE)
    val file: Int = 0 // by lazy { sourceLocation.file }

    @DW_AT(DwAttribute.DECL_LINE)
    val line: Int by lazy { sourceLocation.line }

    @DW_AT(DwAttribute.DECL_COLUMN)
    val column: Int by lazy { sourceLocation.column }
}
