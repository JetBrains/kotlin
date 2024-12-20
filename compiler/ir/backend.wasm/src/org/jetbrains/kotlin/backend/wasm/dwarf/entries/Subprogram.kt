/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.backend.wasm.dwarf.*
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileId
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

data class Subprogram(
    val name: DebugStringTable.StringRef,
    val file: FileId,
    val startGeneratedLocation: SourceLocationMapping
) : DebuggingInformationEntry {
    lateinit var endGeneratedLocation: SourceLocationMapping
    private val sourceLocation = startGeneratedLocation.sourceLocation as SourceLocation.DefinedLocation

    val isPublic = true
    val line by lazy { sourceLocation.line + 1 }
    val column by lazy { sourceLocation.column }
    val lowProgramCounter by lazy { startGeneratedLocation.generatedLocationRelativeToCodeSection.column }
    val highProgramCounter by lazy { endGeneratedLocation.generatedLocationRelativeToCodeSection.column }


    fun write(
        encoding: Dwarf.Encoding,
        subprogramAbbreviation: AbbreviationRef,
        section: DebuggingSection.DebugInfo,
        stringOffsets: List<Int>
    ) {
        require(encoding.format == Dwarf.Format.DWARF_32) { "Unsupported format: ${encoding.format}" }
        require(encoding.version == 5) { "Unsupported DWARF version: ${encoding.version}" }

        with(section.writer) {
            writeVarUInt32(subprogramAbbreviation.index.toUInt())
            writeUInt64(stringOffsets[name.index - 1].toULong(), encoding.format.wordSize)
            writeUByte(if (isPublic) 1u else 0u)
            writeVarUInt32(file.index.toUInt())
            writeVarUInt32(line.toUInt())
            writeVarUInt32(column.toUInt())
            writeUInt64(lowProgramCounter.toULong(), encoding.format.wordSize)
            writeUInt64(highProgramCounter.toULong(), encoding.format.wordSize)
        }
    }

    companion object {
        val abbreviation = Abbreviation(
            tag = DwTag.SUBPROGRAM,
            hasChildren = false,
            attributes = listOf(
                DwAttribute.NAME by DwForm.STRP,
                DwAttribute.EXTERNAL by DwForm.FLAG,
                DwAttribute.DECL_FILE by DwForm.UDATA,
                DwAttribute.DECL_LINE by DwForm.UDATA,
                DwAttribute.DECL_COLUMN by DwForm.UDATA,
                DwAttribute.LOW_PC by DwForm.ADDR,
                DwAttribute.HIGH_PC by DwForm.ADDR,
            )
        )
    }
}
