/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal class CompatibilityWindowSelector(private val compatibilityType: CompatibilityType) {

    fun select(all: List<KotlinToolingVersion>, current: KotlinToolingVersion): List<KotlinToolingVersion> {
        val selectedVersionsWithHighestMaturity = all
            .groupBy { Triple(it.major, it.minor, it.patch) }
            .filterKeys { [major, minor, patch] ->
                val kotlinVersion = KotlinToolingVersion(major, minor, patch, null)
                when (compatibilityType) {
                    CompatibilityType.FORWARD -> kotlinVersion >= current
                    CompatibilityType.BACKWARD -> kotlinVersion <= current
                }
            }
            .values.mapNotNull { it.maxOrNull() }

        val compatibleVersions = selectedVersionsWithHighestMaturity
            .groupBy { it.major to it.minor }
            .toSortedMap(getVersionComparator())
            .values
            .take(compatibilityType.minorVersionSupportCount + 1)
            .flatten()

        return compatibleVersions
    }

    private fun getVersionComparator() =
        when (compatibilityType) {
            CompatibilityType.FORWARD -> compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }
            CompatibilityType.BACKWARD -> compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second }
        }
}
