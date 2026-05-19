/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import java.lang.ref.SoftReference
import java.nio.ByteBuffer

sealed class ReadByteBufferProvider {
    private var lastPosition: Int = 0
    private var isUsingBuffer = false

    fun <T> use(block: (ByteBuffer) -> T): T {
        require(!isUsingBuffer) { "use {} cannot be called recursively." }
        try {
            isUsingBuffer = true

            val tmpBuffer = ensureBuffer()
            tmpBuffer.position(lastPosition)
            val result = block(tmpBuffer)
            lastPosition = tmpBuffer.position()
            return result
        } finally {
            isUsingBuffer = false
        }
    }

    protected abstract fun ensureBuffer(): ByteBuffer

    /**
     * Allows reading data directly from the byte array [bytes].
     * The byte array is known at the time of [MemoryBuffer] creation.
     */
    class MemoryBuffer(bytes: ByteArray) : ReadByteBufferProvider() {
        private val buffer: ByteBuffer = ByteBuffer.wrap(bytes)

        override fun ensureBuffer() = buffer
    }

    /**
     * Allows reading data from the byte array that is obtained though [loadBytes].
     *
     * The byte array may not be known at the time of [OnDemandMemoryBuffer] creation.
     * It will be created later, on the first call to use {}.
     * The byte array is cached using [SoftReference], which means that it can be potentially
     * released (garbage collected) in-between the [use] blocks, in case of JVM heap memory deficit.
     *
     * This implementation allows having lesser memory consumption than [MemoryBuffer] in case
     * of occasional reads from [ReadByteBufferProvider].
     */
    class OnDemandMemoryBuffer(private val loadBytes: () -> ByteArray) : ReadByteBufferProvider() {
        private var cachedBuffer: SoftReference<ByteBuffer?> = SoftReference(null)

        override fun ensureBuffer(): ByteBuffer {
            var buffer = cachedBuffer.get()
            if (buffer != null) {
                return buffer
            }
            buffer = ByteBuffer.wrap(loadBytes())
            cachedBuffer = SoftReference(buffer)
            return buffer
        }
    }
}
