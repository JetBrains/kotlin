/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.konan

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.toTargetPlatform

sealed class NativePlatform : SimplePlatform("Native")

object NativePlatformUnspecifiedTarget : NativePlatform() {
    override fun toString() = "$platformName (general)"

    override val oldFashionedDescription: String
        get() = "Native (general) "
}

data class NativePlatformWithTarget(val target: KonanTarget) : NativePlatform() {
    override fun toString() = "$platformName ($target)"

    override val oldFashionedDescription: String
        get() = "Native ($target) "
}

@Suppress("DEPRECATION_ERROR")
object NativePlatforms {
    private val predefinedNativeTargetToSimpleNativePlatform: Map<KonanTarget, NativePlatformWithTarget> =
        KonanTarget.predefinedTargets.values.associateWith { NativePlatformWithTarget(it) }

    private val predefinedNativeTargetToNativePlatform: Map<KonanTarget, TargetPlatform> =
        predefinedNativeTargetToSimpleNativePlatform.mapValues { (_, simplePlatform) -> simplePlatform.toTargetPlatform() }

    val unspecifiedNativePlatform: TargetPlatform
        get() = CompatNativePlatform

    val allNativePlatforms: List<TargetPlatform> = listOf(unspecifiedNativePlatform) + predefinedNativeTargetToNativePlatform.values

    fun nativePlatformBySingleTarget(target: KonanTarget): TargetPlatform =
        predefinedNativeTargetToNativePlatform[target] ?: unspecifiedNativePlatform

    fun nativePlatformByTargets(targets: Collection<KonanTarget>): TargetPlatform {
        val simplePlatforms = targets.mapNotNullTo(HashSet()) { predefinedNativeTargetToSimpleNativePlatform[it] }
        return when (simplePlatforms.size) {
            0 -> unspecifiedNativePlatform
            1 -> nativePlatformBySingleTarget(simplePlatforms.first().target)
            else -> TargetPlatform(simplePlatforms)
        }
    }

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'unspecifiedNativePlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatNativePlatform : TargetPlatform(setOf(NativePlatformUnspecifiedTarget)),
        // Needed for backward compatibility, because old code uses INSTANCEOF checks instead of calling extensions
        org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform {
        override val platformName: String
            get() = "Native"
    }
}

fun TargetPlatform?.isNative(): Boolean = this?.isNotEmpty() == true && all { it is NativePlatform }
