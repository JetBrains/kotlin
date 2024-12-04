/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.AbstractIrFileEntry
import org.jetbrains.kotlin.ir.IrFileEntry
import java.io.File

internal val IrFileEntry.lineStartOffsetsForSerialization: Iterable<Int>
    get() = when (this) {
        is AbstractIrFileEntry -> this.getLineStartOffsetsForSerialization()
        else -> File(name).directlyReadLineStartOffsets()
    }

private fun File.directlyReadLineStartOffsets(): List<Int> {
    if (!isFile) return emptyList()

    // TODO: could be incorrect, if file is not in system's line terminator format.
    // Maybe use (0..document.lineCount - 1)
    //                .map { document.getLineStartOffset(it) }
    //                .toIntArray()
    // as in PSI.
    val separatorLength = System.lineSeparator().length
    val buffer = ArrayList<Int>()
    var currentOffset = 0
    this.forEachLine { line ->
        buffer.add(currentOffset)
        currentOffset += line.length + separatorLength
    }
    buffer.add(currentOffset)
    return buffer
}
