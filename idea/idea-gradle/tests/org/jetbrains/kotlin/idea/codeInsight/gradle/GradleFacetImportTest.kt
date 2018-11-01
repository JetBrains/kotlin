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

import org.junit.Test

class GradleFacetImportTest : GradleImportingTestCase() {
    private var isCreateEmptyContentRootDirectories = true

    override fun setUp() {
        super.setUp()
        isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
    }

    override fun tearDown() {
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        super.tearDown()
    }

    @Test
    fun testJvmImport() {
        loadProject("idea/testData/gradle/facets/jvmImport")

        checkFacet("idea/testData/gradle/facets/jvmImport", getModule("project_main"))
        checkFacet("idea/testData/gradle/facets/jvmImport", getModule("project_test"))
    }

    @Test
    fun testJvmImportWithPlugin() {
        loadProject("idea/testData/gradle/facets/jvmImportWithPlugin")
    }

    @Test
    fun testJvmImport_1_1_2() {
        loadProject("idea/testData/gradle/facets/jvmImport_1_1_2")

        checkFacet("idea/testData/gradle/facets/jvmImport_1_1_2", getModule("project_main"))
        checkFacet("idea/testData/gradle/facets/jvmImport_1_1_2", getModule("project_test"))
    }

    @Test
    fun testJvmImportWithCustomSourceSets() {
        loadProject("idea/testData/gradle/facets/jvmImportWithCustomSourceSets")

        checkFacet("idea/testData/gradle/facets/jvmImportWithCustomSourceSets", getModule("project_myMain"))
        checkFacet("idea/testData/gradle/facets/jvmImportWithCustomSourceSets", getModule("project_myTest"))
    }

    @Test
    fun testJvmImportWithCustomSourceSets_1_1_2() {
        loadProject("idea/testData/gradle/facets/jvmImportWithCustomSourceSets_1_1_2")

        checkFacet("idea/testData/gradle/facets/jvmImportWithCustomSourceSets_1_1_2", getModule("project_myMain"))
        checkFacet("idea/testData/gradle/facets/jvmImportWithCustomSourceSets_1_1_2", getModule("project_myTest"))
    }

    @Test
    fun testCoroutineImportByOptions() {
        loadProject("idea/testData/gradle/facets/coroutineImportByOptions")
        checkFacet("idea/testData/gradle/facets/coroutineImportByOptions", getModule("project_main"))
    }

    @Test
    fun testCoroutineImportByProperties() {
        loadProject("idea/testData/gradle/facets/coroutineImportByProperties")
        checkFacet("idea/testData/gradle/facets/coroutineImportByProperties", getModule("project_main"))
    }

    @Test
    fun testJsImport() {
        loadProject("idea/testData/gradle/facets/jsImport")
        checkFacet("idea/testData/gradle/facets/jsImport", getModule("project_main"))
    }

    @Test
    fun testJsImportTransitive() {
        loadProject("idea/testData/gradle/facets/jsImportTransitive")
        checkFacet("idea/testData/gradle/facets/jsImportTransitive", getModule("project_main"))
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        loadProject("idea/testData/gradle/facets/jsImportWithCustomSourceSets")
        checkFacet("idea/testData/gradle/facets/jsImportWithCustomSourceSets", getModule("project_myMain"))
        checkFacet("idea/testData/gradle/facets/jsImportWithCustomSourceSets", getModule("project_myTest"))
    }

    @Test
    fun testDetectOldJsStdlib() {
        loadProject("idea/testData/gradle/facets/detectOldJsStdlib")
        checkFacet("idea/testData/gradle/facets/detectOldJsStdlib", getModule("project_main"))
    }

    @Test
    fun testJvmImportByPlatformPlugin() {
        loadProject("idea/testData/gradle/facets/jvmImportByPlatformPlugin")
        checkFacet("idea/testData/gradle/facets/jvmImportByPlatformPlugin", getModule("project_main"))
    }

    @Test
    fun testJsImportByPlatformPlugin() {
        loadProject("idea/testData/gradle/facets/jsImportByPlatformPlugin")
        checkFacet("idea/testData/gradle/facets/jsImportByPlatformPlugin", getModule("project_main"))
    }

    @Test
    fun testCommonImportByPlatformPlugin() {
        loadProject("idea/testData/gradle/facets/commonImportByPlatformPlugin")
        checkFacet("idea/testData/gradle/facets/commonImportByPlatformPlugin", getModule("project_main"))
    }

    @Test
    fun testCommonImportByPlatformPlugin_SingleModule() {
        loadProject("idea/testData/gradle/facets/commonImportByPlatformPlugin_SingleModule")
        checkFacet("idea/testData/gradle/facets/commonImportByPlatformPlugin_SingleModule", getModule("project"))
    }

    @Test
    fun testJvmImportByKotlinPlugin() {
        loadProject("idea/testData/gradle/facets/jvmImportByKotlinPlugin")
        checkFacet("idea/testData/gradle/facets/jvmImportByKotlinPlugin", getModule("project_main"))
    }

    @Test
    fun testJsImportByKotlin2JsPlugin() {
        loadProject("idea/testData/gradle/facets/jsImportByKotlin2JsPlugin")
        checkFacet("idea/testData/gradle/facets/jsImportByKotlin2JsPlugin", getModule("project_main"))
    }

    @Test
    fun testArgumentEscaping() {
        loadProject("idea/testData/gradle/facets/argumentEscaping")
        checkFacet("idea/testData/gradle/facets/argumentEscaping", getModule("project_main"))
    }

    @Test
    fun testNoPluginsInAdditionalArgs() {
        loadProject("idea/testData/gradle/facets/noPluginsInAdditionalArgs")
        checkFacet("idea/testData/gradle/facets/noPluginsInAdditionalArgs", getModule("project_main"))
    }

    @Test
    fun testNoArgInvokeInitializers() {
        loadProject("idea/testData/gradle/facets/noArgInvokeInitializers")
        checkFacet("idea/testData/gradle/facets/noArgInvokeInitializers", getModule("project_main"))
    }

    @Test
    fun testAndroidGradleJsDetection() {
        loadProject("idea/testData/gradle/facets/androidGradleJsDetection")
        checkFacet("idea/testData/gradle/facets/androidGradleJsDetection", getModule("js-module"))
    }

    @Test
    fun testKotlinAndroidPluginDetection() {
        loadProject("idea/testData/gradle/facets/kotlinAndroidPluginDetection")
        checkFacet("idea/testData/gradle/facets/kotlinAndroidPluginDetection", getModule("project"))
    }

    @Test
    fun testNoFacetInModuleWithoutKotlinPlugin() {
        loadProject("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin")
        checkFacet("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin", getModule("gr01_main"))
        checkFacet("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin", getModule("gr01_test"))
        checkFacet("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin", getModule("m1_main"))
        checkFacet("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin", getModule("m1_test"))
    }

    @Test
    fun testClasspathWithDependenciesImport() {
        loadProject("idea/testData/gradle/facets/classpathWithDependenciesImport")
        checkFacet("idea/testData/gradle/facets/classpathWithDependenciesImport", getModule("project_main"))
    }

    @Test
    fun testDependenciesClasspathImport() {
        loadProject("idea/testData/gradle/facets/dependenciesClasspathImport")
        checkFacet("idea/testData/gradle/facets/dependenciesClasspathImport", getModule("project_main"))
    }

    @Test
    fun testImplementsDependency() {
        loadProject("idea/testData/gradle/facets/implementsDependency")

        checkFacet("idea/testData/gradle/facets/implementsDependency", getModule("MultiTest-jvm_main"))
        checkFacet("idea/testData/gradle/facets/implementsDependency", getModule("MultiTest-jvm_test"))
        checkFacet("idea/testData/gradle/facets/implementsDependency", getModule("MultiTest-js_main"))
        checkFacet("idea/testData/gradle/facets/implementsDependency", getModule("MultiTest-jvm_main"))
    }

    @Test
    fun testImplementsDependencyWithCustomSourceSets() {
        loadProject("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets")

        checkFacet("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets", getModule("MultiTest-jvm_main"))
        checkFacet("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets", getModule("MultiTest-jvm_test"))
        checkFacet("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets", getModule("MultiTest-js_main"))
        checkFacet("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets", getModule("MultiTest-jvm_main"))
    }

    @Test
    fun testAPIVersionExceedingLanguageVersion() {
        loadProject("idea/testData/gradle/facets/APIVersionExceedingLanguageVersion")
        checkFacet("idea/testData/gradle/facets/APIVersionExceedingLanguageVersion", getModule("project_main"))
    }

    @Test
    fun testCommonArgumentsImport() {
        loadProject("idea/testData/gradle/facets/commonArgumentsImport")
        checkFacet("idea/testData/gradle/facets/commonArgumentsImport", getModule("project_main"))
        checkFacet("idea/testData/gradle/facets/commonArgumentsImport", getModule("project_test"))
    }

    @Test
    fun testInternalArgumentsFacetImporting() {
        loadProject("idea/testData/gradle/facets/internalArgumentsFacetImporting")
        checkFacet("idea/testData/gradle/facets/internalArgumentsFacetImporting", getModule("project_main"))
    }

    @Test
    fun testNoFriendPathsAreShown() {
        loadProject("idea/testData/gradle/facets/noFriendPathsAreShown")
        checkFacet("idea/testData/gradle/facets/noFriendPathsAreShown", getModule("project_main"))
    }
}
