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
abstract class JvmPlatform : TargetPlatform("JVM")
abstract class JsPlatform : TargetPlatform("JS")

interface KotlinBuiltInPlatforms {
    val konanPlatform: KonanPlatform
    val commonPlatform: CommonPlatform
    val jvmPlatform: JvmPlatform
    val jsPlatform: JsPlatform

    fun areSamePlatforms(first: TargetPlatform, second: TargetPlatform): Boolean
}

object DefaultBuiltInPlatforms : KotlinBuiltInPlatforms {
    override val konanPlatform: KonanPlatform = object : KonanPlatform() {}
    override val commonPlatform: CommonPlatform = object : CommonPlatform() {}
    override val jvmPlatform: JvmPlatform = object : JvmPlatform() {}
    override val jsPlatform: JsPlatform = object : JsPlatform() {}

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