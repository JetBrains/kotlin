/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import org.jetbrains.kotlin.gradle.GradleDaemonAnalyzerTestCase
import org.jetbrains.kotlin.gradle.checkFiles
import org.jetbrains.kotlin.idea.run.KotlinGradleRunConfiguration
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.junit.Test

class GradleTestRunConfigurationAndHighlightingTest : GradleImportingTestCase() {
    @Test
    fun testExpectClassWithTests() {
        doTest()
    }

    @Test
    fun testKotlinJUnitSettings() {
        doTest()
    }

    @Test
    fun preferredConfigurations() {
        doTest()
    }

    private fun doTest() {
        val files = importProjectFromTestData()
        val project = myTestFixture.project

        checkFiles(
            files.filter { it.extension == "kt" },
            project,
            object : GradleDaemonAnalyzerTestCase(testLineMarkers = true, checkWarnings = true, checkInfos = false) {
                override fun renderAdditionalAttributeForTag(tag: TagsTestDataUtil.TagInfo<*>): String? {
                    val lineMarkerInfo = tag.data as? LineMarkerInfo<*> ?: return null

                    // Hacky way to check if it's test line-marker info. Can't rely on extractConfigurationsFromContext returning no
                    // suitable configurationsFromContext, because it basically works on offsets, so if for some range we have two
                    // line markers - one with tests, and one without, - then we'll get proper ConfigurationFromContext for both
                    if ("Run Test" !in lineMarkerInfo.lineMarkerTooltip.orEmpty()) return null

                    val kotlinConfigsFromContext = lineMarkerInfo.extractConfigurationsFromContext()
                        .filter { it.configuration is KotlinGradleRunConfiguration }

                    if (kotlinConfigsFromContext.isEmpty()) return "settings=\"Nothing here\""

                    val configFromContext = kotlinConfigsFromContext.single() // can we have more than one?

                    return "settings=\"${configFromContext.renderDescription().replace("\"", "\\\"")}\""
                }
            }
        )
    }

    private fun ConfigurationFromContext.renderDescription(): String {
        val configuration = configuration as KotlinGradleRunConfiguration

        val location = PsiLocation(sourceElement)
        val context = ConfigurationContext.createEmptyContextForLocation(location)

        var result: String? = null

        // We can not use settings straight away, because exact settings are determined only after 'onFirstRun'
        // (see MultiplatformTestTasksChooser)
        onFirstRun(context) {
            result = configuration.settings.toString()
        }

        return result!!
    }

    private fun LineMarkerInfo<*>.extractConfigurationsFromContext(): List<ConfigurationFromContext> {
        val location = PsiLocation(element)

        // TODO(dsavvinov): consider getting proper context somehow
        val context = ConfigurationContext.createEmptyContextForLocation(location)

        return context.configurationsFromContext.orEmpty()
    }

    override fun testDataDirName(): String = "testRunConfigurations"
}