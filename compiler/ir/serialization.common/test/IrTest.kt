/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class IrTest {

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