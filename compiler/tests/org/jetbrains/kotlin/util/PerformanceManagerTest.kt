/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PerformanceManagerTest {
    @Test
    fun childInheritsExtendedStatisticsModeWithoutGlobalCounters() {
        for (enabled in listOf(false, true)) {
            for (start in listOf(false, true)) {
                val parent = PerformanceManagerImpl(NativePlatforms.unspecifiedNativePlatform, "test")
                if (enabled) {
                    parent.enableExtendedStats()
                }

                val child = PerformanceManagerImpl.createChildIfNeeded(parent, start)!!
                assertEquals(enabled, child.isExtendedStatsEnabled, "enabled=$enabled, start=$start")
                child.notifyCompilationFinished()
                assertTrue(child.unitStats.gcStats.isEmpty())
                assertNull(child.unitStats.jitTimeMillis)
            }
        }
    }
}
