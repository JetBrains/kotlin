/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.IrKotlinLibraryLayout
import org.jetbrains.kotlin.library.IrWriter
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.konan.file.File as KFile

class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
    override fun addIr(ir: SerializedIrModule) {
        irLayout.irDir.mkdirs()

        with(ir.files.sortedBy { it.path }) {
            serializeNonNullableEntities(SerializedIrFile::fileData, irLayout::irFiles)
            serializeNonNullableEntities(SerializedIrFile::declarations, irLayout::irDeclarations)
            serializeNonNullableEntities(SerializedIrFile::types, irLayout::irTypes)
            serializeNonNullableEntities(SerializedIrFile::signatures, irLayout::irSignatures)
            serializeNonNullableEntities(SerializedIrFile::strings, irLayout::irStrings)
            serializeNonNullableEntities(SerializedIrFile::bodies, irLayout::irBodies)
            serializeNullableEntries(SerializedIrFile::debugInfo, irLayout::irDebugInfo)
            serializeNullableEntries(SerializedIrFile::fileEntries, irLayout::irFileEntries)
        }
    }

    private inline fun List<SerializedIrFile>.serializeNonNullableEntities(
        accessor: (SerializedIrFile) -> ByteArray,
        destination: () -> KFile,
    ): Unit = IrArrayWriter(map { accessor(it) }).writeIntoFile(destination().absolutePath)

    private inline fun List<SerializedIrFile>.serializeNullableEntries(
        accessor: (SerializedIrFile) -> ByteArray?,
        destination: () -> KFile,
    ) {
        val nonNullEntries: List<ByteArray> = mapNotNull(accessor)
        if (nonNullEntries.isEmpty()) {
            // No entries -> nothing to write to `destination`.
            return
        }

        // The number of entries should be strictly the same as the number of serialized IR files.
        // Otherwise, the resulting byte table will be incorrectly read during deserialization.
        check(nonNullEntries.size == size) {
            "Error while writing IR to ${destination()}:" +
                    "\nOnly ${nonNullEntries.size} out of $size serialized IR files have non-nullable values."
        }

        IrArrayWriter(nonNullEntries).writeIntoFile(destination().absolutePath)
    }
}
