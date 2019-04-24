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

object JvmPlatforms {
    private val jvmTargetToJdkPlatform: Map<JvmTarget, TargetPlatform> =
        JvmTarget.values().map { it to JdkPlatform(it).toTargetPlatform() }.toMap()

    val defaultJvmPlatform: TargetPlatform = jvmTargetToJdkPlatform[JvmTarget.DEFAULT]!!

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