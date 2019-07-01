/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

inline fun <reified T : SimplePlatform> TargetPlatform.subplatformOfType(): T? = componentPlatforms.filterIsInstance<T>().singleOrNull()
fun <T> TargetPlatform.subplatformOfType(klass: Class<T>): T? = componentPlatforms.filterIsInstance(klass).singleOrNull()

inline fun <reified T : SimplePlatform> TargetPlatform?.has(): Boolean = this != null && subplatformOfType<T>() != null
fun <T> TargetPlatform?.has(klass: Class<T>): Boolean = this != null && subplatformOfType(klass) != null


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
    get() = if (this.isCommon()) "Common (experimental) " else this.single().oldFashionedDescription


/**
 * Renders multiplatform in form
 *      '$PLATFORM_1 / $PLATFORM_2 / ...'
 * e.g.
 *      'JVM (1.8) / JS / Native'
 */
val TargetPlatform.presentableDescription: String
    get() = componentPlatforms.joinToString(separator = "/")