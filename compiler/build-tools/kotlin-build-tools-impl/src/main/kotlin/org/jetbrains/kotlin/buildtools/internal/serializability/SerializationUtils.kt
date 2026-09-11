/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties


internal fun KClass<*>.findPropertyWithSerialName(name: String): KMutableProperty<*> {
    return this.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>()
        .first { it.annotations.any { ann -> ann is SerialName && ann.value == name } }
}

internal object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.absolutePathStringOrThrow())

    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

internal object ListOfPathsAsStringSerializer : KSerializer<List<Path>> by ListSerializer(PathAsStringSerializer)

