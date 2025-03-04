/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class ReleaseDependent<T : Any?>(
    val current: T,
    val valueInVersions: Map<@Serializable(with = ClosedRangeKotlinReleaseVersionSerializer::class) ClosedRange<KotlinReleaseVersion>, T>
)

fun <T : Any?> ReleaseDependent(
    current: T,
    vararg oldValues: Pair<ClosedRange<KotlinReleaseVersion>, T>,
) = ReleaseDependent(current, oldValues.associate { it })

fun <T : Any?> T.asReleaseDependent() = ReleaseDependent(this)

object ClosedRangeKotlinReleaseVersionSerializer : KSerializer<ClosedRange<KotlinReleaseVersion>> {
    private const val delimiter = ".."

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "org.jetbrains.kotlin.arguments.ClosedRangeKotlinReleaseVersion",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ClosedRange<KotlinReleaseVersion>) {
        val serializedString = "${value.start.releaseName}$delimiter${value.endInclusive.releaseName}"
        encoder.encodeString(serializedString)
    }

    override fun deserialize(decoder: Decoder): ClosedRange<KotlinReleaseVersion> {
        val serializedString = decoder.decodeString()
        val startName = serializedString.substringBefore(delimiter)
        val start = KotlinReleaseVersion.entries.single { it.releaseName == startName }
        val endInclusiveName = serializedString.substringAfter(delimiter)
        val endInclusive = KotlinReleaseVersion.entries.single { it.releaseName == endInclusiveName }
        return start..endInclusive
    }
}