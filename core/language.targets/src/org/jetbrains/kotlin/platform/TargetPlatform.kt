/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Core abstraction of the Platform API, representing a collection of platforms.
 *
 * This is the primary abstraction intended to be used in the majority of the API, as, usually,
 * anything that may have a platform may also have several platforms in the context of multiplatform
 * projects.
 *
 * Please use it over [SimplePlatform] unless you are absolutely sure what you are doing.
 *
 * NB. Even in cases where some part of the logic makes sense only for a particular platform (e.g., JVM),
 * it still can be applicable for [TargetPlatform]s with [componentPlatforms] > 1. For example, when the
 * platform consists of two JDK versions, JDK and Android, several versions of the Android API, and so on.
 */
open class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Iterable<SimplePlatform> by componentPlatforms {
    init {
        if (componentPlatforms.isEmpty()) throw IllegalArgumentException("Don't instantiate TargetPlatform with empty set of platforms")
    }

    val size: Int
        get() = componentPlatforms.size

    override fun toString(): String = presentableDescription

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is TargetPlatform) return false

        if (componentPlatforms != other.componentPlatforms) return false

        return true
    }

    override fun hashCode(): Int {
        return componentPlatforms.hashCode()
    }
}

/**
 * Core abstraction of the Platform API, representing exactly one platform.
 *
 * API guarantees:
 *
 * - Direct inheritors are well-known and represent the major platforms supported at the moment (JVM, JS, Native, Wasm).
 * - The exact enumeration of all inheritors isn't available at compile time, see [CommonPlatforms].
 * - Each implementation should support equality in a broad sense of "absolutely the same platform."
 * - It is _prohibited_ to create instances of [SimplePlatform] in the client's code. Use the respective factory instances such as
 *   [JvmPlatforms] to get instances of platforms.
 *
 * Ideally, each subtype should either be a data class or a singleton.
 */
abstract class SimplePlatform(val platformName: String) {
    override fun toString(): String {
        val targetName = targetName
        return if (targetName.isNotEmpty()) "$platformName ($targetName)" else platformName
    }

    // description of TargetPlatformVersion or name of custom platform-specific target; used in serialization
    open val targetName: String
        get() = targetPlatformVersion.description

    /** See KDoc for [TargetPlatform.oldFashionedDescription] */
    abstract val oldFashionedDescription: String

    // FIXME(dsavvinov): hack to allow injection inject JvmTarget into container.
    //   Proper fix would be to rewrite clients to get JdkPlatform from container, and pull JvmTarget from it
    //   (this will also remove need in TargetPlatformVersion as the whole, and, in particular, ugly passing
    //   of TargetPlatformVersion.NoVersion in non-JVM code)
    open val targetPlatformVersion: TargetPlatformVersion = TargetPlatformVersion.NoVersion
}

interface TargetPlatformVersion {
    val description: String

    object NoVersion : TargetPlatformVersion {
        override val description = ""
    }
}

/**
 * Whether this is a [TargetPlatform] that targets multiple [SimplePlatform]s.
 */
fun TargetPlatform?.isMultiPlatform(): Boolean {
    contract { returns(true) implies (this@isMultiPlatform != null) }
    return this != null && size > 1
}

/**
 * Whether this is a "Common" platform in its classical sense (MPP v1).
 */
fun TargetPlatform?.isCommon(): Boolean {
    contract { returns(true) implies (this@isCommon != null) }
    return isMultiPlatform() && this.iterator().let { i ->
        val firstPlatformName = i.next().platformName
        while (i.hasNext()) {
            if (i.next().platformName != firstPlatformName) return@let true
        }
        false
    }
}

fun TargetPlatform?.isMultiplatformWeb(): Boolean {
    contract { returns(true) implies (this@isMultiplatformWeb != null) }
    return isMultiPlatform() && all { it is PotentiallyWebPlatform && it.isWeb }
}

fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform([this])

fun SimplePlatform.serializeToString(): String = "$platformName [$targetName]"
