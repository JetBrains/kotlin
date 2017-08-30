/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.junit.Test
import java.io.File
import kotlin.reflect.KMutableProperty0

class GradleUpdateConfigurationQuickFixTest : GradleImportingTestCase() {
    private lateinit var codeInsightTestFixture: CodeInsightTestFixture

    fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/gradle/languageFeature/" + getTestName(true).substringBefore('_')

    override fun setUpFixtures() {
        myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).fixture
        codeInsightTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myTestFixture)
        codeInsightTestFixture.setUp()
    }

    override fun tearDownFixtures() {
        codeInsightTestFixture.tearDown()
        (this::codeInsightTestFixture as KMutableProperty0<CodeInsightTestFixture?>).set(null)
        myTestFixture = null
    }

    @Test fun testUpdateLanguageVersion() {
        doTest("Set module language version to 1.1")
    }

    @Test fun testUpdateApiVersion() {
        doTest("Set module API version to 1.1")
    }

    @Test fun testUpdateLanguageAndApiVersion() {
        doTest("Set module language version to 1.1")
    }

    @Test fun testEnableCoroutines() {
        doTest("Enable coroutine support in the current module")
    }

    @Test fun testAddKotlinReflect() {
        doTest("Add kotlin-reflect.jar to the classpath")
    }

    private fun doTest(intentionName: String) {
        val buildGradleVFile = createProjectSubFile("build.gradle", File(getTestDataPath(), "build.gradle").readText())
        importProject()
        runInEdtAndWait {
            val sourceVFile = createProjectSubFile("src/main/kotlin/src.kt", File(getTestDataPath(), "src.kt").readText())
            codeInsightTestFixture.configureFromExistingVirtualFile(sourceVFile)
            codeInsightTestFixture.launchAction(codeInsightTestFixture.findSingleIntention(intentionName))
            FileDocumentManager.getInstance().saveAllDocuments()
            checkResult(buildGradleVFile)
        }
    }

    private fun checkResult(file: VirtualFile) {
        val expectedPath = File(getTestDataPath(), "build.gradle.after")
        val expectedContent = StringUtil.convertLineSeparators(expectedPath.readText())
        val actualContent = StringUtil.convertLineSeparators(LoadTextUtil.loadText(file).toString())
        if (actualContent != expectedContent) {
            throw FileComparisonFailure("build.gradle doesn't match", expectedContent, actualContent, expectedPath.path)
        }
    }
}
