package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("name")
@Serializable
sealed class KotlinReleaseVersionDetails(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    override fun toString(): String {
        return "KotlinReleaseVersionDetails(major=$major, minor=$minor, patch=$patch)"
    }

    @Serializable
    @SerialName("1.4.0")
    class Kotlin140Details : KotlinReleaseVersionDetails(1, 4, 0)

    @Serializable
    @SerialName("1.9.20")
    class Kotlin1920Details : KotlinReleaseVersionDetails(1, 9, 20)

    @Serializable
    @SerialName("2.0.0")
    class Kotlin200Details : KotlinReleaseVersionDetails(2, 0, 0)

    companion object {
        val allReleaseVersions = listOf(
            Kotlin140Details(),
            Kotlin1920Details(),
            Kotlin200Details(),
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("name")
@Serializable
sealed class KotlinReleaseVersion(val details: KotlinReleaseVersionDetails) {

    @Serializable
    @SerialName("1.4.0")
    object KOTLIN_1_4_0 : KotlinReleaseVersion(KotlinReleaseVersionDetails.Kotlin140Details())

    @Serializable
    @SerialName("1.9.20")
    object KOTLIN_1_9_20 : KotlinReleaseVersion(KotlinReleaseVersionDetails.Kotlin1920Details())

    @Serializable
    @SerialName("2.0.0")
    object KOTLIN_2_0_0 : KotlinReleaseVersion(KotlinReleaseVersionDetails.Kotlin200Details())
}
