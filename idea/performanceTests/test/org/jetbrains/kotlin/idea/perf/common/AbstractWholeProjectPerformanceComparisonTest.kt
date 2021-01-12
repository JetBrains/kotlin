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

    protected fun doTestRustPluginHighlighting() {
        TeamCity.suite("$testPrefix highlighting in Rust plugin") {
            Stats("$testPrefix highlighting in Rust plugin").use { stat ->
                perfOpenRustPluginProject(stat)

                val filesToHighlight = arrayOf(
                    "src/main/kotlin/org/rust/ide/inspections/RsExternalLinterInspection.kt",
                    "src/main/kotlin/org/rust/ide/injected/RsDoctestLanguageInjector.kt",
                    FILE_NAMES.REGEXP_FILE_LINK_FILTER,
                    "src/main/kotlin/org/rust/cargo/util/CargoOptions.kt",
                    "src/main/kotlin/org/rust/lang/core/macros/MacroExpansionManager.kt",
                    FILE_NAMES.NAME_RESOLUTION
                )

                filesToHighlight.forEach { file -> perfHighlightFileEmptyProfile(file, stats = stat) }
            }
        }
    }

    protected fun doTestRustPluginCompletion() {
        TeamCity.suite("$testPrefix completion in Rust plugin") {
            Stats("$testPrefix completion in Rust plugin").use { stat ->
                perfOpenRustPluginProject(stat)

                perfTypeAndAutocomplete(
                    stat,
                    fileName = FILE_NAMES.REGEXP_FILE_LINK_FILTER,
                    marker = "fun applyFilter(line: String, entireLength: Int): Filter.Result? {",
                    insertString = "val a = l",
                    highlightFileBeforeStartTyping = true,
                    lookupElements = listOf("line"),
                    note = "in-method completion"
                )

                perfTypeAndAutocomplete(
                    stat,
                    fileName = FILE_NAMES.NAME_RESOLUTION,
                    marker = "private data class ImplicitStdlibCrate(val name: String, val crateRoot: RsFile)",
                    insertString = "\nval a = ",
                    highlightFileBeforeStartTyping = true,
                    lookupElements = listOf("processAssocTypeVariants"),
                    note = "top-level completion"
                )

                perfTypeAndAutocomplete(
                    stat,
                    fileName = FILE_NAMES.NAME_RESOLUTION,
                    marker = "testAssert { cameFrom.context == scope }",
                    highlightFileBeforeStartTyping = true,
                    insertString = "\nval a = s",
                    lookupElements = listOf("scope"),
                    note = "in big method in big file completion"
                )
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

    private object FILE_NAMES {
        const val NAME_RESOLUTION = "src/main/kotlin/org/rust/lang/core/resolve/NameResolution.kt"
        const val REGEXP_FILE_LINK_FILTER = "src/main/kotlin/org/rust/cargo/runconfig/filters/RegexpFileLinkFilter.kt"
    }
}

