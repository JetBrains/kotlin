/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.jvm

import java.util.*

// The maximum possible length of the byte array in the CONSTANT_Utf8_info structure in the bytecode, as per JVMS7 4.4.7
private val MAX_UTF8_INFO_LENGTH = 65535

// Leading bytes are prefixed with 110 in UTF-8
private val LEADING_BYTE_MASK = 0b11000000
// Continuation bytes are prefixed with 10 in UTF-8
private val CONTINUATION_BYTE_MASK = 0b10000000

private val TWO_HIGHER_BITS_MASK = 0b11000000
private val TWO_LOWER_BITS_MASK = 0b00000011
private val SIX_LOWER_BITS_MASK = 0b00111111

fun bytesToStrings(bytes: ByteArray): List<String> {
    val result = ArrayList<String>(1)
    val buffer = StringBuilder()
    var bytesInBuffer = 0

    for (b in bytes) {
        if (b >= 0) {
            buffer.append(b.toChar())
            bytesInBuffer++
        }
        else {
            val int = b.toInt() and 0xFF
            val leadingByte = LEADING_BYTE_MASK or ((int and TWO_HIGHER_BITS_MASK) shr 6)
            val continuationByte = CONTINUATION_BYTE_MASK or (int and SIX_LOWER_BITS_MASK)
            val encodedByte = (leadingByte shl 8) or continuationByte

            buffer.append(encodedByte.toChar())
            bytesInBuffer += 2

            if (bytesInBuffer > MAX_UTF8_INFO_LENGTH) {
                result.add(buffer.substring(0, buffer.length - 1))
                buffer.setLength(0)
                buffer.append(encodedByte.toChar())
                bytesInBuffer = 2
            }
        }

        if (bytesInBuffer == MAX_UTF8_INFO_LENGTH) {
            result.add(buffer.toString())
            buffer.setLength(0)
            bytesInBuffer = 0
        }
    }

    if (!buffer.isEmpty()) {
        result.add(buffer.toString())
    }

    return result
}

fun stringsToBytes(strings: Array<String>): ByteArray {
    val resultLength = strings.sumBy { it.length }
    val result = ByteArray(resultLength)

    var i = 0
    for (s in strings) {
        for (si in 0..s.length - 1) {
            val c = s[si]

            val int = c.toInt()
            if (int <= 127) {
                result[i++] = c.toByte()
            }
            else {
                val leadingByte = (int and 0xFFFF) shr 8
                val continuationByte = int and 0xFF
                val higherBits = (leadingByte and TWO_LOWER_BITS_MASK) shl 6
                val lowerBits = continuationByte and SIX_LOWER_BITS_MASK
                result[i++] = (higherBits or lowerBits).toByte()
            }
        }
    }

    return result
}