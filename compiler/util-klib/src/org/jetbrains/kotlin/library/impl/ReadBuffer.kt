/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import java.io.File
import java.lang.ref.SoftReference
import java.nio.ByteBuffer

sealed class ReadBuffer {

    abstract val size: Int
    abstract fun get(result: ByteArray, offset: Int, length: Int)
    abstract var position: Int


    abstract val int: Int
    abstract val long: Long

    abstract class NIOReader(private val buffer: ByteBuffer) : ReadBuffer() {

        override val size: Int
            get() = buffer.limit()

        override fun get(result: ByteArray, offset: Int, length: Int) {
            buffer.get(result, offset, length)
        }

        override var position: Int
            get() = buffer.position()
            set(value) { buffer.position(value) }

        override val int: Int
            get() = buffer.int

        override val long: Long
            get() = buffer.long
    }

    class MemoryBuffer(bytes: ByteArray) : NIOReader(bytes.buffer)

    class DirectFileBuffer(file: File) : NIOReader(file.readBytes().buffer)

    class WeakFileBuffer(private val file: File) : ReadBuffer() {
        override val size: Int
            get() = file.length().toInt()

        override fun get(result: ByteArray, offset: Int, length: Int) {
            val buf = ensureBuffer()
            pos += length
            buf.get(result, offset, length)
        }

        override val int: Int
            get(): Int {
                val buf = ensureBuffer()
                pos += Int.SIZE_BYTES
                return buf.int
            }

        override val long: Long
            get(): Long {
                val buf = ensureBuffer()
                pos += Long.SIZE_BYTES
                return buf.long
            }

        private var pos: Int = 0

        override var position: Int
            get() = pos.also { assert(it == ensureBuffer().position()) }
            set(value) {
                val buf = ensureBuffer()
                pos = value
                buf.position(value)
            }

        private fun ensureBuffer(): ByteBuffer {
            var tmpBuffer = weakBuffer.get()
            if (tmpBuffer == null) {
                tmpBuffer = file.readBytes().buffer
                println(
                    """
                    FILE: ${file.absolutePath}
                    TMP buffer position: ${tmpBuffer.position()}
                    TMP buffer limit: ${tmpBuffer.limit()}
                    TMP buffer capacity: ${tmpBuffer.capacity()}
                """.trimIndent()
                )
                tmpBuffer.position(pos)
                weakBuffer = SoftReference(tmpBuffer)
            }
            return tmpBuffer
        }

        private var weakBuffer: SoftReference<ByteBuffer> = SoftReference(null)
    }
}