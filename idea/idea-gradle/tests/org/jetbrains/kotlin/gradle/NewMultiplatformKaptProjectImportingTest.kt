/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.ModulesOrderEnumerator
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.junit.Test
import java.io.File

class NewMultiplatformKaptProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {
    override fun isApplicableTest(): Boolean {
        val isOldGradlePlugin = gradleKotlinPluginVersion != MINIMAL_SUPPORTED_VERSION
                && VersionComparatorUtil.compare(gradleKotlinPluginVersion, "1.3.40") < 0

        return !isOldGradlePlugin && VersionComparatorUtil.compare(gradleVersion, "4.0") >= 0
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
            exhaustiveDependencyList = false,
            exhaustiveTestsList = false
        ) {
            module("project")

            // Importing module per source set is not supported in Android Studio
            if (KaptImportingTest().isAndroidStudio()) {
                module("project")
            } else {
                module("project_main")
                module("project_test")
            }

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

    @Test
    fun testRuntimeClasspath() {
        configureByFiles()

        val projectPath = this.projectPath

        val expectedRoots = listOf(
            "build/tmp/kapt3/classes/main",
            "build/classes/java/main",
            "build/classes/kotlin/jvm/main",
            "build/processedResources/jvm/main"
        ).map {
            File(projectPath, it).apply { mkdirs() }
        }

        importProject()

        val jvmMainModule = ModuleManager.getInstance(project).modules.first { it.name == "project_jvmMain" }

        val enumerator = ModulesOrderEnumerator(listOf(jvmMainModule))
        val roots = enumerator.classesRoots

        fun isRootPresent(file: File) = roots.any { it.path == ExternalSystemApiUtil.toCanonicalPath(file.path) }

        val missingRoots = expectedRoots.filter { !isRootPresent(it) }

        assert(missingRoots.isEmpty()) {
            "Missing roots found: " + missingRoots.joinToString()
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