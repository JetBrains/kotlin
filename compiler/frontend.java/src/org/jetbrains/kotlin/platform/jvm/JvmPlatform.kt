/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.jvm

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.toTargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion

abstract class JvmPlatform : SimplePlatform("JVM") {
    override val oldFashionedDescription: String
        get() = "JVM "
}

@Suppress("DEPRECATION_ERROR")
object JvmPlatforms {
    private object UnspecifiedSimpleJvmPlatform : JvmPlatform() {
        override val targetPlatformVersion: TargetPlatformVersion
            get() = JvmTarget.JVM_1_6
    }

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'defaultJvmPlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatJvmPlatform : TargetPlatform(setOf(UnspecifiedSimpleJvmPlatform)),
        // Needed for backward compatibility, because old code uses INSTANCEOF checks instead of calling extensions
        org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform {}

    private val jvmTargetToJdkPlatform: Map<JvmTarget, TargetPlatform> =
        JvmTarget.values().map { it to JdkPlatform(it).toTargetPlatform() }.toMap()

    val defaultJvmPlatform: TargetPlatform
        get() = CompatJvmPlatform

    val jvm16: TargetPlatform = jvmTargetToJdkPlatform[JvmTarget.JVM_1_6]!!
    val jvm18: TargetPlatform = jvmTargetToJdkPlatform[JvmTarget.JVM_1_8]!!

    fun jvmPlatformByTargetVersion(targetVersion: JvmTarget): TargetPlatform =
        jvmTargetToJdkPlatform[targetVersion]!!

    val allJvmPlatforms: List<TargetPlatform> = jvmTargetToJdkPlatform.values.toList()
}

data class JdkPlatform(val targetVersion: JvmTarget) : JvmPlatform() {
    override fun toString(): String = "$platformName ($targetVersion)"

    override val oldFashionedDescription: String
        get() = "JVM " + targetVersion.description

    override val targetPlatformVersion: TargetPlatformVersion
        get() = targetVersion
}

fun TargetPlatform?.isJvm(): Boolean = this?.singleOrNull() is JvmPlatform