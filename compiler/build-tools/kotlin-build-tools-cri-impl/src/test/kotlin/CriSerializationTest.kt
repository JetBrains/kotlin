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
import org.jetbrains.kotlin.incremental.storage.BasicFileToPathConverter
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

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

        val (serializedLookups, serializedFileIdsToPaths) = CriDataSerializerImpl().serializeLookups(lookups, BasicFileToPathConverter)

        val deserializer = CriDataDeserializerImpl()
        val decodedLookups = deserializer.deserializeLookupData(serializedLookups)
        val decodedFileIdsToPaths = deserializer.deserializeFileIdToPathData(serializedFileIdsToPaths)

        val expectedLookups = listOf(
            LookupEntryImpl(fqName1.hashCode(), listOf(file1.hashCode(), file2.hashCode())),
            LookupEntryImpl(fqName2.hashCode(), listOf(file2.hashCode(), file3.hashCode())),
        )
        assertEquals(expectedLookups, decodedLookups.toList())

        val expectedFileIdsToPaths = listOf(
            FileIdToPathEntryImpl(file1.hashCode(), file1),
            FileIdToPathEntryImpl(file2.hashCode(), file2),
            FileIdToPathEntryImpl(file3.hashCode(), file3),
        )
        assertEquals(expectedFileIdsToPaths, decodedFileIdsToPaths.toList())
    }

    @Test
    fun testLookupSerializationWithStreaming() {
        val fqName1 = FqName("scope1.Name1")
        val fqName2 = FqName("scope2.Name2")
        val fqName3 = FqName("scope3.Name3")
        val file1 = "A.kt"
        val file2 = "B.kt"
        val file3 = "C.kt"
        val lookups1 = mapOf(
            LookupSymbol(name = fqName1.shortName().asString(), scope = fqName1.parent().asString())
                    to listOf(file1, file2),
            LookupSymbol(name = fqName2.shortName().asString(), scope = fqName2.parent().asString())
                    to listOf(file2, file3),
        )
        val lookups2 = mapOf(
            LookupSymbol(name = fqName1.shortName().asString(), scope = fqName1.parent().asString())
                    to listOf(file3),
            LookupSymbol(name = fqName3.shortName().asString(), scope = fqName3.parent().asString())
                    to listOf(file1, file2),
        )

        val (serializedLookups1, _) = CriDataSerializerImpl().serializeLookups(lookups1, BasicFileToPathConverter)
        val (serializedLookups2, _) = CriDataSerializerImpl().serializeLookups(lookups2, BasicFileToPathConverter)
        val serializedLookups = ByteArrayOutputStream().use { stream ->
            stream.write(serializedLookups1)
            stream.write(serializedLookups2)
            stream.toByteArray()
        }

        val deserializer = CriDataDeserializerImpl()
        val decodedLookups = deserializer.deserializeLookupData(serializedLookups)

        val expectedLookups = listOf(
            LookupEntryImpl(fqName1.hashCode(), listOf(file1.hashCode(), file2.hashCode())),
            LookupEntryImpl(fqName2.hashCode(), listOf(file2.hashCode(), file3.hashCode())),
            LookupEntryImpl(fqName1.hashCode(), listOf(file3.hashCode())),
            LookupEntryImpl(fqName3.hashCode(), listOf(file1.hashCode(), file2.hashCode())),
        )
        assertEquals(expectedLookups, decodedLookups.toList())
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

        val subtypes = CriDataDeserializerImpl().deserializeSubtypeData(serializedSubtypes).map { it.fqNameHashCode to it.subtypes }

        val expected = listOf(
            fqName1.hashCode() to listOf(fqName2, fqName3),
            fqName4.hashCode() to listOf(fqName5),
        )
        assertEquals(expected, subtypes)
    }
}
