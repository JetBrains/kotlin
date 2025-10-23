import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import org.jetbrains.kotlin.buildtools.internal.cri.CriDataDeserializerImpl
import org.jetbrains.kotlin.buildtools.internal.cri.FileIdToPathEntryImpl
import org.jetbrains.kotlin.buildtools.internal.cri.LookupEntryImpl
import org.jetbrains.kotlin.buildtools.internal.cri.SubtypeEntryImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@OptIn(ExperimentalSerializationApi::class)
class CriDeserializationTest {

    @Test
    fun generateProtoSchema() {
        val descriptors = listOf(
            LookupEntryImpl.serializer().descriptor,
            FileIdToPathEntryImpl.serializer().descriptor,
            SubtypeEntryImpl.serializer().descriptor,
        )
        val schema = ProtoBufSchemaGenerator.generateSchemaText(descriptors)
        assertEquals(EXPECTED_SCHEMA, schema)
    }

    @Test
    fun testLookupsSerialization() {
        val lookups = listOf(
            LookupEntryImpl(0, emptyList()),
            LookupEntryImpl(1, listOf(0)),
            LookupEntryImpl(2, listOf(1, 2)),
        )
        val serializedLookups = ProtoBuf.encodeToByteArray(lookups)
        val deserializer = CriDataDeserializerImpl()
        val deserializedLookups = deserializer.deserializeLookupData(serializedLookups)
        assertEquals(lookups, deserializedLookups)
    }

    companion object {
        // TODO ignore comments, etc. in schema generation
        private val EXPECTED_SCHEMA = """
            syntax = "proto2";


            // serial name 'org.jetbrains.kotlin.buildtools.internal.cri.LookupEntryImpl'
            message LookupEntryImpl {
              optional int32 fqNameHashCode = 1;
              repeated int32 fileIds = 2;
            }

            // serial name 'org.jetbrains.kotlin.buildtools.internal.cri.FileIdToPathEntryImpl'
            message FileIdToPathEntryImpl {
              optional int32 fileId = 1;
              optional string path = 2;
            }

            // serial name 'org.jetbrains.kotlin.buildtools.internal.cri.SubtypeEntryImpl'
            message SubtypeEntryImpl {
              optional int32 fqNameHashCode = 1;
              repeated string subtypes = 2;
            }
            
        """.trimIndent()
    }
}
