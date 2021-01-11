/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.common.AbstractWholeProjectPerformanceComparisonTest

class WholeProjectPerformanceComparisonFE10ImplTest : AbstractWholeProjectPerformanceComparisonTest() {
    override val testPrefix: String = "FE10"
    override fun getWarmUpProject(): WarmUpProject = warmUpProject

    fun testRustPluginHighlighting() = doTestRustPluginHighlighting()
    fun testRustPluginCompletion() = doTestRustPluginCompletion()


    companion object {
        private val hwStats: Stats = Stats("FE10 warmup project")
        private val warmUpProject = WarmUpProject(hwStats)
    }
}