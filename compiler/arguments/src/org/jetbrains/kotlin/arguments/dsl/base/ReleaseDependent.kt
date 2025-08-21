/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.serialization.json.ReleaseDependentSerializer

/**
 * A type which value could change between releases.
 *
 * @param current the latest actual value of this type
 * @param valueInVersions provides information about previous values of this type in the given [KotlinReleaseVersion] range
 *
 * Few helper methods are available for this type:
 * - [ReleaseDependent] - alternative way to define this type
 * - [asReleaseDependent] - quick way to convert another type to this one
 */
@Serializable(with = ReleaseDependentSerializer::class)
data class ReleaseDependent<T>(
    val current: T,
    val valueInVersions: Map<ClosedRange<KotlinReleaseVersion>, T>
)

/**
 * Creates an instance of [ReleaseDependent] class.
 *
 * Example usage:
 * ```
 * val someField = ReleaseDependent(
 *     current = "The target version of the generated JVM bytecode (1.8 and 9-24), with 1.8 as the default.",
 *     KotlinReleaseVersion.v2.1.0..KotlinReleaseVersion.v2_1_21 to
 *         "The target version of the generated JVM bytecode (1.8 and 9-23), with 1.8 as the default.",
 *     KotlinReleaseVersion.v2_0_0..KotlinReleaseVersion.v2_2_21 to
 *         "The target version of the generated JVM bytecode (1.8 and 9-22), with 1.8 as the default.",
 * )
 * ```
 */
internal fun <T> ReleaseDependent(
    current: T,
    vararg oldValues: Pair<ClosedRange<KotlinReleaseVersion>, T>,
) = ReleaseDependent(current, oldValues.associate { it })

/**
 * Wraps current type [T] into [ReleaseDependent] type where [ReleaseDependent.current] value is type [T] value.
 *
 * Example usage:
 * ```
 * val someFields = "some value".asReleaseDependent()
 * ```
 */
internal fun <T> T.asReleaseDependent() = ReleaseDependent(this)
