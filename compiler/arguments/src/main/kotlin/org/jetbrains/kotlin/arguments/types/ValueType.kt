package org.jetbrains.kotlin.arguments.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.AllDetailsJvmTargetSerializer
import org.jetbrains.kotlin.arguments.AllDetailsKotlinVersionSerializer
import org.jetbrains.kotlin.arguments.JvmTarget
import org.jetbrains.kotlin.arguments.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.KotlinVersion
import org.jetbrains.kotlin.arguments.ReleaseDependent
import kotlin.Boolean

@Serializable
class KotlinArgumentTypes {
    @Serializable(with = AllDetailsKotlinVersionSerializer::class)
    val kotlinVersions = KotlinVersion.entries.toSet()

    @Serializable(with = AllDetailsJvmTargetSerializer::class)
    val jvmTargets = JvmTarget.entries.toSet()
}

@Serializable
sealed interface KotlinArgumentValueType<T : Any> {
    val isNullable: ReleaseDependent<Boolean>
    val defaultValue: ReleaseDependent<T?>
}

@Serializable
class BooleanType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<Boolean?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Boolean>

@Serializable
class KotlinVersionType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<KotlinVersion?> = ReleaseDependent(
        KotlinVersion.v2_0,
        KotlinReleaseVersion.v1_4_0..KotlinReleaseVersion.v1_9_20 to KotlinVersion.v1_9
    )
) : KotlinArgumentValueType<KotlinVersion>

@Serializable
class KotlinJvmTargetType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<JvmTarget?> = ReleaseDependent(
        JvmTarget.jvm1_8,
        KotlinReleaseVersion.v1_4_0..KotlinReleaseVersion.v1_9_20 to JvmTarget.jvm1_6
    )
) : KotlinArgumentValueType<JvmTarget>
