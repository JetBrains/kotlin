/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.DirectoryTable
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.DwLinesHeader
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileId
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileInfo
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileTable
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.LineInstruction

private const val OPCODE_BASE: UByte = 13u

class LineProgram(private val encoding: Dwarf.Encoding, private val lineEncoding: LineEncoding = LineEncoding.Default) {
    private val files = FileTable()
    private val directories = DirectoryTable()

    private var previousRow = DEFAULT_ROW
    private val instructions = mutableListOf<LineInstruction>()

    data class LineRow(
        val file: FileId,
        val addressOffset: Int,
        val line: Int,
        val column: Int,
    )

    fun addFile(file: DebugLinesStringTable.StringRef, directory: DebugLinesStringTable.StringRef): FileId {
        val dirIndex = directories.add(directory)
        return files.add(FileInfo(file, dirIndex))
    }

    fun startFunction(row: LineRow) {
        instructions.push(LineInstruction.SetAddress(row.addressOffset, encoding))
        previousRow = DEFAULT_ROW.copy(addressOffset = row.addressOffset)
        add(row)
        instructions[instructions.lastIndex] = LineInstruction.SetPrologueEnd
    }

    fun addEmptyMapping(addressOffset: Int) {
        val lastInstructionIndex = instructions.lastIndex
        if (lastInstructionIndex < 0 || instructions[lastInstructionIndex] != LineInstruction.SetPrologueEnd) return

        val offsetAdvance = addressOffset - previousRow.addressOffset
        if (offsetAdvance != 0) {
            instructions[lastInstructionIndex] = LineInstruction.Copy
            instructions.push(LineInstruction.AdvancePC(offsetAdvance))
            instructions.push(LineInstruction.SetPrologueEnd)
            instructions.push(LineInstruction.Copy)
            previousRow = previousRow.copy(addressOffset = addressOffset)
        }
    }

    fun endFunction(row: LineRow) {
        add(row)
        instructions[instructions.lastIndex] = LineInstruction.EndSequence
        previousRow = DEFAULT_ROW
    }

    fun add(row: LineRow) {
        if (row.file != previousRow.file) {
            instructions.push(LineInstruction.SetFile(row.file))
        }

        if (row.column != previousRow.column) {
            instructions.push(LineInstruction.SetColumn(row.column))
        }

        val lineAdvance = row.line - previousRow.line

        if (lineAdvance != 0) {
            instructions.push(LineInstruction.AdvanceLine(lineAdvance))
        }

        val offsetAdvance = row.addressOffset - previousRow.addressOffset
        if (offsetAdvance != 0) {
            instructions.push(LineInstruction.AdvancePC(offsetAdvance))
        }

        instructions.push(LineInstruction.Copy)
        previousRow = row
    }

    fun write(section: DebuggingSection.DebugLines, stringOffsets: List<Int>) {
        require(encoding.format == Dwarf.Format.DWARF_32) { "Unsupported format: ${encoding.format}" }
        require(encoding.version == 5) { "Unsupported DWARF version: ${encoding.version}" }

        val sectionEpilogue = section.writer.createTemp().apply {
            writeUInt16(encoding.version.toUShort())
            writeUByte(encoding.addressSize.toUByte())
            // Segment selector size.
            writeByte(0)
        }

        val header = section.writer.createTemp().apply {
            writeUByte(lineEncoding.minimumInstructionLength)
            writeUByte(lineEncoding.maximumOperandsPerInstruction)
            writeBoolean(lineEncoding.defaultIsStatement)
            writeByte(lineEncoding.lineBase)
            writeUByte(lineEncoding.lineRange)
            writeUByte(OPCODE_BASE)
            writeBytes(byteArrayOf(0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1))

            // Directory entry formats (only ever 1).
            writeUByte(1u)
            writeVarUInt32(DwLinesHeader.PATH.opcode)
            writeVarUInt32(DwForm.LINE_STRP.opcode)

            // Directory entries
            writeVarUInt32(directories.size.toUInt())
            for (dir in directories) {
                writeUInt64(stringOffsets[dir.index - 1].toULong(), encoding.format.wordSize)
            }

            // File name entry formats (only ever 3)
            writeUByte(2u)
            writeVarUInt32(DwLinesHeader.PATH.opcode)
            writeVarUInt32(DwForm.LINE_STRP.opcode)
            writeVarUInt32(DwLinesHeader.DIRECTORY_INDEX.opcode)
            writeVarUInt32(DwForm.UDATA.opcode)

            writeVarUInt32(files.size.toUInt())
            for (file in files) {
                writeUInt64(stringOffsets[file.path.index - 1].toULong(), encoding.format.wordSize)
                writeVarUInt32(file.directory.index.toUInt())
            }
        }

        val wholeSectionWithoutLength = section.writer.createTemp().apply {
            writeUInt64(header.written.toULong(), encoding.format.wordSize)
            write(header)

            for (instruction in instructions) {
                instruction.writeTo(this)
            }
        }

        with(section.writer) {
            writeUInt64((wholeSectionWithoutLength.written + sectionEpilogue.written).toULong(), encoding.format.wordSize)
            write(sectionEpilogue)
            write(wholeSectionWithoutLength)
        }
    }

    interface LineEncoding {
        val lineBase: Byte
        val lineRange: UByte
        val minimumInstructionLength: UByte
        val maximumOperandsPerInstruction: UByte
        val defaultIsStatement: Boolean

        // Values from LLVM.
        object Default : LineEncoding {
            override val lineBase: Byte = -5
            override val lineRange: UByte = 14u
            override val minimumInstructionLength: UByte = 1u
            override val maximumOperandsPerInstruction: UByte = 1u
            override val defaultIsStatement: Boolean = true
        }
    }

    private companion object {
        private val DEFAULT_ROW = LineRow(
            file = FileId(-1),
            addressOffset = 0,
            line = 0,
            column = 0,
        )
    }
}