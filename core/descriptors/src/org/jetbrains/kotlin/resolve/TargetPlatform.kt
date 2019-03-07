/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

sealed class TargetPlatform(val platformName: String) {
    override fun toString() = platformName
}

abstract class KonanPlatform : TargetPlatform("Native")
abstract class CommonPlatform : TargetPlatform("Common")
abstract class JvmPlatform(val targetVersion: JvmTarget) : TargetPlatform("JVM")
abstract class JsPlatform : TargetPlatform("JS")

interface KotlinBuiltInPlatforms {
    val konanPlatform: KonanPlatform
    val commonPlatform: CommonPlatform
    val jvmPlatform: JvmPlatform
    val jvm16: JvmPlatform
    val jvm18: JvmPlatform
    val jsPlatform: JsPlatform

    fun areSamePlatforms(first: TargetPlatform, second: TargetPlatform): Boolean
    fun jvmPlatformByTargetVersion(targetVersion: JvmTarget): JvmPlatform
}

object DefaultBuiltInPlatforms : KotlinBuiltInPlatforms {
    override val konanPlatform: KonanPlatform = object : KonanPlatform() {}
    override val commonPlatform: CommonPlatform = object : CommonPlatform() {}
    override val jvmPlatform: JvmPlatform = object : JvmPlatform(JvmTarget.DEFAULT) {}
    override val jvm16: JvmPlatform = object : JvmPlatform(JvmTarget.JVM_1_6) {}
    override val jvm18: JvmPlatform = object : JvmPlatform(JvmTarget.JVM_1_8) {}
    override val jsPlatform: JsPlatform = object : JsPlatform() {}

    override fun jvmPlatformByTargetVersion(targetVersion: JvmTarget) = when (targetVersion) {
        JvmTarget.JVM_1_6 -> jvm16
        JvmTarget.JVM_1_8 -> jvm18
    }

    override fun areSamePlatforms(first: TargetPlatform, second: TargetPlatform): Boolean = first === second
}

fun TargetPlatform?.isNative(): Boolean =
    this != null && DefaultBuiltInPlatforms.areSamePlatforms(this, DefaultBuiltInPlatforms.konanPlatform)

fun TargetPlatform?.isCommon(): Boolean =
    this != null && DefaultBuiltInPlatforms.areSamePlatforms(this, DefaultBuiltInPlatforms.commonPlatform)

fun TargetPlatform?.isJvm(): Boolean =
    this != null && DefaultBuiltInPlatforms.areSamePlatforms(this, DefaultBuiltInPlatforms.jvmPlatform)

fun TargetPlatform?.isJs(): Boolean =
    this != null && DefaultBuiltInPlatforms.areSamePlatforms(this, DefaultBuiltInPlatforms.jsPlatform)

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