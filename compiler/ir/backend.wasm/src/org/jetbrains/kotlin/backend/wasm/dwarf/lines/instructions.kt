/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.lines

import org.jetbrains.kotlin.backend.wasm.dwarf.DebuggingSection
import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter

sealed class LineInstruction(val operation: DwLines) {
    protected abstract fun writeArguments(writer: ByteWriter)

    fun writeTo(writer: ByteWriter) {
        writer.writeUByte(operation.opcode.toUByte())
        writeArguments(writer)
    }

    data object Copy : LineInstruction(DwLines.COPY) {
        override fun writeArguments(writer: ByteWriter) {
            // Empty Arguments
        }
    }

    class AdvancePC(val diff: Int) : LineInstruction(DwLines.ADVANCE_PC) {
        override fun writeArguments(writer: ByteWriter) {
            writer.writeVarUInt32(diff.toUInt())
        }
    }

    class AdvanceLine(val diff: Int) : LineInstruction(DwLines.ADVANCE_LINE) {
        override fun writeArguments(writer: ByteWriter) {
            writer.writeVarInt32(diff)
        }
    }

    class SetFile(val file: FileTable.FileId) : LineInstruction(DwLines.SET_FILE) {
        override fun writeArguments(writer: ByteWriter) {
            writer.writeVarUInt32(file.index.toUInt())
        }
    }

    class SetColumn(val column: Int) : LineInstruction(DwLines.SET_COLUMN) {
        override fun writeArguments(writer: ByteWriter) {
            writer.writeVarUInt32(column.toUInt())
        }
    }
}