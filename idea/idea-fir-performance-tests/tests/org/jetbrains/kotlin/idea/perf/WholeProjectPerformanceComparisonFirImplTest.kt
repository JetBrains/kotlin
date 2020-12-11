/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.common.AbstractWholeProjectPerformanceComparisonTest

class WholeProjectPerformanceComparisonFirImplTest : AbstractWholeProjectPerformanceComparisonTest() {
    override val testPrefix: String = "FIR"
    override fun getWarmUpProject(): WarmUpProject = warmUpProject

    fun testRustPlugin() {
        doTestRustPlugin()
    }

    companion object {
        private val hwStats: Stats = Stats("FIR warmup project")
        private val warmUpProject = WarmUpProject(hwStats)
    }
}