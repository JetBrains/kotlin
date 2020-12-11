/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.common

import org.jetbrains.kotlin.idea.perf.*
import org.jetbrains.kotlin.idea.perf.util.TeamCity
import org.jetbrains.kotlin.idea.testFramework.ProjectOpenAction

abstract class AbstractWholeProjectPerformanceComparisonTest : AbstractPerformanceProjectsTest() {
    abstract val testPrefix: String

    abstract fun getWarmUpProject(): WarmUpProject

    override fun setUp() {
        super.setUp()
        getWarmUpProject().warmUp(this)
    }

    protected fun doTestRustPlugin() {
        TeamCity.suite("$testPrefix Rust plugin") {
            Stats("$testPrefix Rust plugin").use {
                perfOpenRustPluginProject(it)

                val filesToHighlight = arrayOf(
                    "src/main/kotlin/org/rust/ide/inspections/RsExternalLinterInspection.kt",
                    "src/main/kotlin/org/rust/ide/injected/RsDoctestLanguageInjector.kt",
                    "src/main/kotlin/org/rust/cargo/runconfig/filters/RegexpFileLinkFilter.kt",
                )

                filesToHighlight.forEach { file -> perfHighlightFile(file, stats = it) }
            }
        }
    }

    private fun perfOpenRustPluginProject(stats: Stats) {
        myProject = perfOpenProject(
            name = "intellijRustPlugin",
            stats = stats,
            note = "",
            path = "../intellij-rust",
            openAction = ProjectOpenAction.GRADLE_PROJECT,
            fast = true
        )
    }
}

