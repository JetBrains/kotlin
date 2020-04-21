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

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.ModuleOrderEntryImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.facetSettings
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test

class MultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {

    private fun legacyMode() = gradleVersion.split(".")[0].toInt() < 4
    private fun getDependencyLibraryUrls(moduleName: String) =
        getRootManager(moduleName)
            .orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .flatMap { it.getUrls(OrderRootType.CLASSES).map { it.replace(projectPath, "") } }

    private fun assertProductionOnTestDependency(moduleName: String, depModuleName: String, expected: Boolean) {
        val depOrderEntry = getModule(moduleName)
            .rootManager
            .orderEntries
            .filterIsInstance<ModuleOrderEntryImpl>()
            .first { it.moduleName == depModuleName }
        assert(depOrderEntry.isProductionOnTestDependency == expected)
    }

    private fun assertFileInModuleScope(file: VirtualFile, moduleName: String) {
        assert(getModule(moduleName).getModuleWithDependenciesAndLibrariesScope(true).contains(file))
    }

    @Test
    fun testPlatformToCommonDependency() {
        val files = configureByFiles()
        importProject()

        assertModuleModuleDepScope("jvm_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "common_test", DependencyScope.COMPILE)

        assertProductionOnTestDependency("jvm_main", "common_main", false)
        assertProductionOnTestDependency("jvm_test", "common_test", true)
        assertProductionOnTestDependency("js_main", "common_main", false)
        assertProductionOnTestDependency("js_test", "common_test", true)

        val commonTestFile = files.find { it.path.contains("common") }!!
        assertFileInModuleScope(commonTestFile, "jvm_test")
        assertFileInModuleScope(commonTestFile, "js_test")
    }

    @Test
    fun testPlatformToCommonExpectedByDependency() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("jvm_main", "common1_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_main", "common2_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common1_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common2_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "common1_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "common1_test", DependencyScope.COMPILE)
        assertNoModuleDepForModule("js_main", "common2_main")
        assertNoModuleDepForModule("js_test", "common2_test")
    }

    @Test
    fun testPlatformToCommonDependencyRoot() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("jvm_main", "foo_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "foo_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "foo_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "foo_test", DependencyScope.COMPILE)
    }

    @Test
    fun testMultiProject() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope("jvm-app_main", "common-app_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "common-lib_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "jvm-lib_main", DependencyScope.COMPILE)

        assertModuleModuleDepScope("js-app_main", "common-app_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js-app_main", "common-lib_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js-app_main", "js-lib_main", DependencyScope.COMPILE)
    }

    @Test
    fun testDependenciesReachableViaImpl() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope("jvm-app_main", "jvm-lib2_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "jvm-lib1_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "common-lib1_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "common-lib2_main", DependencyScope.COMPILE)

        assertModuleModuleDepScope("jvm-app_test", "jvm-lib2_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_test", "jvm-lib1_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_test", "common-lib1_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_test", "common-lib2_test", DependencyScope.COMPILE)
    }

    @Test
    fun testTransitiveImplement() {
        configureByFiles()

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet

        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project1_test", "project1_main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project2_main", "project1_main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project2_test", "project2_main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project2_test", "project1_test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project2_test", "project1_main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project2_custom", "project1_custom", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project3_main", "project2_main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_main", "project1_main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project3_test", "project3_main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_test", "project2_test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_test", "project2_main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_test", "project1_test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_test", "project1_main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project3_custom", "project1_custom", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_custom", "project2_main", DependencyScope.COMPILE)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)
            if (legacyMode()) {
                // This data is obtained from Gradle model. Actually RUNTIME+TEST+PROVIDED == COMPILE, thus this difference does not matter for user
                assertModuleModuleDepScope("project3", "project2", DependencyScope.RUNTIME, DependencyScope.TEST, DependencyScope.PROVIDED)
            } else {
                assertModuleModuleDepScope("project3", "project2", DependencyScope.COMPILE)
            }
            assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    fun testTransitiveImplementWithNonDefaultConfig() {
        configureByFiles()

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet

        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project2_main", "project1_main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3_main", "project2_main", DependencyScope.COMPILE)
            assertNoModuleDepForModule("project3_main", "project1_main")

            TestCase.assertEquals(
                    listOf("jar:///project2/build/libs/project2-jar.jar!/"),
                    getDependencyLibraryUrls("project3_main")
            )

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            /*
             * Note that currently such dependencies can't be imported correctly in "No separate module per source set" mode
             * due to IDEA importer limitations
             */
            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)
            if (legacyMode()) {
                assertModuleModuleDepScope("project3", "project2", DependencyScope.TEST, DependencyScope.PROVIDED, DependencyScope.RUNTIME)
            } else {
                assertModuleModuleDepScope("project3", "project2", DependencyScope.COMPILE)
            }

            assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)

            TestCase.assertEquals(
                    emptyList<String>(),
                    getDependencyLibraryUrls("project3")
            )
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    fun testTransitiveImplementWithAndroid() {
        configureByFiles()

        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet
        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project3", "project2", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("project1"), facetSettings("project2").implementedModuleNames)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("project3", "project2", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("project1"), facetSettings("project2").implementedModuleNames)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    fun simpleAndroidAppWithCommonModule() {
        configureByFiles()

        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet
        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("app", "cmn", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("cmn"), facetSettings("jvm").implementedModuleNames)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("app", "cmn", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("cmn"), facetSettings("jvm").implementedModuleNames)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    fun testJsTestOutputFile() {
        configureByFiles()

        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2_main"))!!.configuration.settings.testOutputPath)
        )
        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2_test"))!!.configuration.settings.testOutputPath)
        )
    }

    @Test
    fun testJsProductionOutputFile() {
        configureByFiles()
        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}main/project2.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2_main"))!!.configuration.settings.productionOutputPath)
        )
        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}main/project2.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2_test"))!!.configuration.settings.productionOutputPath)
        )
    }

    @Test
    fun testJsTestOutputFileInProjectWithAndroid() {
        configureByFiles()
        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2"))!!.configuration.settings.testOutputPath)
        )
    }

    override fun testDataDirName(): String {
        return "multiplatform"
    }
}