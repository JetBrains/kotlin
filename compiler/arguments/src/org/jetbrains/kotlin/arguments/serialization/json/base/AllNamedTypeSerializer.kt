/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata

abstract class AllNamedTypeSerializer<T : WithKotlinReleaseVersionsMetadata>(
    serialName: String,
    private val jsonElementNameForName: String,
    private val nameAccessor: (T) -> String,
    private val typeFinder: (String) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        serialName,
        KotlinReleaseVersionLifecycle.serializer().descriptor
    ) {
        element<String>(jsonElementNameForName)
        element<KotlinReleaseVersionLifecycle>("releaseVersionsMetadata")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, nameAccessor(value))
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer(), value.releaseVersionsMetadata)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        var nameValue = ""
        lateinit var metadata: KotlinReleaseVersionLifecycle
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> nameValue = decodeStringElement(descriptor, 0)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(nameValue.isNotEmpty())
        return typeFinder(nameValue)
    }
}