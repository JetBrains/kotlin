/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

abstract class CustomTypeSerializer<T : Any>(
    private val valueTypeQualifiedNamed: String,
    serialName: String,
) : KSerializer<TypeDescriptor> {

    private val propertiesSerializer = ListSerializer(Property.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        serialName
    ) {
        element<String>("type")
        element("properties", propertiesSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: TypeDescriptor) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, valueTypeQualifiedNamed)
            encodeSerializableElement(descriptor, 1, propertiesSerializer, value.properties)
        }
    }

    override fun deserialize(decoder: Decoder): TypeDescriptor {
        var type = ""
        var properties = emptyList<Property>()
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> properties = decodeSerializableElement(descriptor, 1, propertiesSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        require(type.isNotEmpty())
        return TypeDescriptor(properties)
    }
}