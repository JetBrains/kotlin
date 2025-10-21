/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.kotlin.buildtools.internal.cri.CriDataDeserializerImpl
import org.jetbrains.kotlin.buildtools.internal.cri.CriDataSerializerImpl
import org.jetbrains.kotlin.buildtools.internal.cri.FileIdToPathEntryImpl
import org.jetbrains.kotlin.buildtools.internal.cri.LookupEntryImpl
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class CriSerializationTest {

    @Test
    fun testLookupSerialization() {
        val fqName1 = FqName("scope1.Name1")
        val fqName2 = FqName("scope2.Name2")
        val file1 = "A.kt"
        val file2 = "B.kt"
        val file3 = "C.kt"
        val lookups = mapOf(
            LookupSymbol(name = fqName1.shortName().asString(), scope = fqName1.parent().asString())
                    to listOf(file1, file2),
            LookupSymbol(name = fqName2.shortName().asString(), scope = fqName2.parent().asString())
                    to listOf(file2, file3),
        )

        val (serializedLookups, serializedFileIdsToPaths) = CriDataSerializerImpl().serializeLookups(lookups)

        val deserializer = CriDataDeserializerImpl()
        val decodedLookups = deserializer.deserializeLookupData(serializedLookups)
        val decodedFileIdsToPaths = deserializer.deserializeFileIdToPathData(serializedFileIdsToPaths)

        val expectedLookups = listOf(
            LookupEntryImpl(fqName1.hashCode(), listOf(1, 2)),
            LookupEntryImpl(fqName2.hashCode(), listOf(2, 3)),
        )
        assertEquals(expectedLookups, decodedLookups.toList())

        val expectedFileIdsToPaths = listOf(
            FileIdToPathEntryImpl(1, file1),
            FileIdToPathEntryImpl(2, file2),
            FileIdToPathEntryImpl(3, file3),
        )
        assertEquals(expectedFileIdsToPaths, decodedFileIdsToPaths.toList())
    }

    @Test
    fun testSubtypesSerialization() {
        val fqName1 = "scope1.Name1"
        val fqName2 = "scope2.Name2"
        val fqName3 = "scope3.Name3"
        val fqName4 = "scope4.Name4"
        val fqName5 = "scope5.Name5"

        val input = mapOf(
            FqName(fqName1) to listOf(FqName(fqName2), FqName(fqName3)),
            FqName(fqName4) to listOf(FqName(fqName5)),
        )

        val serializedSubtypes = CriDataSerializerImpl().serializeSubtypes(input)

        val subtypes = CriDataDeserializerImpl().deserializeSubtypeData(serializedSubtypes).map { it.className to it.subtypes }

        val expected = listOf(
            fqName1 to listOf(fqName2, fqName3),
            fqName4 to listOf(fqName5),
        )
        assertEquals(expected, subtypes)
    }
}
