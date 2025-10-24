/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * On the contrary to [KlibIrComponent], which provides read access to IR,
 * [KlibIrWriterImpl] provides allows writing the IR to the file system.
 *
 * TODO (KT-81411): This class is an implementation detail. It should be made internal after dropping `KonanLibraryImpl`.
 */
sealed class KlibIrWriterImpl() {
    class ForMainIr(override val layout: KlibIrComponentLayout) : KlibIrWriterImpl() {
        fun writeMainIr(irFiles: Collection<SerializedIrFile>) = writeIrFiles(irFiles)
    }

    class ForInlinableFunctionsIr(override val layout: KlibIrComponentLayout) : KlibIrWriterImpl() {
        fun writeInlinableFunctionsIr(inlinableFunctionsFile: SerializedIrFile) = writeIrFiles(listOf(inlinableFunctionsFile))
    }

    protected abstract val layout: KlibIrComponentLayout

    protected fun writeIrFiles(irFiles: Collection<SerializedIrFile>) {
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
