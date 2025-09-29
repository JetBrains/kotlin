/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.test

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.LoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.getJvmLoweringPhaseListsForTests
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

private typealias PassId = Class<out ModuleLoweringPass>

@Suppress("JUnitTestCaseWithNoTests")
class JvmLoweringPrerequisitesTest {
    @Test
    fun checkPrerequisites() {
        val allPhases = getJvmLoweringPhaseListsForTests().flatten()

        val createdPhases = mutableListOf<PassId>()
        val unsatisfiedPrerequisites = mutableListOf<Pair<PassId, PassId>>()
        for (phase in allPhases) {
            phase as? LoweringPhase<*, *, *>
                ?: fail("Unexpected phase type: ${phase::class.simpleName}")
            val loweringClass = phase.loweringClass
            val prerequisites = loweringClass.getDeclaredAnnotation(PhasePrerequisites::class.java)?.value.orEmpty()
            for (prerequisite in prerequisites) {
                if (prerequisite.java !in createdPhases) {
                    unsatisfiedPrerequisites.add(loweringClass to prerequisite.java)
                }
            }
            createdPhases.add(loweringClass)
        }

        assertTrue(
            unsatisfiedPrerequisites.isEmpty(),
            "The following phases have unsatisfied prerequisites:\n\n" +
                    unsatisfiedPrerequisites.joinToString("\n", postfix = "\n") { (phase, prerequisite) ->
                        "${phase.simpleName} -> ${prerequisite.simpleName}"
                    },
        )
    }
}
