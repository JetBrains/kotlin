/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.google.common.primitives.Longs.min
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

/**
 * A 'sliding window mapped buffer' allowing efficient mapping of large files beyond the 2GB ByteBuffer limit.
 *
 * @property dataSize The max size of the data to be mapped, usually the file size.
 * @property mapBuffer A callback to create a [MappedByteBuffer] given an offset and size.
 * @property unmapBuffer A callback to unmap given [MappedByteBuffer].
 * @property defaultByteOrder default byte order for multibyte data extraction
 */
internal class LargeDynamicMappedBuffer(
    private val dataSize: Long,
    private val mapBuffer: (Long, Long) -> MappedByteBuffer, // (offset, size) -> MappedByteBuffer
    private val unmapBuffer: MappedByteBuffer.() -> Unit,
    private val defaultByteOrder: ByteOrder = ByteOrder.nativeOrder(),
) {

    private var currentMappedBuffer: MappedByteBuffer? = null
    private var currentStart: Long = 0L
    private var currentEnd: Long = 0L

    @Synchronized
    fun <R> withMappedRange(start: Long, end: Long, body: Mapping.() -> R): R {
        require(end in (start + 1)..dataSize && end - start <= Int.MAX_VALUE)
        var currentSize = Int.MAX_VALUE.toLong()
        if (currentMappedBuffer == null || currentStart > start || currentEnd < end) {
            if (dataSize <= Int.MAX_VALUE) {
                currentStart = 0L
                currentEnd = dataSize
                currentSize = dataSize
            } else if (start + Int.MAX_VALUE > dataSize) {
                currentStart = dataSize - Int.MAX_VALUE
                currentEnd = dataSize
            } else {
                currentStart = start
                currentEnd = start + Int.MAX_VALUE
            }
            unmap()
            currentMappedBuffer = mapBuffer(currentStart, currentSize).also {
                it.order(defaultByteOrder)
            }
        }
        val buffer = currentMappedBuffer!!
        require(currentStart <= start && currentEnd >= end && start - currentStart < Int.MAX_VALUE)
        buffer.position((start - currentStart).toInt())
        return Mapping(buffer, buffer.position()).body()
    }

    fun <R> withMappedTail(body: Mapping.() -> R): R {
        val size = min(dataSize, Int.MAX_VALUE.toLong())
        return withMappedRange(dataSize - size, dataSize, body)
    }

    fun <R> withMappedRangeFrom(start: Long, body: Mapping.() -> R): R {
        require(start < dataSize)
        val size = min(dataSize - start, Int.MAX_VALUE.toLong())
        return withMappedRange(start, start + size, body)
    }

    fun unmap() {
        currentMappedBuffer?.unmapBuffer()
    }

    class Mapping(private val buffer: MappedByteBuffer, private val baseOffset: Int) {

        fun order(bo: ByteOrder) {
            buffer.order(bo)
        }

        fun getInt(offset: Int) = buffer.getInt(baseOffset + offset)
        fun getLong(offset: Int) = buffer.getLong(baseOffset + offset)
        fun getShort(offset: Int) = buffer.getShort(baseOffset + offset)

        fun getBytes(offset: Int, length: Int): ByteArray {
            val bytes = ByteArray(length)
            buffer.position(baseOffset + offset)
            try {
                buffer.get(bytes, 0, length)
                return bytes
            } finally {
                buffer.position(baseOffset)
            }
        }

        fun endOffset() = buffer.capacity() - baseOffset
    }
}