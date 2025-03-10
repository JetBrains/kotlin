/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

interface WithKotlinReleaseVersionsMetadata {
    val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
}

data class KotlinReleaseVersionLifecycle(
    val introducedVersion: KotlinReleaseVersion,
    val stabilizedVersion: KotlinReleaseVersion? = null,
    val deprecatedVersion: KotlinReleaseVersion? = null,
    val removedVersion: KotlinReleaseVersion? = null,
)
