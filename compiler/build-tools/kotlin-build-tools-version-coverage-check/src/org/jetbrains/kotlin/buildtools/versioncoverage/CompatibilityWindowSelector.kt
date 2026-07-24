/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal class CompatibilityWindowSelector(private val compatibilityType: CompatibilityType) {

    fun select(all: List<KotlinToolingVersion>, current: KotlinToolingVersion): List<KotlinToolingVersion> {
        val selectedMinorVersionsWithHighestMaturity = all
            .groupBy { KotlinToolingVersion(it.major, it.minor, it.patch, null) }
            .filterKeys { kotlinVersion ->
                getVersionComparator().compare(kotlinVersion, current) >= 0
            }
            .values.mapNotNull { it.maxOrNull() }
            .groupBy { KotlinToolingVersion(it.major, it.minor, 0, null) }

        val currentMinor = KotlinToolingVersion(current.major, current.minor, 0, null)

        val compatibleVersions = (selectedMinorVersionsWithHighestMaturity.keys + currentMinor)
            .sortedWith(getVersionComparator())
            .take(compatibilityType.minorVersionSupportCount + 1)
            .mapNotNull { selectedMinorVersionsWithHighestMaturity[it] }
            .flatten()

        return compatibleVersions
    }

    private fun getVersionComparator(): Comparator<KotlinToolingVersion> =
        when (compatibilityType) {
            CompatibilityType.FORWARD -> compareBy { it }
            CompatibilityType.BACKWARD -> compareByDescending { it }
        }
}
