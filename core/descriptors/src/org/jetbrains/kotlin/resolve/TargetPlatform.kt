/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Collection<SimplePlatform> by componentPlatforms {
    init {
        if (componentPlatforms.isEmpty()) throw IllegalArgumentException("Don't instantiate TargetPlatform with empty set of platforms")
    }

    @Deprecated("Introduces legacy CommonPlatform, use just 'single' or 'singleOrNull' extensions instead")
    fun convertToOldPlatforms(): SimplePlatform = componentPlatforms.singleOrNull() ?: DefaultBuiltInPlatforms.commonPlatform.single()

    inline fun <reified T : SimplePlatform> subplatformOfType(): T? = componentPlatforms.filterIsInstance<T>().singleOrNull()
}

sealed class SimplePlatform(val platformName: String) {
    override fun toString(): String = platformName
}

abstract class KonanPlatform : SimplePlatform("Native")
abstract class CommonPlatform : SimplePlatform("Common")

abstract class JvmPlatform : SimplePlatform("JVM")
data class JdkPlatform(val targetVersion: JvmTarget) : JvmPlatform()


abstract class JsPlatform : SimplePlatform("JS")

interface KotlinBuiltInPlatforms {
    val konanPlatform: TargetPlatform
    val commonPlatform: TargetPlatform
    val jvmPlatform: TargetPlatform
    val jvm16: TargetPlatform
    val jvm18: TargetPlatform
    val jsPlatform: TargetPlatform

    fun jvmPlatformByTargetVersion(targetVersion: JvmTarget): TargetPlatform
}

object DefaultBuiltInPlatforms : KotlinBuiltInPlatforms {
    override val konanPlatform: TargetPlatform = object : KonanPlatform() {}.toTargetPlatform()

    override val commonPlatform: TargetPlatform = object : CommonPlatform() {}.toTargetPlatform()

    override val jvmPlatform: TargetPlatform = JdkPlatform(JvmTarget.DEFAULT).toTargetPlatform()
    override val jvm16: TargetPlatform = JdkPlatform(JvmTarget.JVM_1_6).toTargetPlatform()
    override val jvm18: TargetPlatform = JdkPlatform(JvmTarget.JVM_1_8).toTargetPlatform()

    override val jsPlatform: TargetPlatform = object : JsPlatform() {}.toTargetPlatform()

    override fun jvmPlatformByTargetVersion(targetVersion: JvmTarget) = when (targetVersion) {
        JvmTarget.JVM_1_6 -> jvm16
        JvmTarget.JVM_1_8 -> jvm18
    }
}

fun TargetPlatform?.isNative(): Boolean =
    this?.singleOrNull() is KonanPlatform

fun TargetPlatform?.isJvm(): Boolean =
    this?.singleOrNull() is JvmPlatform

fun TargetPlatform?.isJs(): Boolean =
    this?.singleOrNull() is JsPlatform

fun TargetPlatform?.isCommon(): Boolean = when (this?.size) {
    0 -> false
    1 -> this == DefaultBuiltInPlatforms.commonPlatform
    else -> false
}

fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform(setOf(this))

//fun SimplePlatform?.isNative(): Boolean =
//    this is KonanPlatform
//
//fun SimplePlatform?.isCommon(): Boolean =
//    this is CommonPlatform
//
//fun SimplePlatform?.isJvm(): Boolean =
//    this is JvmPlatform
//
//fun SimplePlatform?.isJs(): Boolean =
//    this is JsPlatform


enum class JvmTarget(override val description: String) : TargetPlatformVersion {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    ;

    companion object {
        @JvmField
        val DEFAULT = JVM_1_6

        @JvmStatic
        fun fromString(string: String) = values().find { it.description == string }
    }
}

interface TargetPlatformVersion {
    val description: String

    object NoVersion : TargetPlatformVersion {
        override val description = ""
    }
}

