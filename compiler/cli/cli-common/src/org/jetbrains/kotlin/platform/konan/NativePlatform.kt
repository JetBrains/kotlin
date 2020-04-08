/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.konan

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.toTargetPlatform

sealed class NativePlatform : SimplePlatform("Native") {
    override val oldFashionedDescription: String
        get() = toString() + " "
}

object NativePlatformUnspecifiedTarget : NativePlatform() {
    override val targetName: String
        get() = "general"
}

data class NativePlatformWithTarget(val target: KonanTarget) : NativePlatform() {
    override fun toString() = super.toString() // override the method generated for data class

    override val targetName: String
        get() = target.visibleName
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

    fun nativePlatformByTargetNames(targets: Collection<String>): TargetPlatform =
        nativePlatformByTargets(targets.mapNotNull { KonanTarget.predefinedTargets[it] })

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

private val legacyNativePlatformUnspecifiedTargetSerializedRepresentation = "${NativePlatformUnspecifiedTarget.platformName} []"
fun NativePlatformUnspecifiedTarget.legacySerializeToString(): String = legacyNativePlatformUnspecifiedTargetSerializedRepresentation
