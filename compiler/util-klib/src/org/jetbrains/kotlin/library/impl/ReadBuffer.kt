/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import java.lang.ref.SoftReference
import java.nio.ByteBuffer

/**
 * A buffer that allows effectively reading data from an underlying byte buffer.
 * See inheritors for implementation details.
 */
sealed interface ReadBuffer {

    val size: Int
    fun get(result: ByteArray, offset: Int, length: Int)
    var position: Int

    val int: Int
    val long: Long

    /**
     * Allows reading data directly from the byte array [bytes].
     * The byte array is known at the time of [MemoryBuffer] creation.
     */
    class MemoryBuffer(bytes: ByteArray) : ReadBuffer {
        private val buffer: ByteBuffer = bytes.buffer

        override val size: Int
            get() = buffer.limit()

        override fun get(result: ByteArray, offset: Int, length: Int) {
            buffer.get(result, offset, length)
        }

        override var position: Int
            get() = buffer.position()
            set(value) {
                buffer.position(value)
            }

        override val int: Int
            get() = buffer.int

        override val long: Long
            get() = buffer.long
    }

    /**
     * Allows reading data from the byte array that is obtained though [loadBytes].
     *
     * The byte array may not be known at the time of [OnDemandMemoryBuffer] creation.
     * It will be created later, on the first access to any of the declared [OnDemandMemoryBuffer] members.
     * The byte array is cached using [SoftReference], which means that it can be potentially
     * released (garbage collected) in case of JVM heap memory deficit.
     *
     * This implementation allows having lesser memory consumption than [MemoryBuffer] in case
     * of occasional reads from [ReadBuffer].
     */
    class OnDemandMemoryBuffer(private val loadBytes: () -> ByteArray) : ReadBuffer {
        private var bufferRef: SoftReference<ByteBuffer> = SoftReference(null)
        private var pos: Int = 0

        override val size: Int
            get() = ensureBuffer().limit()

        override fun get(result: ByteArray, offset: Int, length: Int) {
            val buf = ensureBuffer()
            pos += length
            buf.get(result, offset, length)
        }

        override var position: Int
            get() = pos.also { assert(it == ensureBuffer().position()) }
            set(value) {
                val buf = ensureBuffer()
                pos = value
                buf.position(value)
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

        private fun ensureBuffer(): ByteBuffer {
            var tmpBuffer = bufferRef.get()
            if (tmpBuffer == null) {
                tmpBuffer = loadBytes().buffer
                tmpBuffer.position(pos)
                bufferRef = SoftReference(tmpBuffer)
            }
            return tmpBuffer
        }
    }
}