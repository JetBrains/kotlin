/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import java.io.Serializable

public interface SerializationFormat {

    public val format: SerialFormat
    public fun <T> serialize(serializationStrategy: SerializationStrategy<T>, data: T): SerializedDataContainer
    public fun <T> deserialize(serializationStrategy: DeserializationStrategy<T>, data: SerializedDataContainer): T
}

@OptIn(ExperimentalSerializationApi::class)
internal object ProtoBufFormat : SerializationFormat {

    override val format = ProtoBuf { serializersModule = btaSerializersModule }
    override fun <T> serialize(serializationStrategy: SerializationStrategy<T>, data: T): SerializedDataContainer {
        return ProtoBufContainer(format.encodeToByteArray(serializationStrategy, data))
    }

    override fun <T> deserialize(serializationStrategy: DeserializationStrategy<T>, data: SerializedDataContainer): T {
        require(data is ProtoBufContainer)
        return format.decodeFromByteArray(serializationStrategy, data.byteArray)
    }

    class ProtoBufContainer(val byteArray: ByteArray) : SerializedDataContainer, Serializable
}

// needs implementation(libs.kotlinx.serialization.protobuf) in build.gradle
// leave here for enabling easier debugging of data

//internal object JsonFormat : SerializationFormat {
//    override val format = Json {
//        serializersModule = btaSerializersModule
//        encodeDefaults = true
//        classDiscriminator = "__type__"
//    }
//
//    override fun <T> serialize(serializationStrategy: SerializationStrategy<T>, data: T): SerializedDataContainer {
//        return JsonContainer(format.encodeToString(serializationStrategy, data))
//    }
//
//    override fun <T> deserialize(serializationStrategy: DeserializationStrategy<T>, data: SerializedDataContainer): T {
//        require(data is JsonContainer)
//        return format.decodeFromString(serializationStrategy, data.string)
//    }
//
//    class JsonContainer(val string: String) : SerializedDataContainer, Serializable
//}


public inline fun <reified T> SerializationFormat.serialize(data: T): SerializedDataContainer =
    serialize(format.serializersModule.serializer(), data)

public inline fun <reified T> SerializationFormat.deserialize(data: SerializedDataContainer): T =
    deserialize(format.serializersModule.serializer(), data)
