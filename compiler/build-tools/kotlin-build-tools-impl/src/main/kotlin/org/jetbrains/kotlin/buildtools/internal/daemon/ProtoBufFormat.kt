/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.daemon

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import org.jetbrains.kotlin.daemon.common.serialization.SerializationFormat
import java.io.Serializable

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

// needs embedded(libs.kotlinx.serialization.json) in build.gradle.kts to work
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
