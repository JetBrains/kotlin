/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

/**
 * Core abstraction of Platform API, represents a collection of platforms.
 *
 * This is the primarily abstraction intended to use in the most part of API, as, usually,
 * pretty much anything that may have a platform, may have a several platforms as well in the
 * context of multiplatform projects.
 *
 * Please, use it over the [SimplePlatform] unless you're absolutely sure what you're doing.
 *
 * NB. Even in cases, where some part of logic makes sense only for a particular platform (e.g., JVM),
 * it still can be applicable for [TargetPlatform]s with [componentPlatforms] > 1, e.g. when it consists
 * of two version of JDK, JDK and Android, several versions of Android API, etc.
 */
open class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Collection<SimplePlatform> by componentPlatforms {
    init {
        if (componentPlatforms.isEmpty()) throw IllegalArgumentException("Don't instantiate TargetPlatform with empty set of platforms")
    }

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
 * Core abstraction of Platform API, represents exactly one platform.
 *
 * API guarantees:
 *
 * - direct inheritors are well-known and represent three major platforms supported at the moment (JVM, JS, Native)
 *
 * - exact enumeration of all inheritors isn't available at the compile time, see [CommonPlatforms]
 *
 * - each implementation should support equality in a broad sense of "absolutely the same platform"
 *
 * - it is _prohibited_ to create instances of [SimplePlatform] in the client's code, use respective factory instance (e.g., [JvmPlatforms])
 *  to get instances of platforms
 *
 * Ideally, each specific subtype should be either a data class or singleton.
 */
abstract class SimplePlatform(val platformName: String) {
    override fun toString(): String = platformName

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

fun TargetPlatform?.isCommon(): Boolean = this != null && this.size > 1

fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform(setOf(this))