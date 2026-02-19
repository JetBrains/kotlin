/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

abstract class SetTypeSerializer<T : Any>(
    typeSerializer: KSerializer<T>,
    private val valueTypeQualifiedNamed: String,
    serialName: String,
) : KSerializer<Set<T>> {
    private val delegateSerializer: KSerializer<Set<T>> = SetSerializer(typeSerializer)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
        element<String>("type")
        element("values", delegateSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: Set<T>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, valueTypeQualifiedNamed)
            encodeSerializableElement(descriptor, 1, delegateSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): Set<T> {
        var type = ""
        val values = mutableSetOf<T>()
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> values.addAll(decodeSerializableElement(descriptor, 1, delegateSerializer))
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(type.isNotEmpty() && values.isNotEmpty())
        return values.toSet()
    }
}