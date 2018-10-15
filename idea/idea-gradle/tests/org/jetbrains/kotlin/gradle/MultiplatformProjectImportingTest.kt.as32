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
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.facetSettings
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test

class MultiplatformProjectImportingTest : GradleImportingTestCase() {
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

    @Test
    fun testPlatformToCommonDependency() {
        val files = configureByFiles()
        importProject()

        assertModuleModuleDepScope("jvm", "common", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js", "common", DependencyScope.COMPILE)

        assertProductionOnTestDependency("jvm", "common", false)
        assertProductionOnTestDependency("js", "common", false)

        val commonTestFile = files.find { it.path.contains("common") }!!
    }

    @Test
    fun testPlatformToCommonExpectedByDependency() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("jvm", "common1", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm", "common2", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js", "common1", DependencyScope.COMPILE)
        assertNoDepForModule("js", "common2")
    }

    @Test
    fun testPlatformToCommonExpectedByDependencyInComposite() {
        configureByFiles()
        importProject()

        TestCase.assertEquals(listOf("common"), facetSettings("jvm").implementedModuleNames)
        TestCase.assertEquals(listOf("common"), facetSettings("js").implementedModuleNames)

        assertModuleModuleDepScope("jvm", "common", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js", "common", DependencyScope.COMPILE)
    }

    @Test
    fun testPlatformToCommonDependencyRoot() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("jvm", "foo", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js", "foo", DependencyScope.COMPILE)
    }

    @Test
    fun testMultiProject() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope("jvm-app", "common-app", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app", "common-lib", DependencyScope.COMPILE)
        assertModuleModuleDepScope(
            "jvm-app",
            "jvm-lib",
            DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
        )

        assertModuleModuleDepScope("js-app", "common-app", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js-app", "common-lib", DependencyScope.COMPILE)
        assertModuleModuleDepScope(
            "js-app",
            "js-lib",
            DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
        )
    }

    @Test
    fun testDependenciesReachableViaImpl() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope(
            "jvm-app",
            "jvm-lib2",
            DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
        )
        assertModuleModuleDepScope(
            "jvm-app",
            "jvm-lib1",
            DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
        )
        assertModuleModuleDepScope("jvm-app","common-lib1", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app", "common-lib2",DependencyScope.COMPILE)
    }

    @Test
    fun testTransitiveImplement() {
        configureByFiles()

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet

        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)

            assertModuleModuleDepScope(
                "project3",
                "project2",
                DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
            )
            assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)
            assertModuleModuleDepScope(
                "project3",
                "project2",
                DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
            )
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

            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)
            assertModuleModuleDepScope(
                "project3",
                "project2",
                DependencyScope.COMPILE, DependencyScope.RUNTIME, DependencyScope.PROVIDED, DependencyScope.TEST
            )
            assertNoDepForModule("project3", "project1")

            TestCase.assertEquals(
                    listOf("jar:///project2/build/libs/project2-jar.jar!/"),
                    getDependencyLibraryUrls("project3")
            )

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            /*
             * Note that currently such dependencies can't be imported correctly in "No separate module per source set" mode
             * due to IDEA importer limitations
             */
            assertModuleModuleDepScope("project2", "project1", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project3", "project2", DependencyScope.TEST, DependencyScope.PROVIDED, DependencyScope.RUNTIME)
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
            projectPath + "/project2/build/classes/test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2"))!!.configuration.settings.testOutputPath)
        )
    }

    @Test
    fun testJsProductionOutputFile() {
        configureByFiles()
        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/main/project2.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2"))!!.configuration.settings.productionOutputPath)
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
            projectPath + "/project2/build/classes/test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project2"))!!.configuration.settings.testOutputPath)
        )
    }

    override fun testDataDirName(): String {
        return "multiplatform"
    }
}