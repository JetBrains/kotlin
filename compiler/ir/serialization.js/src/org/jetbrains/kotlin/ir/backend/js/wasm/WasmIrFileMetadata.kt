/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayReader
import org.jetbrains.kotlin.library.impl.IrArrayWriter
import org.jetbrains.kotlin.library.impl.IrStringWriter
import org.jetbrains.kotlin.library.impl.toArray

class WasmIrFileMetadata(
    val exportNames: Map<ExportKind, List<Pair<String, String>>>,
) : IrFileSerializer.FileBackendSpecificMetadata {

    override fun toByteArray(): ByteArray {
        val allExportBytes = ExportKind.entries.flatMap { kind ->
            val entries = exportNames[kind].orEmpty()
            listOf(
                IrStringWriter(entries.map { it.first }, false).writeIntoMemory(),  // export names
                IrStringWriter(entries.map { it.second }, false).writeIntoMemory(), // declaration names
            )
        }
        return IrArrayWriter(allExportBytes, false).writeIntoMemory()
    }

    companion object {
        fun fromByteArray(data: ByteArray): WasmIrFileMetadata {

            val reader = IrArrayReader(data)
            val exportArrays = reader.toArray()

            val exportNames = ExportKind.entries.associateWith { kind ->
                val base = kind.ordinal * 2
                val exportNames = IrArrayReader(exportArrays[base]).toArray().map(WobblyTF8::decode)
                val declNames = IrArrayReader(exportArrays[base + 1]).toArray().map(WobblyTF8::decode)
                exportNames.zip(declNames)
            }

            return WasmIrFileMetadata(exportNames)
        }
    }
}
