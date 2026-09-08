/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SerializedMetadataTest {
    @Test
    fun `Package fragment names must match package fragments`() {
        assertThrows<IllegalArgumentException> {
            SerializedMetadata(
                module = ByteArray(0),
                fragments = listOf(listOf(ByteArray(1))),
                fragmentNames = listOf("foo", "bar"),
                metadataVersion = MetadataVersion.INSTANCE.toArray(),
            )
        }
    }

    @Test
    fun `Packages without package fragments are rejected`() {
        assertThrows<IllegalArgumentException> {
            SerializedMetadata(
                module = ByteArray(0),
                fragments = listOf(listOf(ByteArray(1)), emptyList()),
                fragmentNames = listOf("bar", "foo"),
                metadataVersion = MetadataVersion.INSTANCE.toArray(),
            )
        }
    }
}
