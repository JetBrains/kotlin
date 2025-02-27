package org.jetbrains.kotlin.arguments

import kotlinx.serialization.Serializable
import kotlin.Boolean

@Serializable
class KotlinArgumentTypes {
    @Serializable(with = AllDetailsKotlinVersionSerializer::class)
    val kotlinVersions = KotlinVersions.allKotlinVersions

    @Serializable(with = AllDetailsJvmTargetSerializer::class)
    val jvmTargets = JvmTargets.allJvmTargets
}

@Serializable
sealed interface KotlinArgumentValueType<T : Any> {
    val name: String
    val isNullable: ReleaseDependent<Boolean>
    val defaultValue: ReleaseDependent<T?>

    @Serializable
    class BooleanType(
        override val name: String = Boolean::class.simpleName!!,
        override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
        override val defaultValue: ReleaseDependent<Boolean?> = ReleaseDependent(null),
    ) : KotlinArgumentValueType<Boolean>

    @Serializable
    class KotlinVersionType(
        override val name: String = KotlinVersion::class.simpleName!!,
        override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
        override val defaultValue: ReleaseDependent<KotlinVersion?> = ReleaseDependent(
            KotlinVersions.v2_0,
            KotlinReleaseVersions.v1_4_0..KotlinReleaseVersions.v1_9_20 to KotlinVersions.v1_9
        )
    ) : KotlinArgumentValueType<KotlinVersion>

    @Serializable
    class KotlinJvmTargetType(
        override val name: String = JvmTarget::class.simpleName!!,
        override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
        override val defaultValue: ReleaseDependent<JvmTarget?> = ReleaseDependent(
            JvmTargets.jvm1_8,
            KotlinReleaseVersions.v1_4_0..KotlinReleaseVersions.v1_9_20 to JvmTargets.jvm1_6
        )
    ) : KotlinArgumentValueType<JvmTarget>
}
