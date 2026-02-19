/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class NamedTypeSerializer<T : Any>(
    private val serialName: String,
    private val nameAccessor: (T) -> String,
    private val typeFinder: (String) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = serialName,
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeString(nameAccessor(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val name = decoder.decodeString()
        return typeFinder(name)
    }
}
