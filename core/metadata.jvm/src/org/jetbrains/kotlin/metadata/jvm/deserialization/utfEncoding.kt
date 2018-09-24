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

package org.jetbrains.kotlin.metadata.jvm.deserialization

import java.util.*

// The maximum possible length of the byte array in the CONSTANT_Utf8_info structure in the bytecode, as per JVMS7 4.4.7
const val MAX_UTF8_INFO_LENGTH = 65535

const val UTF8_MODE_MARKER = 0.toChar()

fun bytesToStrings(bytes: ByteArray): Array<String> {
    val result = ArrayList<String>(1)
    val buffer = StringBuilder()
    var bytesInBuffer = 0

    buffer.append(UTF8_MODE_MARKER)
    // Zeros effectively occupy two bytes because each 0x0 is converted to 0x80 0xc0 in Modified UTF-8, see JVMS7 4.4.7
    bytesInBuffer += 2

    for (b in bytes) {
        val c = b.toInt() and 0xFF // 0 <= c <= 255
        buffer.append(c.toChar())
        if (b in 1..127) {
            bytesInBuffer++
        } else {
            bytesInBuffer += 2
        }

        if (bytesInBuffer >= MAX_UTF8_INFO_LENGTH - 1) {
            result.add(buffer.toString())
            buffer.setLength(0)
            bytesInBuffer = 0
        }
    }

    if (!buffer.isEmpty()) {
        result.add(buffer.toString())
    }

    return result.toTypedArray()
}

fun stringsToBytes(strings: Array<String>): ByteArray {
    val resultLength = strings.sumBy { it.length }
    val result = ByteArray(resultLength)

    var i = 0
    for (s in strings) {
        for (si in 0..s.length - 1) {
            result[i++] = s[si].toByte()
        }
    }

    assert(i == result.size) { "Should have reached the end" }

    return result
}
