/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * An implementation of [KlibComponentWriter] that writes IR to the constructed Klib library.
 */
internal sealed class KlibIrComponentWriterImpl : KlibComponentWriter {
    class ForMainIr(private val irFiles: Collection<SerializedIrFile>) : KlibIrComponentWriterImpl() {
        override fun writeTo(root: KlibFile) {
            writeIrFiles(
                irFiles = irFiles,
                layout = KlibIrComponentLayout.createForMainIr(root)
            )
        }
    }

    class ForInlinableFunctionsIr(private val inlinableFunctionsFile: SerializedIrFile) : KlibIrComponentWriterImpl() {
        override fun writeTo(root: KlibFile) {
            writeIrFiles(
                irFiles = listOf(inlinableFunctionsFile),
                layout = KlibIrComponentLayout.createForInlinableFunctionsIr(root)
            )
        }
    }

    protected fun writeIrFiles(irFiles: Collection<SerializedIrFile>, layout: KlibIrComponentLayout) {
        layout.irDir.mkdirs()

        with(irFiles.sortedBy { it.path }) {
            serializeNonNullableEntities(SerializedIrFile::fileData, layout::irFilesFile)
            serializeNullableEntries(SerializedIrFile::fileEntries, layout::irFileEntriesFile)
            serializeNonNullableEntities(SerializedIrFile::declarations, layout::declarationsFile)
            serializeNonNullableEntities(SerializedIrFile::bodies, layout::bodiesFile)
            serializeNonNullableEntities(SerializedIrFile::types, layout::typesFile)
            serializeNonNullableEntities(SerializedIrFile::signatures, layout::signaturesFile)
            serializeNullableEntries(SerializedIrFile::debugInfo, layout::signaturesDebugInfoFile)
            serializeNonNullableEntities(SerializedIrFile::strings, layout::stringLiteralsFile)
        }
    }

    private inline fun List<SerializedIrFile>.serializeNonNullableEntities(
        accessor: (SerializedIrFile) -> ByteArray,
        destination: () -> KlibFile,
    ): Unit = IrArrayWriter(map { accessor(it) }).writeIntoFile(destination().absolutePath)

    private inline fun List<SerializedIrFile>.serializeNullableEntries(
        accessor: (SerializedIrFile) -> ByteArray?,
        destination: () -> KlibFile,
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
