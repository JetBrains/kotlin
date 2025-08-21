/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable

/**
 * Types/values containing [KotlinReleaseVersionLifecycle] metadata should implement this interface.
 */
interface WithKotlinReleaseVersionsMetadata {

    /**
     * Kotlin release version lifecycle metadata.
     */
    val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
}

/**
 * Metadata containing information about type or value lifecycle in Kotlin releases.
 *
 * Types/values containing such metadata should implement [WithKotlinReleaseVersionsMetadata] interface.
 *
 * @param introducedVersion in which Kotlin release, this type/value was introduced. Mandatory property.
 * @param stabilizedVersion in which Kotlin release, this type/value was promoted to stable.
 * If the value of this property is `null` - the type/value should be considered experimental.
 * @param deprecatedVersion in which Kotlin release, this type/value was deprecated.
 * @param removedVersion in which Kotlin release, this type/value was removed.
 */
@Serializable
data class KotlinReleaseVersionLifecycle(
    val introducedVersion: KotlinReleaseVersion,
    val stabilizedVersion: KotlinReleaseVersion? = null,
    val deprecatedVersion: KotlinReleaseVersion? = null,
    val removedVersion: KotlinReleaseVersion? = null,
)
