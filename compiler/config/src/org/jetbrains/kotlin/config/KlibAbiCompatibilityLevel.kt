/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.library.KotlinAbiVersion

enum class KlibAbiCompatibilityLevel(val major: Int, val minor: Int) {
    ABI_LEVEL_2_2(2, 2),
    ABI_LEVEL_2_3(2, 3),
    ;

    override fun toString() = "$major.$minor"

    fun toAbiVersionForManifest(): KotlinAbiVersion = KotlinAbiVersion(major, minor, 0)

    fun isAtLeast(other: KlibAbiCompatibilityLevel): Boolean =
        major > other.major || major == other.major && minor >= other.minor

    companion object {
        val LATEST_STABLE = ABI_LEVEL_2_3
    }
}
