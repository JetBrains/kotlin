/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal class CompatibilityWindowSelector(private val compatibilityType: CompatibilityType) {

    fun select(all: List<KotlinToolingVersion>, current: KotlinToolingVersion): List<KotlinToolingVersion> {
        val selectedVersionsWithHighestMaturity = all
            .groupBy { KotlinToolingVersion(it.major, it.minor, it.patch, null) }
            .filterKeys { kotlinVersion ->
                getVersionComparator().compare(kotlinVersion, current) >= 0
            }
            .values.mapNotNull { it.maxOrNull() }

        val compatibleVersions = selectedVersionsWithHighestMaturity
            .groupBy { KotlinToolingVersion(it.major, it.minor, 0, null) }
            .toSortedMap(getVersionComparator())
            .values
            .take(compatibilityType.minorVersionSupportCount + 1)
            .flatten()

        return compatibleVersions
    }

    private fun getVersionComparator(): Comparator<KotlinToolingVersion> =
        when (compatibilityType) {
            CompatibilityType.FORWARD -> compareBy { it }
            CompatibilityType.BACKWARD -> compareByDescending { it }
        }
}
