/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

class ProtoReader(val source: ByteArray,
                  var offset: Int = 0) {

    private inline fun nextByte(): Byte = source[offset++]

    private fun readVarint64(): Long {
        var result = 0L

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F).toLong() shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift >= 64) error("int64 overflow")

        return result
    }

    private fun readVarint32(): Int {
        var result = 0

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F) shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift >= 32) error("int32 overflow")

        return result
    }

    fun readInt32(): Int = readVarint32()

    fun readInt64(): Long = readVarint64()

    fun readBool(): Boolean = readVarint32() != 0

    fun readFloat(): Float {
        var bits = nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()

        return Float.fromBits(bits)
    }

    fun readDouble(): Double {
        var bits = nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()

        return Double.fromBits(bits)
    }

    fun readString(): String {
        val length = readInt32()
        val result = String(source, offset, length)
        offset += length
        return result
    }

    inline fun <T> readField(block: (Int, Int) -> T): T {
        val wire = readInt32()
        val fieldNumber = wire ushr 3
        val wireType = wire and 0x7
        return block(fieldNumber, wireType)
    }
}

enum class MessageType {
    VARINT,
    FIXED64,
    LENGTH_DELIMITED,
    START_GROUP,
    END_GROUP,
    FIXED_32
}