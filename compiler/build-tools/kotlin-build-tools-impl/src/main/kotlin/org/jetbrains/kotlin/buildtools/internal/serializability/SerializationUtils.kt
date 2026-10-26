/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.reflect.KMutableProperty1

internal fun <T, K : Any> K.getPropertyByName(
    propertyAccessors: Map<String, KMutableProperty1<*, out Any?>>,
    name: String,
): T {
    @Suppress("UNCHECKED_CAST")
    return (propertyAccessors[name] ?: error("Property with name $name not found in $this")).getter.call(this) as T
}

internal fun <T, K : Any> K.setPropertyByName(
    propertyAccessors: Map<String, KMutableProperty1<*, out Any?>>,
    name: String,
    value: T,
) {
    (propertyAccessors[name] ?: error("Property with name $name not found in $this")).setter.call(this, value)
}

internal object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.toFile().path)

    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}
