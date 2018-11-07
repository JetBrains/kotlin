/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.inspections.gradle.DeprecatedGradleDependencyInspection
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentKotlinGradleVersionInspection
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentStdlibGradleVersionInspection
import org.jetbrains.kotlin.idea.inspections.gradle.GradleKotlinxCoroutinesDeprecationInspection
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Assert
import org.junit.Test

class GradleInspectionTest : GradleImportingTestCase() {
    @Test
    fun testDifferentStdlibGradleVersion() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.0.2) is not the same as library version (1.0.3)", problems.single())
    }

    @Test
    fun testDifferentStdlibGradleVersionWithImplementation() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.0.2) is not the same as library version (1.0.3)", problems.single())
    }

    @Test
    fun testDifferentStdlibJre7GradleVersion() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.1.0-beta-17) is not the same as library version (1.1.0-beta-22)", problems.single())
    }

    @Test
    fun testDifferentStdlibJdk7GradleVersion() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.1.0-beta-17) is not the same as library version (1.1.0-beta-22)", problems.single())
    }

    @Test
    fun testDifferentStdlibGradleVersionWithVariables() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.0.1) is not the same as library version (1.0.3)", problems.single())
    }

    @Test
    fun testDifferentKotlinGradleVersion() {
        val tool = DifferentKotlinGradleVersionInspection()
        tool.testVersionMessage = "\$PLUGIN_VERSION"
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals(
            "Kotlin version that is used for building with Gradle (1.0.1) differs from the one bundled into the IDE plugin (\$PLUGIN_VERSION)",
            problems.single()
        )
    }

    @Test
    fun testJreInOldVersion() {
        val tool = DeprecatedGradleDependencyInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.isEmpty())
    }

    @Test
    fun testJreIsDeprecated() {
        val tool = DeprecatedGradleDependencyInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals(
            "kotlin-stdlib-jre7 is deprecated since 1.2.0 and should be replaced with kotlin-stdlib-jdk7",
            problems.single()
        )
    }

    @Test
    fun testJreIsDeprecatedWithImplementation() {
        val tool = DeprecatedGradleDependencyInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals(
            "kotlin-stdlib-jre7 is deprecated since 1.2.0 and should be replaced with kotlin-stdlib-jdk7",
            problems.single()
        )
    }

    @TargetVersions("4.9+")
    @Test
    fun testJreIsDeprecatedWithoutImplicitVersion() {
        val tool = DeprecatedGradleDependencyInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals(
            "kotlin-stdlib-jre8 is deprecated since 1.2.0 and should be replaced with kotlin-stdlib-jdk8",
            problems.single()
        )
    }

    @Test
    fun testNoDifferentStdlibCommonGradleVersion() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun testNoDifferentStdlibJdk7GradleVersion() {
        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun testObsoleteCoroutinesUsage() {
        val tool = GradleKotlinxCoroutinesDeprecationInspection()
        val problems = getInspectionResultFromTestDataProject(tool)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals(
            "Library should be updated to be compatible with Kotlin 1.3",
            problems.single()
        )
    }

    private fun getInspectionResultFromTestDataProject(tool: LocalInspectionTool): List<String> {
        val buildGradle = importProjectFromTestData().find { it.name == "build.gradle" }!!
        return getInspectionResult(tool, buildGradle)
    }

    private fun getInspectionResult(tool: LocalInspectionTool, file: VirtualFile): List<String> {
        val resultRef = Ref<List<String>>()
        invokeTestRunnable {
            val presentation = runInspection(tool, myProject, listOf(file))

            val foundProblems = presentation.problemElements
                .values
                .mapNotNull { it as? ProblemDescriptorBase }
                .map { it.descriptionTemplate }

            resultRef.set(foundProblems)
        }

        return resultRef.get()
    }

    override fun testDataDirName(): String {
        return "inspections"
    }
}