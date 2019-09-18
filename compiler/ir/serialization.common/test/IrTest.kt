/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.Coordinates
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class IrTest {

    /**
     * message Coordinates {
     *   required int32 start_offset = 1;
     *   required int32 end_offset = 2;
     * }
     */
    @Test
    fun coordinatesTest() {
        val start = 817_431_284
        val end = 736_818_941
        val bytes = Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build().toByteArray()

        val reader = IrProtoReader(bytes)
        val (newStart, newEnd) = reader.readCoordinates()

        assertEquals(start, newStart)
        assertEquals(end, newEnd)

    }

    /**
     * message IrDataIndex {
     *   required int32 index = 1;
     * }
     */
    @Test
    fun dataIndexTest() {
        val bytes = IrDataIndex.newBuilder().setIndex(100).build().toByteArray()

        val reader = IrProtoReader(bytes)

        assertEquals(100, reader.readDataIndex())
    }

    /**
     * message FileEntry {
     *   required string name = 1;
     *   repeated int32 line_start_offsets = 2;
     * }
     */
    @Test
    fun fileEntryTest() {
        val bytes = FileEntry.newBuilder()
            .addLineStartOffsets(10)
            .setName("<entry name>")
            .addAllLineStartOffsets(listOf(1, 2, 3))
            .build().toByteArray()

        val reader = IrProtoReader(bytes)
        val fileEntry = reader.readFileEntry()

        assertEquals("<entry name>", fileEntry.name)
        assertEquals(listOf(10, 1, 2, 3), fileEntry.lineStartOffsets.toList())
    }
}