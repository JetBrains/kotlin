/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal enum class CompatibilityType(val minorVersionSupportCount: Int, val comparator: Comparator<KotlinToolingVersion>) {
    BACKWARD(3, compareBy<KotlinToolingVersion> { it }.reversed()),
    FORWARD(1, compareBy { it });

    companion object {
        fun fromString(string: String): CompatibilityType =
            entries.firstOrNull { it.name.lowercase() == string } ?: error("Unknown compatibility type: $string")
    }
}
