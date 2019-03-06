/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

abstract class TargetPlatform(val platformName: String) {
    override fun toString() = platformName
}

object KonanPlatform : TargetPlatform("Native")
object CommonPlatform : TargetPlatform("Common")
object JvmPlatform : TargetPlatform("JVM")
object JsPlatform : TargetPlatform("JS")

fun TargetPlatform?.isNative(): Boolean = this === KonanPlatform
fun TargetPlatform?.isCommon(): Boolean = this === CommonPlatform
fun TargetPlatform?.isJvm(): Boolean = this === JvmPlatform
fun TargetPlatform?.isJs(): Boolean = this === JsPlatform