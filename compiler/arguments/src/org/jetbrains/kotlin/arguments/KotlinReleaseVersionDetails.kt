package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = KotlinReleaseVersionAsNameSerializer::class)
enum class KotlinReleaseVersion(
    val releaseName: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<KotlinReleaseVersion> {
    v1_4_0("1.4.0", 1, 4, 0),
    v1_9_20("1.9.20", 1, 9, 20),
    v2_0_0("2.0.0", 2, 0, 0);
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
        encoder.encodeString(value.releaseName)
    }

    override fun deserialize(decoder: Decoder): KotlinReleaseVersion {
        val versionName = decoder.decodeString()
        return KotlinReleaseVersion.entries.single { version -> version.releaseName == versionName }
    }
}

object AllKotlinReleaseVersionSerializer : KSerializer<KotlinReleaseVersion> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.arguments.KotlinReleaseVersion") {
        element<String>("name")
        element<Int>("major")
        element<Int>("minor")
        element<Int>("patch")
    }

    override fun serialize(encoder: Encoder, value: KotlinReleaseVersion) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.releaseName)
            encodeIntElement(descriptor, 1, value.major)
            encodeIntElement(descriptor, 2, value.minor)
            encodeIntElement(descriptor, 3, value.patch)
        }
    }

    override fun deserialize(decoder: Decoder): KotlinReleaseVersion {
        var releaseName = ""
        var major = -1
        var minor = -1
        var patch = -1
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> releaseName = decodeStringElement(descriptor, 0)
                    1 -> major = decodeIntElement(descriptor, 1)
                    2 -> minor = decodeIntElement(descriptor, 2)
                    3 -> patch = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(releaseName.isNotEmpty() && major >= 1 && minor >= 0 && patch >= 0)
        return KotlinReleaseVersion.entries.single { version -> version.releaseName == releaseName }
    }
}

object AllDetailsKotlinReleaseVersionSerializer : KSerializer<Set<KotlinReleaseVersion>> {
    private val delegateSerializer: KSerializer<Set<KotlinReleaseVersion>> = SetSerializer(AllKotlinReleaseVersionSerializer)
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
