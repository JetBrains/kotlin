/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

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
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersionType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer

object KotlinVersionAsNameSerializer : NamedTypeSerializer<KotlinVersion>(
    serialName = "org.jetbrains.kotlin.arguments.KotlinVersion",
    nameAccessor = { it.versionName },
    typeFinder = {
        KotlinVersion.entries.single { version -> version.versionName == it }
    }
)

private object AllKotlinVersionSerializer : AllNamedTypeSerializer<KotlinVersion>(
    serialName = "org.jetbrains.kotlin.arguments.KotlinVersion",
    jsonElementNameForName = "name",
    nameAccessor = { it.versionName },
    typeFinder = {
        KotlinVersion.entries.single { version -> version.versionName == it }
    }
)


object AllDetailsKotlinVersionSerializer : KSerializer<Set<KotlinVersion>> {
    private val delegateSerializer: KSerializer<Set<KotlinVersion>> = SetSerializer(AllKotlinVersionSerializer)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.arguments.SetKotlinVersion") {
        element<String>("type")
        element<Set<KotlinVersion>>("values")
    }

    override fun serialize(
        encoder: Encoder,
        value: Set<KotlinVersion>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, KotlinVersionType::class.qualifiedName!!)
            encodeSerializableElement(descriptor, 1, delegateSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): Set<KotlinVersion> {
        var type = ""
        val values = mutableSetOf<KotlinVersion>()
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
