/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.junit.*
import org.junit.Assert.*;

/**
 * Based on https://developers.google.com/protocol-buffers/docs/encoding
 */
class SimpleTest {
    private fun String.asBytes(): ByteArray {
        return this.split(" ").map {
            it.toInt(radix = 16).toByte()
        }.toByteArray()
    }

    /**
     * message Test1 {
     *   optional int32 a = 1;
     * }
     *
     * a = 150
     */
    @Test fun testInt32() {
        val bytes = "08 96 01".asBytes()
        val reader = ProtoReader(bytes)
        reader.readField { number, type ->
            assertEquals(1, number)
            assertEquals(0, type)
            assertEquals(150, reader.readInt32())
        }
    }

    /**
     * message Test2 {
     *   optional string b = 2;
     * }
     *
     * b = "testing"
     */
    @Test fun testString() {
        val bytes = "12 07 74 65 73 74 69 6e 67".asBytes()
        val reader = ProtoReader(bytes)
        reader.readField { number, type ->
            assertEquals(2, number)
            assertEquals(2, type)
            assertEquals("testing", reader.readString())
        }
    }

    /**
     * message Test3 {
     *   optional Test1 c = 3;
     * }
     *
     * c = 150
     */
    @Test fun testNested() {
        val bytes = "1a 03 08 96 01".asBytes()
        val reader = ProtoReader(bytes)
        reader.readField { number, type ->
            assertEquals(3, number)
            assertEquals(2, type)

            val test1Length = reader.readInt32()
            assertEquals(3, test1Length)

            reader.readField { number, type ->
                assertEquals(1, number)
                assertEquals(0, type)
                assertEquals(150, reader.readInt32())
            }
        }
    }

    /**
     * message Test3 {
     *   repeated Test1 c = 3;
     * }
     *
     * c = 150
     */
    @Test fun testRepeated() {
        val bytes = "1a 03 08 96 01 1a 03 08 96 01 1a 03 08 96 01".asBytes()
        val reader = ProtoReader(bytes)
        while (reader.hasData) {

            reader.readField { number, type ->
                assertEquals(3, number)
                assertEquals(2, type)

                reader.readWithLength {
                    reader.readField { number, type ->
                        assertEquals(1, number)
                        assertEquals(0, type)
                        assertEquals(150, reader.readInt32())
                    }
                }
            }
        }
    }

}