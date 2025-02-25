package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = KotlinReleaseVersionAsNameSerializer::class)
@KeepGeneratedSerializer
class KotlinReleaseVersion(
    val name: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
)

object KotlinReleaseVersions {
    val v1_4_0 = KotlinReleaseVersion("1.4.0", 1, 4, 0)
    val v1_9_20 = KotlinReleaseVersion("1.9.20", 1, 9, 20)
    val v2_0_0 = KotlinReleaseVersion("2.0.0", 2, 0, 0)

    val allKotlinReleaseVersions = setOf(
        v1_4_0,
        v1_9_20,
        v2_0_0,
    )
}

object KotlinReleaseVersionAsNameSerializer : KSerializer<KotlinReleaseVersion> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "org.jetbrains.kotlin.arguments.KotlinReleaseVersion",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(
        encoder: Encoder,
        value: KotlinReleaseVersion,
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): KotlinReleaseVersion {
        val versionName = decoder.decodeString()
        return KotlinReleaseVersions.allKotlinReleaseVersions.single { version -> version.name == versionName }
    }
}

object AllDetailsKotlinReleaseVersionSerializer : KSerializer<Set<KotlinReleaseVersion>> {
    private val delegateSerializer: KSerializer<Set<KotlinReleaseVersion>> = SetSerializer(KotlinReleaseVersion.generatedSerializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Set<KotlinReleaseVersion>,
    ) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Set<KotlinReleaseVersion> {
        return delegateSerializer.deserialize(decoder)
    }
}
