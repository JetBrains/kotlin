/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.*
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.junit.Test
import java.io.File

class NewMultiplatformKaptProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {
    override fun isApplicableTest(): Boolean {
        val isOldGradlePlugin = gradleKotlinPluginVersion != MINIMAL_SUPPORTED_VERSION
                && StringUtil.compareVersionNumbers(gradleKotlinPluginVersion, "1.3.40") < 0

        return !isOldGradlePlugin && !gradleVersion.startsWith("3.")
    }

    @Test
    fun testKaptPaths() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            project,
            projectPath,
            exhaustiveModuleList = true,
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false
        ) {
            module("project")

            module("project_main")
            module("project_test")

            module("project_commonMain")
            module("project_commonTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
            }

            module("project_jvmMain") {
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
                val basePath = File(projectPath).parentFile.path.replace(File.separatorChar, '/')
                val actualSourceRoots = module.sourceRoots.map { it.path.replace(basePath, "") }.sorted()

                val expectedSourceRoots = listOf(
                    "/project/build/generated/source/kapt/main",
                    "/project/build/generated/source/kaptKotlin/main",
                    "/project/src/jvmMain/java",
                    "/project/src/jvmMain/kotlin",
                    "/project/src/jvmMain/resources"
                )

                assertEquals(expectedSourceRoots, actualSourceRoots)
            }

            module("project_jvmTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
                moduleDependency("project_commonTest", DependencyScope.TEST)
                moduleDependency("project_jvmMain", DependencyScope.TEST)
            }
        }
    }

    override fun importProject() {
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject()
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        }
    }

    override fun testDataDirName(): String {
        return "newMultiplatformImport"
    }
}