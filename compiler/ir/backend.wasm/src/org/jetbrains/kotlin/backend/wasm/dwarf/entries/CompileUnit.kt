/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

import org.jetbrains.kotlin.backend.wasm.dwarf.*

class CompileUnit(
    val name: StringTable.StringRef,
    val producer: StringTable.StringRef,
    val compileDirectory: StringTable.StringRef,
    val encoding: Dwarf.Encoding
) : DebuggingInformationEntry {
    companion object {
        val abbreviation = Abbreviation(
            tag = DwTag.COMPILE_UNIT,
            hasChildren = true,
            attributes = listOf(
                DwAttribute.NAME by DwForm.STRP,
                DwAttribute.LANGUAGE by DwForm.UDATA,
                DwAttribute.PRODUCER by DwForm.STRP,
                DwAttribute.COMP_DIR by DwForm.STRP,
                DwAttribute.LOW_PC by DwForm.ADDR,
                DwAttribute.HIGH_PC by DwForm.ADDR,
                DwAttribute.STMT_LIST by DwForm.SEC_OFFSET,
            )
        )
    }

    val language = 0x001d // DW_LANG_Kotlin - https://dwarfstd.org/languages.html
    val lowProgramCounter: Int = 0
    val highProgramCounter: Int by lazy {
        children.lastOrNull()?.highProgramCounter ?: 0
    }
    val stmtList: Int = 0
    val children = mutableListOf<Subprogram>()

    fun write(
        compileUnitAbbreviation: AbbreviationTable.AbbreviationRef,
        subprogramAbbreviation: AbbreviationTable.AbbreviationRef,
        section: DebuggingSection.DebugInfo,
        stringOffsets: List<Int>
    ) {
        require(encoding.format == Dwarf.Format.DWARF_32) { "Unsupported format: ${encoding.format}" }
        require(encoding.version == 5) { "Unsupported DWARF version: ${encoding.version}" }

        val encodedUnit = section.writer.createTemp().apply {
            writeUInt16(encoding.version.toUShort())
            writeUByte(UnitHeader.COMPILE.opcode)
            writeUByte(encoding.addressSize.toUByte())

            // Abbreviation offset (always 0 in our case)
            writeUInt64(0u, encoding.format.wordSize)
            writeVarUInt32(compileUnitAbbreviation.index.toUInt())

            writeUInt64(stringOffsets[name.index - 1].toULong(), encoding.format.wordSize)
            writeVarUInt32(language.toUInt())
            writeUInt64(stringOffsets[producer.index - 1].toULong(), encoding.format.wordSize)
            writeUInt64(stringOffsets[compileDirectory.index - 1].toULong(), encoding.format.wordSize)
            writeUInt64(lowProgramCounter.toULong(), encoding.format.wordSize)
            writeUInt64(highProgramCounter.toULong(), encoding.format.wordSize)
            writeUInt64(stmtList.toULong(), encoding.format.wordSize)

            if (children.isNotEmpty()) {
                for (child in children) {
                    child.write(encoding, subprogramAbbreviation, this, stringOffsets)
                }
                writeUByte(0u)
            }
        }

        with(section.writer) {
            writeUInt64(encodedUnit.written.toULong(), encoding.format.wordSize)
            write(encodedUnit)
        }
    }
}