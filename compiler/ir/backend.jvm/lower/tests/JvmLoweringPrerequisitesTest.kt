/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.test

import org.jetbrains.kotlin.backend.common.LoweringPrerequisitesTest
import org.jetbrains.kotlin.backend.jvm.getJvmLoweringPhaseListsForTests
import kotlin.test.Test

@Suppress("JUnitTestCaseWithNoTests")
class JvmLoweringPrerequisitesTest : LoweringPrerequisitesTest() {
    @Test
    fun checkPrerequisites() {
        val allPhases = getJvmLoweringPhaseListsForTests().flatten()
        checkPrerequisites(allPhases)
    }
}
