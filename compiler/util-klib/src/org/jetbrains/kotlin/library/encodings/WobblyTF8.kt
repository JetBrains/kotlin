/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.kotlin.library.encodings

/**
 * Implementation of Wobbly Transformation Format 8, a superset of UTF-8
 * that encodes surrogate code points if they are not in a pair.
 *
 * See also https://simonsapin.github.io/wtf-8/
 */
object WobblyTF8 {

    fun encode(string: String): ByteArray {
        val stringLength = string.length
        if (stringLength == 0) return EMPTY_BYTE_ARRAY

        val buffer = ByteArray(stringLength * 3) // Allocate for the worse case.
        var writtenBytes = 0

        var index = 0
        while (index < stringLength) {
            val char1 = string[index++]

            if (char1 < '\u0080') {
                // U+0000..U+007F -> 0xxxxxxx
                // 7 meaningful bits -> 1 byte
                buffer[writtenBytes++] = char1.toInt()
            } else if (char1 < '\u0800') {
                // U+0080..U+07FF -> 110xxxxx 10xxxxxx
                // 11 meaningful bits -> 2 bytes
                val codePoint = char1.toInt()
                buffer[writtenBytes++] = (codePoint ushr 6) or 0b1100_0000
                buffer[writtenBytes++] = (codePoint and 0b0011_1111) or 0b1000_0000
            } else {
                if (Character.isHighSurrogate(char1) && index < stringLength) {
                    val char2 = string[index]
                    if (Character.isLowSurrogate(char2)) {
                        // a pair of surrogates, encode as in traditional UTF8
                        // U+D800..U+DBFF + U+DC00..U+DFFF -> 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                        // 21 meaningful bits -> 4 bytes
                        index++
                        val codePoint = Character.toCodePoint(char1, char2)
                        buffer[writtenBytes++] = (codePoint ushr 18) or 0b1111_0000
                        buffer[writtenBytes++] = ((codePoint ushr 12) and 0b0011_1111) or 0b1000_0000
                        buffer[writtenBytes++] = ((codePoint ushr 6) and 0b0011_1111) or 0b1000_0000
                        buffer[writtenBytes++] = (codePoint and 0b0011_1111) or 0b1000_0000
                        continue
                    }
                }

                // U+0800..U+FFFF -> 1110xxxx 10xxxxxx 10xxxxxx
                // 16 meaningful bits -> 3 bytes
                val codePoint = char1.toInt()
                buffer[writtenBytes++] = (codePoint ushr 12) or 0b1110_0000
                buffer[writtenBytes++] = ((codePoint ushr 6) and 0b0011_1111) or 0b1000_0000
                buffer[writtenBytes++] = (codePoint and 0b0011_1111) or 0b1000_0000
            }
        }

        return if (buffer.size == writtenBytes) buffer else buffer.copyOf(writtenBytes)
    }

    fun decode(array: ByteArray): String {
        val arraySize = array.size
        if (arraySize == 0) return EMPTY_STRING

        val buffer = CharArray(arraySize) // Allocate for the worse case.
        var charsWritten = 0

        var index = 0
        while (index < arraySize) {
            val byte1 = array.readByteAsInt(index++)

            if (byte1 and 0b1000_0000 == 0) {
                // 0xxxxxxx -> U+0000..U+007F
                // 1 byte -> 7 meaningful bits
                buffer[charsWritten++] = byte1
                continue
            } else if (byte1 ushr 5 == 0b000_0110) {
                // 110xxxxx 10xxxxxx -> U+0080..U+07FF
                // 2 bytes -> 11 meaningful bits
                if (index < arraySize) {
                    val byte2 = array.readByteAsInt(index)
                    if (isValidContinuation(byte2)) {
                        index++
                        buffer[charsWritten++] = ((byte1 and 0b0001_1111) shl 6) or (byte2 and 0b0011_1111)
                        continue
                    }
                }
            } else if (byte1 ushr 4 == 0b0000_1110) {
                // 1110xxxx 10xxxxxx 10xxxxxx -> U+0800..U+FFFF
                // 3 bytes -> 16 meaningful bits
                if (index < arraySize) {
                    val byte2 = array.readByteAsInt(index)
                    if (isValidContinuation(byte2)) {
                        index++
                        if (index < arraySize) {
                            val byte3 = array.readByteAsInt(index)
                            if (isValidContinuation(byte3)) {
                                index++
                                buffer[charsWritten++] = ((byte1 and 0b0000_1111) shl 12) or
                                        ((byte2 and 0b0011_1111) shl 6) or
                                        (byte3 and 0b0011_1111)
                                continue
                            }
                        }
                    }
                }
            } else if (byte1 ushr 3 == 0b0001_1110) {
                // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx -> U+D800..U+DBFF + U+DC00..U+DFFF
                // 4 bytes -> 21 meaningful bits
                if (index < arraySize) {
                    val byte2 = array.readByteAsInt(index)
                    if (isValidContinuation(byte2)) {
                        index++
                        if (index < arraySize) {
                            val byte3 = array.readByteAsInt(index)
                            if (isValidContinuation(byte3)) {
                                index++
                                if (index < arraySize) {
                                    val byte4 = array.readByteAsInt(index)
                                    if (isValidContinuation(byte4)) {
                                        index++
                                        val codePoint = ((byte1 and 0b0000_0111) shl 18) or
                                                ((byte2 and 0b0011_1111) shl 12) or
                                                ((byte3 and 0b0011_1111) shl 6) or
                                                (byte4 and 0b0011_1111)
                                        buffer[charsWritten++] = Character.highSurrogate(codePoint)
                                        buffer[charsWritten++] = Character.lowSurrogate(codePoint)
                                        continue
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // unexpected end of the byte sequence or unexpected bit pattern
            buffer[charsWritten++] = REPLACEMENT_CHAR
        }

        return if (buffer.size == charsWritten) String(buffer) else String(buffer, 0, charsWritten)
    }

    private fun ByteArray.readByteAsInt(index: Int): Int = this[index].toInt() and 0b1111_1111

    private operator fun ByteArray.set(index: Int, value: Int) {
        this[index] = value.toByte()
    }

    private operator fun CharArray.set(index: Int, value: Int) {
        this[index] = value.toChar()
    }

    private fun isValidContinuation(byteN: Int) = byteN ushr 6 == 0b0000_0010

    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    private const val EMPTY_STRING = ""

    private const val REPLACEMENT_CHAR = '\ufffd'
}
