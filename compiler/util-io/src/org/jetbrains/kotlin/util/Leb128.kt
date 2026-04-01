/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.utils

import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

fun writeUnsignedLeb128Fixed(v: UInt, writeNextByte: (Byte) -> Unit) {
    @Suppress("NAME_SHADOWING")
    var v = v
    var remaining = v shr 7
    repeat(UInt.SIZE_BYTES) {
        val byte = (v and 0x7fu) or 0x80u
        writeNextByte(byte.toByte())
        v = remaining
        remaining = remaining shr 7
    }
    val byte = v and 0x7fu
    writeNextByte(byte.toByte())
}

fun writeUnsignedLeb128(v: UInt, writeNextByte: (Byte) -> Unit) {
    @Suppress("NAME_SHADOWING")
    var v = v
    var remaining = v shr 7
    while (remaining != 0u) {
        val byte = (v and 0x7fu) or 0x80u
        writeNextByte(byte.toByte())
        v = remaining
        remaining = remaining shr 7
    }
    val byte = v and 0x7fu
    writeNextByte(byte.toByte())
}

fun writeSignedLeb128(v: Long, writeNextByte: (Byte) -> Unit) {
    @Suppress("NAME_SHADOWING")
    var v = v
    var remaining = v shr 7
    var hasMore = true
    val end = if (v and Long.MIN_VALUE == 0L) 0L else -1L
    while (hasMore) {
        hasMore = remaining != end || remaining and 1 != (v shr 6) and 1
        val byte = ((v and 0x7f) or if (hasMore) 0x80 else 0).toInt()
        writeNextByte(byte.toByte())
        v = remaining
        remaining = remaining shr 7
    }
}

fun readUnsignedLeb128(readNextByte: () -> Byte, maxCount: Int = 4): UInt {
    var result = 0u
    var cur: UInt
    var count = 0
    do {
        cur = readNextByte().toUInt() and 0xffu
        result = result or ((cur and 0x7fu) shl (count * 7))
        count++
    } while (cur and 0x80u == 0x80u && count <= maxCount)
    if (cur and 0x80u == 0x80u) error("InvalidLeb128Number")
    return result
}

fun readSignedLeb128(readNextByte: () -> Byte, maxCount: Int = 4): Long {
    var result = 0L
    var cur: Int
    var count = 0
    var signBits = -1L
    do {
        cur = readNextByte().toInt() and 0xff
        result = result or ((cur and 0x7f).toLong() shl (count * 7))
        signBits = signBits shl 7
        count++
    } while (cur and 0x80 == 0x80 && count <= maxCount)
    if (cur and 0x80 == 0x80) error("InvalidLeb128Number")

    // Check for 64 bit invalid, taken from Apache/MIT licensed:
    //  https://github.com/paritytech/parity-wasm/blob/2650fc14c458c6a252c9dc43dd8e0b14b6d264ff/src/elements/primitives.rs#L351
    // TODO: probably need 32 bit checks too, but meh, not in the suite
    if (count > maxCount && maxCount == 9) {
        if (cur and 0b0100_0000 == 0b0100_0000) {
            if ((cur or 0b1000_0000).toByte() != (-1).toByte()) error("InvalidLeb128Number")
        } else if (cur != 0) {
            error("InvalidLeb128Number")
        }
    }

    if ((signBits shr 1) and result != 0L) result = result or signBits
    return result
}