/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.backend.wasm.dwarf.*
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileTable
import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

data class Subprogram(
    val name: StringTable.StringRef,
    val file: FileTable.FileId,
    val startGeneratedLocation: SourceLocationMapping
) : DebuggingInformationEntry {
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

    lateinit var endGeneratedLocation: SourceLocationMapping
    private val sourceLocation = startGeneratedLocation.sourceLocation as SourceLocation.Location

    val isPublic = true
    val line by lazy { sourceLocation.line + 1 }
    val column by lazy { sourceLocation.column }
    val lowProgramCounter by lazy { startGeneratedLocation.generatedLocation.column }
    val highProgramCounter by lazy { endGeneratedLocation.generatedLocation.column }


    fun write(
        encoding: Dwarf.Encoding,
        abbreviation: AbbreviationTable.AbbreviationRef,
        writer: ByteWriter,
        stringOffsets: List<Int>
    ) {
        require(encoding.format == Dwarf.Format.DWARF_32) { "Unsupported format: ${encoding.format}" }
        require(encoding.version == 5) { "Unsupported DWARF version: ${encoding.version}" }


        with(writer) {
            writeVarUInt32(abbreviation.index.toUInt())
            writeUInt64(stringOffsets[name.index - 1].toULong(), encoding.format.wordSize)
            writeUByte(if (isPublic) 1u else 0u)
            writeVarUInt32(file.index.toUInt())
            writeVarUInt32(line.toUInt())
            writeVarUInt32(column.toUInt())
            writeUInt64(lowProgramCounter.toULong(), encoding.format.wordSize)
            writeUInt64(highProgramCounter.toULong(), encoding.format.wordSize)
        }
    }
}
