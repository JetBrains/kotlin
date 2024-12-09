package org.jetbrains.kotlin.arguments

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.Boolean

@Serializable
class KotlinArgumentTypes {
    val kotlinVersions = KotlinVersionDetails.allKotlinVersions
    val jvmTargets = JvmTargetDetails.allJvmTargets
}

@Serializable
sealed interface KotlinArgumentValueType<T : Any> {
    val name: String
    val isNullable: Boolean
    val defaultValue: T?

    @Serializable
    class BooleanType(
        @Required override val name: String = Boolean::class.simpleName!!,
        @Required override val isNullable: Boolean = true,
        @Required override val defaultValue: Boolean? = null,
    ) : KotlinArgumentValueType<Boolean>

    @Serializable
    class KotlinVersionType(
        @Required override val name: String = KotlinVersion::class.simpleName!!,
        @Required override val isNullable: Boolean = true,
        @Required override val defaultValue: KotlinVersion? = KotlinVersion.KOTLIN_2_0,
    ) : KotlinArgumentValueType<KotlinVersion>

    @Serializable
    class KotlinJvmTargetType(
        @Required override val name: String = JvmTarget::class.simpleName!!,
        @Required override val isNullable: Boolean = true,
        @Required override val defaultValue: JvmTarget? = JvmTarget.JvmTarget_1_8,
    ) : KotlinArgumentValueType<JvmTarget>
}
