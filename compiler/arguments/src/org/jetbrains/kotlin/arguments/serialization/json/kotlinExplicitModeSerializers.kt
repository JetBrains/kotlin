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
import org.jetbrains.kotlin.arguments.dsl.types.ExplicitApiMode
import org.jetbrains.kotlin.arguments.dsl.types.KotlinExplicitApiModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer

object KotlinExplicitApiModeAsModeSerializer : NamedTypeSerializer<ExplicitApiMode>(
    serialName = "org.jetbrains.kotlin.config.ExplicitApiMode",
    nameAccessor = { it.modeName },
    typeFinder = {
        ExplicitApiMode.entries.single { mode -> mode.modeName == it }
    }
)

private object AllExplicitApiModeSerializer : AllNamedTypeSerializer<ExplicitApiMode>(
    serialName = "org.jetbrains.kotlin.config.ExplicitApiMode",
    jsonElementNameForName = "modeName",
    nameAccessor = { it.modeName },
    typeFinder = {
        ExplicitApiMode.entries.single { mode -> mode.modeName == it }
    }
)

object AllDetailsExplicitApiModeSerializer : KSerializer<Set<ExplicitApiMode>> {
    private val delegateSerializer: KSerializer<Set<ExplicitApiMode>> = SetSerializer(AllExplicitApiModeSerializer)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.config.SetExplicitApiMode") {
        element<String>("type")
        element<Set<ExplicitApiMode>>("values")
    }

    override fun serialize(
        encoder: Encoder,
        value: Set<ExplicitApiMode>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, KotlinExplicitApiModeType::class.qualifiedName!!)
            encodeSerializableElement(descriptor, 1, delegateSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): Set<ExplicitApiMode> {
        var type = ""
        val values = mutableSetOf<ExplicitApiMode>()
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
