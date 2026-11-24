/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.serialization

import kotlinx.serialization.*
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer

interface SerializationFormat {

    val format: SerialFormat
    fun <T> serialize(serializationStrategy: SerializationStrategy<T>, data: T): SerializedDataContainer
    fun <T> deserialize(serializationStrategy: DeserializationStrategy<T>, data: SerializedDataContainer): T
}

inline fun <reified T> SerializationFormat.serialize(data: T): SerializedDataContainer =
    serialize(format.serializersModule.serializer(), data)

inline fun <reified T> SerializationFormat.deserialize(data: SerializedDataContainer): T =
    deserialize(format.serializersModule.serializer(), data)
