/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.dsl.types.WithStringRepresentation

class NamedTypeSerializerWithVersions<T>(
    private val enumClass: Class<T>,
) : KSerializer<T> where T : WithKotlinReleaseVersionsMetadata, T : Enum<T>, T : WithStringRepresentation {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        enumClass.name,
        KotlinReleaseVersionLifecycle.serializer().descriptor
    ) {
        element<String>("name")
        element<KotlinReleaseVersionLifecycle>("releaseVersionsMetadata")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.stringRepresentation)
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer(), value.releaseVersionsMetadata)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        var nameValue = ""
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> nameValue = decodeStringElement(descriptor, 0)
                    1 -> decodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(nameValue.isNotEmpty())
        return enumClass.enumConstants.single { it.stringRepresentation == nameValue }
    }
}

inline fun <reified T> allNamedTypeSerializerWithVersions(): KSerializer<T> where T : WithKotlinReleaseVersionsMetadata, T : Enum<T>, T : WithStringRepresentation =
    NamedTypeSerializerWithVersions(enumClass = T::class.java)
