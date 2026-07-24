/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Compatibility window selector")
internal class CompatibilityWindowSelectorTest {

    private lateinit var backwardSelector: CompatibilityWindowSelector
    private lateinit var forwardSelector: CompatibilityWindowSelector

    @BeforeTest
    fun setup() {
        backwardSelector = CompatibilityWindowSelector(CompatibilityType.BACKWARD)
        forwardSelector = CompatibilityWindowSelector(CompatibilityType.FORWARD)
    }

    // ── BACKWARD: window size ─────────────────────────────────────────────────

    @Test
    @DisplayName("Backward: returns empty when input is empty")
    fun `backward - empty input returns empty`() {
        assertTrue(backwardSelector.select(emptyList(), KotlinToolingVersion("2.3.0")).isEmpty())
    }

    @Test
    @DisplayName("Backward: includes current and 3 prior minor versions")
    fun `backward - takes current minor version plus 3 previous`() {
        val all = listOf("2.0.0", "2.1.0", "2.2.0", "2.3.0", "2.4.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.0.0", "2.1.0", "2.2.0", "2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: includes all available versions when fewer than the window count exist")
    fun `backward - truncates when fewer minor versions available than count`() {
        val all = listOf("2.2.0", "2.3.0", "2.4.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.2.0", "2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: includes all patch versions within a selected minor version")
    fun `backward - all patch versions in a minor version are included`() {
        val all = listOf("2.3.0", "2.3.10", "2.3.20", "2.2.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.3.20"))
        assertEquals(listOf("2.2.0", "2.3.0", "2.3.10", "2.3.20").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: crosses major version boundaries when counting minor versions")
    fun `backward - cross-major boundary`() {
        val all = listOf("1.9.0", "2.0.0", "2.1.0", "2.2.0", "2.3.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.2.0"))
        assertEquals(listOf("1.9.0", "2.0.0", "2.1.0", "2.2.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: snapshot current in an unpublished minor version counts the window from its own minor version")
    fun `backward - snapshot current in unpublished minor version counts window from current`() {
        val all = listOf("1.9.0", "2.0.0", "2.1.0", "2.2.0", "2.3.0", "2.4.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.5.255-SNAPSHOT"))
        assertEquals(listOf("2.2.0", "2.3.0", "2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: returns minor versions in descending order")
    fun `backward - minor versions sorted descending`() {
        val all = listOf("2.0.0", "2.1.0", "2.2.0", "2.3.0", "2.4.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0", "2.2.0", "2.1.0", "2.0.0").map { KotlinToolingVersion(it) }, result)
    }

    @Test
    @DisplayName("Backward: SNAPSHOT is excluded since it is not published")
    fun `backward - SNAPSHOT excluded`() {
        val result =
            backwardSelector.select(listOf("2.3.0-SNAPSHOT", "2.3.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.4.0"))
        assertEquals(listOf("2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: DEV is excluded since it is not published")
    fun `backward - DEV excluded`() {
        val result =
            backwardSelector.select(listOf("2.3.0-dev-123", "2.3.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.4.0"))
        assertEquals(listOf("2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: STABLE, RC, and BETA versions are all included")
    fun `backward - STABLE RC and BETA are included`() {
        val all = listOf("2.3.0", "2.3.10-RC", "2.3.20-Beta1").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.4.0"))
        assertEquals(listOf("2.3.0", "2.3.10-RC", "2.3.20-Beta1").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: keeps only the highest qualifier for the same major.minor.patch")
    fun `backward - same triple keeps highest qualifier`() {
        val result =
            backwardSelector.select(listOf("2.3.0-Beta1", "2.3.0-RC").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.4.0"))
        assertEquals(listOf("2.3.0-RC").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: stable release takes precedence over RC for the same major.minor.patch")
    fun `backward - stable beats RC for same triple`() {
        val result = backwardSelector.select(listOf("2.3.0-RC", "2.3.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.4.0"))
        assertEquals(listOf("2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: snapshot current includes published patches at lower patch numbers in the same minor version")
    fun `backward - snapshot current includes published versions at lower patch in same minor version`() {
        val all = listOf("2.3.20", "2.4.0", "2.4.20").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.4.255-SNAPSHOT"))
        assertEquals(listOf("2.3.20", "2.4.0", "2.4.20").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: snapshot current excludes versions from future minor versions")
    fun `backward - snapshot current excludes versions from future minor versions`() {
        val all = listOf("2.3.0", "2.4.0", "2.5.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.4.255-SNAPSHOT"))
        assertEquals(listOf("2.3.0", "2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Backward: snapshot current selects prior minor versions when no versions are published in its own minor version")
    fun `backward - snapshot current with no published versions in its minor version`() {
        val all = listOf("2.1.0", "2.2.0", "2.3.0").map { KotlinToolingVersion(it) }
        val result = backwardSelector.select(all, KotlinToolingVersion("2.4.0-SNAPSHOT"))
        assertEquals(listOf("2.1.0", "2.2.0", "2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    // ── FORWARD ──────────────────────────────────────────────────

    @Test
    @DisplayName("Forward: returns empty when input is empty")
    fun `forward - empty input returns empty`() {
        assertTrue(forwardSelector.select(emptyList(), KotlinToolingVersion("2.3.0")).isEmpty())
    }

    @Test
    @DisplayName("Forward: includes current and the next minor version")
    fun `forward - takes current version plus 1 forward minor version`() {
        val all = listOf("2.2.0", "2.3.0", "2.4.0", "2.5.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0", "2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: includes only current version when no future minor versions exist")
    fun `forward - takes only current when no forward versions exist`() {
        val all = listOf("2.2.0", "2.3.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: crosses major version boundaries when counting minor versions")
    fun `forward - cross-major boundary`() {
        val all = listOf("2.9.0", "3.0.0", "3.1.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.9.0"))
        assertEquals(listOf("2.9.0", "3.0.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: picks only the nearest future minor version when multiple exist")
    fun `forward - takes only nearest forward version when multiple exist`() {
        val all = listOf("2.3.0", "2.4.0", "2.5.0", "2.6.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0", "2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: includes all patch versions within a selected minor version")
    fun `forward - all patch versions in forward minor version are included`() {
        val all = listOf("2.3.0", "2.3.10", "2.4.0", "2.4.5").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0", "2.3.10", "2.4.0", "2.4.5").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: returns minor versions in ascending order")
    fun `forward - minor versions sorted ascending`() {
        val all = listOf("2.3.0", "2.4.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.3.0", "2.4.0").map { KotlinToolingVersion(it) }, result)
    }

    @Test
    @DisplayName("Forward: SNAPSHOT is excluded since it is not published")
    fun `forward - SNAPSHOT excluded`() {
        val result =
            forwardSelector.select(listOf("2.4.0-SNAPSHOT", "2.4.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: DEV is excluded since it is not published")
    fun `forward - DEV excluded`() {
        val result =
            forwardSelector.select(listOf("2.4.0-dev-123", "2.4.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: STABLE, RC, and BETA versions are all included")
    fun `forward - STABLE RC and BETA are included`() {
        val all = listOf("2.4.0", "2.4.10-RC", "2.4.20-Beta1").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.4.0", "2.4.10-RC", "2.4.20-Beta1").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: keeps only the highest qualifier for the same major.minor.patch")
    fun `forward - same triple keeps highest qualifier`() {
        val result =
            forwardSelector.select(listOf("2.4.0-Beta1", "2.4.0-RC").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.4.0-RC").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: stable release takes precedence over RC for the same major.minor.patch")
    fun `forward - stable beats RC for same triple`() {
        val result =
            forwardSelector.select(listOf("2.4.0-RC", "2.4.0").map { KotlinToolingVersion(it) }, KotlinToolingVersion("2.3.0"))
        assertEquals(listOf("2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: snapshot current excludes older patches in its own minor version")
    fun `forward - snapshot current excludes older patches in same minor version`() {
        val all = listOf("2.3.10", "2.3.20-RC", "2.4.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.3.20-SNAPSHOT"))
        assertEquals(listOf("2.3.20-RC", "2.4.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }

    @Test
    @DisplayName("Forward: snapshot current skips to the next minor version when no versions are published in its own")
    fun `forward - snapshot current with no published versions in its minor version`() {
        val all = listOf("2.3.0", "2.5.0").map { KotlinToolingVersion(it) }
        val result = forwardSelector.select(all, KotlinToolingVersion("2.4.0-SNAPSHOT"))
        assertEquals(listOf("2.5.0").map { KotlinToolingVersion(it) }.toSet(), result.toSet())
    }
}
