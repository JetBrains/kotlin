/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.Test


class ImportAndCheckHighlighting : MultiplePluginVersionGradleImportingTestCase() {

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.40+")
    fun testMultiplatformLibrary() {
        importAndCheckHighlighting()
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.40+")
    fun testUnresolvedInMultiplatformLibrary() {
        importAndCheckHighlighting(false, false)
    }

    private fun importAndCheckHighlighting(testLineMarkers: Boolean = true, checkWarnings: Boolean = true) {
        val files = configureByFiles()
        importProject()
        val project = myTestFixture.project
        checkFiles(
            files.filter { it.extension == "kt" || it.extension == "java" },
            project,
            object : GradleDaemonAnalyzerTestCase(
                testLineMarkers = testLineMarkers,
                checkWarnings = checkWarnings,
                checkInfos = false,
                rootDisposable = testRootDisposable
            ) {}
        )
    }

    override fun testDataDirName(): String {
        return "importAndCheckHighlighting"
    }
}
