/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import kotlin.reflect.KClass

inline fun <reified T : SimplePlatform> TargetPlatform.subplatformsOfType(): List<T> = componentPlatforms.filterIsInstance<T>()
fun <T> TargetPlatform.subplatformsOfType(klass: Class<T>): List<T> = componentPlatforms.filterIsInstance(klass)

inline fun <reified T : SimplePlatform> TargetPlatform?.has(): Boolean = this != null && subplatformsOfType<T>().isNotEmpty()
fun TargetPlatform?.has(klass: KClass<*>): Boolean = this != null && subplatformsOfType(klass.java).isNotEmpty()


/**
 * Returns human-readable description, mapping multiplatform to 'Common (experimental)',
 * as well as maintaining some quirks of the previous representation, like trailing whitespaces
 *
 * It is needed mainly for backwards compatibility, because some subsystem actually
 * managed to rely on the format of that string. In particular, 'facetSerialization.kt' uses
 * those string as keys in serialized `.iml`-file, and changing format of that string (including
 * trimming pointless whitespaces) leads to incorrectly deserialized facets.
 *
 * New clients are encouraged to use [presentableDescription] description instead, as it
 * also provides better description for multiplatforms.
 */
val TargetPlatform.oldFashionedDescription: String
    // this method mistakenly detects "common native" platform as "Common (experimental)"
    // though this does not seem to have any significant effect
    get() = this.singleOrNull()?.oldFashionedDescription ?: "Common (experimental) "


/**
 * Renders multiplatform in form
 *      '$PLATFORM_1/$PLATFORM_2/...'
 * e.g.
 *      'JVM (1.8)/JS/Native (ios_x64)'
 */
val TargetPlatform.presentableDescription: String
    get() = componentPlatforms.joinToString(separator = "/")
