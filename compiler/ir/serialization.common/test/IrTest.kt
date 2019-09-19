/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.Coordinates
import org.jetbrains.kotlin.backend.common.serialization.proto.FqName as ProtoFqName
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex
import org.jetbrains.kotlin.name.FqName
import org.junit.Assert.assertEquals
import org.junit.Test

class IrTest {

    private val stringTable = listOf("A", "B", "C", "D", "E")

    inner class MockIrProtoReader(bytes: ByteArray) : IrProtoReader(bytes) {
        override fun readStringById(id: Int) = stringTable[id]
    }

    /**
     * message IrDataIndex {
     *   required int32 index = 1;
     * }
     */

    @Test
    fun dataIndexTest() {
        val id = 8765643
        val bytes = IrDataIndex.newBuilder()
            .setIndex(id)
            .build().toByteArray()

        val reader = MockIrProtoReader(bytes)
        val newId = reader.readDataIndex()

        assertEquals(id, newId)
    }

    /**
     * message FqName {
     *   repeated IrDataIndex segment = 1;
     * }
     */

    @Test
    fun fqNameTest() {
        val fqnName = FqName("A.B.C.D.A")
        val proto = ProtoFqName.newBuilder()

        fqnName.pathSegments().forEach {
            val dataIndex = IrDataIndex.newBuilder().setIndex(stringTable.indexOf(it.asString()))
            proto.addSegment(dataIndex)
        }

        val bytes = proto.build().toByteArray()

        val reader = MockIrProtoReader(bytes)
        val newFqName = reader.readFqName()

        assertEquals(fqnName, newFqName)
    }

    /**
     * message Coordinates {
     *   required int32 start_offset = 1;
     *   required int32 end_offset = 2;
     * }
     */
    @Test
    fun coordinatesTest() {
        val start = -1
        val end = 736_818_941
        val bytes = Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build().toByteArray()

        val reader = MockIrProtoReader(bytes)
        val (newStart, newEnd) = reader.readCoordinates()

//        val simpleReader = SimpleIrProtoReader(bytes)
//        val result = simpleReader.readCoordinates() as Array<Any>

//        assertEquals(start, result[0] as Int)
//        assertEquals(end, result[1] as Int)

        assertEquals(start, newStart)
        assertEquals(end, newEnd)

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

        val reader = MockIrProtoReader(bytes)
        val fileEntry = reader.readFileEntry()

        assertEquals("<entry name>", fileEntry.name)
        assertEquals(listOf(10, 1, 2, 3), fileEntry.lineStartOffsets.toList())
    }
}