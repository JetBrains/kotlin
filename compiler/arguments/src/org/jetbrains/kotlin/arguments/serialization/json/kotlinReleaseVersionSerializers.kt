/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion

object KotlinReleaseVersionAsNameSerializer : KSerializer<KotlinReleaseVersion> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "org.jetbrains.kotlin.arguments.KotlinReleaseVersion",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(
        encoder: Encoder,
        value: KotlinReleaseVersion,
    ) {
        encoder.encodeString(value.releaseName)
    }

    override fun deserialize(decoder: Decoder): KotlinReleaseVersion {
        val versionName = decoder.decodeString()
        return KotlinReleaseVersion.entries.single { version -> version.releaseName == versionName }
    }
}
