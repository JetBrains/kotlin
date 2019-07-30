/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import java.nio.channels.FileChannel

class SimpleIrTableFileReader(file: File) {
    private val buffer = file.map(FileChannel.MapMode.READ_ONLY)
    private val indexToOffset: IntArray

    init {
        val count = buffer.int
        indexToOffset = IntArray(count + 1)
        indexToOffset[0] = 4 * (count + 1)
        for (i in 0 until count) {
            val size = buffer.int
            indexToOffset[i + 1] = indexToOffset[i] + size
        }
    }

    fun tableItemBytes(id: Int): ByteArray {
        val offset = indexToOffset[id]
        val size = indexToOffset[id + 1] - offset
        val result = ByteArray(size)
        buffer.position(offset)
        buffer.get(result, 0, size)
        return result
    }
}


data class DeclarationId(val id: Long, val isLocal: Boolean)

class CombinedIrFileReader(file: File) {
    private val buffer = file.map(FileChannel.MapMode.READ_ONLY)
    private val declarationToOffsetSize = mutableMapOf<DeclarationId, Pair<Int, Int>>()

    init {
        val declarationsCount = buffer.int
        for (i in 0 until declarationsCount) {
            val id = buffer.long
            val isLocal = buffer.int != 0
            val offset = buffer.int
            val size = buffer.int
            declarationToOffsetSize[DeclarationId(id, isLocal)] = offset to size
        }
    }

    fun declarationBytes(id: DeclarationId): ByteArray {
        val offsetSize = declarationToOffsetSize[id] ?: throw Error("No declaration with $id here")
        val result = ByteArray(offsetSize.second)
        buffer.position(offsetSize.first)
        buffer.get(result, 0, offsetSize.second)
        return result
    }
}