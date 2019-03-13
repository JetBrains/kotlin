/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

// TODO(dsavvinov): discuss and split up this file into several, maybe move in another package

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
data class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Collection<SimplePlatform> by componentPlatforms {
    init {
        if (componentPlatforms.isEmpty()) throw IllegalArgumentException("Don't instantiate TargetPlatform with empty set of platforms")
    }

    override fun toString(): String = presentableDescription
}

/**
 * Core abstraction of Platform API, represents exactly one platform.
 *
 * API guarantees:
 *
 * - direct inheritors are well-known and represent three major platforms supported at the moment (JVM, JS, Native)
 *
 * - exact enumeration of all inheritors isn't available at the compile time: see [KotlinPlatforms]
 *
 * - each implementation should support equality in a broad sense of "absolutely the same platform"
 *
 * - it is _prohibited_ to create instances of [SimplePlatform] in the client's code, use [KotlinPlatforms] to get instances
 *   of platforms
 *
 * Ideally, each specific subtype should be either a data class or singleton.
 */
sealed class SimplePlatform(val platformName: String) {
    override fun toString(): String = platformName
}

abstract class KonanPlatform : SimplePlatform("Native")
abstract class JvmPlatform : SimplePlatform("JVM")
abstract class JsPlatform : SimplePlatform("JS")

data class JdkPlatform(val targetVersion: JvmTarget) : JvmPlatform() {
    override fun toString(): String = "$platformName ($targetVersion)"
}

interface KotlinPlatforms {
    val konanPlatform: TargetPlatform

    val jvmPlatform: TargetPlatform

    val jsPlatform: TargetPlatform

    val commonPlatform: TargetPlatform

    val jvm16: TargetPlatform
    val jvm18: TargetPlatform

    fun jvmPlatformByTargetVersion(targetVersion: JvmTarget): TargetPlatform

    val allSimplePlatforms: List<TargetPlatform>
}

object DefaultBuiltInPlatforms : KotlinPlatforms {
    private val jvmTargetToJdkPlatform: Map<JvmTarget, TargetPlatform> =
        JvmTarget.values().map { it to JdkPlatform(it).toTargetPlatform() }.toMap()

    override val konanPlatform: TargetPlatform = object : KonanPlatform() {}.toTargetPlatform()

    override val jvmPlatform: TargetPlatform = JdkPlatform(JvmTarget.DEFAULT)
        .toTargetPlatform()

    override val jvm16: TargetPlatform = jvmTargetToJdkPlatform[JvmTarget.JVM_1_6]!!
    override val jvm18: TargetPlatform = JdkPlatform(JvmTarget.JVM_1_8)
        .toTargetPlatform()


    override val jsPlatform: TargetPlatform = object : JsPlatform() {}.toTargetPlatform()

    override fun jvmPlatformByTargetVersion(targetVersion: JvmTarget) = jvmTargetToJdkPlatform[targetVersion]!!

    override val commonPlatform: TargetPlatform = TargetPlatform(
        setOf(
            jvm18.single(),
            jsPlatform.single(),
            konanPlatform.single()
        )
    )

    override val allSimplePlatforms: List<TargetPlatform>
        get() = sequence {
            yieldAll(jvmTargetToJdkPlatform.values)
            yield(konanPlatform)
            yield(jsPlatform)

            // TODO(dsavvinov): extensions points?
        }.toList()
}

inline fun <reified T : SimplePlatform> TargetPlatform.subplatformOfType(): T? = componentPlatforms.filterIsInstance<T>().singleOrNull()
fun <T> TargetPlatform.subplatformOfType(klass: Class<T>): T? = componentPlatforms.filterIsInstance(klass).singleOrNull()

inline fun <reified T : SimplePlatform> TargetPlatform?.has(): Boolean = this != null && subplatformOfType<T>() != null
fun <T> TargetPlatform?.has(klass: Class<T>): Boolean = this != null && subplatformOfType(klass) != null

fun TargetPlatform?.isNative(): Boolean = this?.singleOrNull() is KonanPlatform
fun TargetPlatform?.isJvm(): Boolean = this?.singleOrNull() is JvmPlatform
fun TargetPlatform?.isJs(): Boolean = this?.singleOrNull() is JsPlatform
fun TargetPlatform?.isCommon(): Boolean = this != null && this.size > 1

fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform(setOf(this))

/**
 * Returns human-readable description, mapping multiplatform to 'Common (experimental)',
 * as well as maintaining some quirks of the previous representation.
 * It is needed mainly for backwards compatibility, because some subsystem actually
 * managed to rely on the format of that string (yes, facets, I'm looking at you).
 *
 * New clients are encouraged to use [presentableDescription] description instead, as it
 * also provides better description for multiplatforms.
 */
val TargetPlatform.oldFashionedDescription: String
    get() = when (val singlePlatform = singleOrNull()) {
        is JdkPlatform -> "JVM " + singlePlatform.targetVersion.description
        is JvmPlatform -> "JVM "
        is JsPlatform -> "JavaScript "
        is KonanPlatform -> "Kotlin/Native "
        null -> "Common (experimental) "
    }

/**
 * Renders multiplatform in form
 *      '$PLATFORM_1 / $PLATFORM_2 / ...'
 * e.g.
 *      'JVM (1.8) / JS / Native'
 */
val TargetPlatform.presentableDescription: String
    get() = componentPlatforms.joinToString(separator = "/")


enum class JvmTarget(override val description: String) : TargetPlatformVersion {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    JVM_9("9"),
    JVM_10("10"),
    JVM_11("11"),
    JVM_12("12"),
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

