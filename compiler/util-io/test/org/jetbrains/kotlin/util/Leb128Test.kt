/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.readUnsignedLeb128
import org.jetbrains.kotlin.utils.writeUnsignedLeb128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Leb128Test {
    @Test
    fun unsignedRoundTrip() {
        for (value in listOf(0u, 1u, 0x7fu, 0x80u, 0x3fffu, 0x4000u, 0x0fff_ffffu, 0x1000_0000u, UInt.MAX_VALUE)) {
            val bytes = mutableListOf<Byte>()
            writeUnsignedLeb128(value) { bytes += it }
            assertEquals(value, readBytes(bytes))
        }
    }

    @Test
    fun unsignedMaxValueTakesFiveBytes() {
        assertEquals(UInt.MAX_VALUE, readBytes(listOf(0xff, 0xff, 0xff, 0xff, 0x0f)))
    }

    @Test
    fun unsignedFifthByteMustFitInFourBits() {
        // 0x10 shifted by 28 lands at bit 32, which `UInt` cannot hold; decoding it as `0u` would hide the corruption.
        assertThrows(IllegalStateException::class.java) { readBytes(listOf(0x80, 0x80, 0x80, 0x80, 0x10)) }
    }

    @Test
    fun unsignedRejectsTooManyBytes() {
        assertThrows(IllegalStateException::class.java) { readBytes(listOf(0x80, 0x80, 0x80, 0x80, 0x80, 0x00)) }
    }

    private fun readBytes(bytes: List<Number>): UInt {
        val iterator = bytes.iterator()
        return readUnsignedLeb128({ iterator.next().toByte() })
    }
}
