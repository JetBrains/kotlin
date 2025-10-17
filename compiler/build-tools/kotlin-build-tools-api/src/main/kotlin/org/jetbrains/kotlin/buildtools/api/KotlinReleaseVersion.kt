/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * Represents a specific Kotlin release version as denoted by [major].[minor].[patch] version.
 *
 * [major], [minor] and [patch] are integer components of a version,
 * they must be non-negative and not greater than 255.
 *
 * @constructor Creates a version from all three components.
 */
public class KotlinReleaseVersion(public val major: Int, public val minor: Int, public val patch: Int) : Comparable<KotlinReleaseVersion> {
    private fun toKotlinVersion(): KotlinVersion = KotlinVersion(major, minor, patch)
    override fun compareTo(other: KotlinReleaseVersion): Int {
        return this.toKotlinVersion().compareTo(other.toKotlinVersion())
    }

    override fun toString(): String = "$major.$minor.$patch"
}