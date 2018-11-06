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
    @Test
    fun testJvmImport() {
        doTest("idea/testData/gradle/facets/jvmImport")
    }

    @Test
    fun testJvmImportWithPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportWithPlugin")
    }

    @Test
    fun testJvmImport_1_1_2() {
        doTest("idea/testData/gradle/facets/jvmImport_1_1_2")
    }

    @Test
    fun testJvmImportWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/jvmImportWithCustomSourceSets")
    }

    @Test
    fun testJvmImportWithCustomSourceSets_1_1_2() {
        doTest("idea/testData/gradle/facets/jvmImportWithCustomSourceSets_1_1_2")
    }

    @Test
    fun testCoroutineImportByOptions() {
        doTest("idea/testData/gradle/facets/coroutineImportByOptions")
    }

    @Test
    fun testCoroutineImportByProperties() {
        doTest("idea/testData/gradle/facets/coroutineImportByProperties")
    }

    @Test
    fun testJsImport() {
        doTest("idea/testData/gradle/facets/jsImport")
    }

    @Test
    fun testJsImportTransitive() {
        doTest("idea/testData/gradle/facets/jsImportTransitive")
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/jsImportWithCustomSourceSets")
    }

    @Test
    fun testDetectOldJsStdlib() {
        doTest("idea/testData/gradle/facets/detectOldJsStdlib")
    }

    @Test
    fun testJvmImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportByPlatformPlugin")
    }

    @Test
    fun testJsImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/jsImportByPlatformPlugin")
    }

    @Test
    fun testCommonImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/commonImportByPlatformPlugin")
    }

    @Test
    fun testCommonImportByPlatformPlugin_SingleModule() {
        doTest("idea/testData/gradle/facets/commonImportByPlatformPlugin_SingleModule")
    }

    @Test
    fun testJvmImportByKotlinPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportByKotlinPlugin")
    }

    @Test
    fun testJsImportByKotlin2JsPlugin() {
        doTest("idea/testData/gradle/facets/jsImportByKotlin2JsPlugin")
    }

    @Test
    fun testArgumentEscaping() {
        doTest("idea/testData/gradle/facets/argumentEscaping")
    }

    @Test
    fun testNoPluginsInAdditionalArgs() {
        doTest("idea/testData/gradle/facets/noPluginsInAdditionalArgs")
    }

    @Test
    fun testNoArgInvokeInitializers() {
        doTest("idea/testData/gradle/facets/noArgInvokeInitializers")
    }

    @Test
    fun testAndroidGradleJsDetection() {
        doTest("idea/testData/gradle/facets/androidGradleJsDetection")
    }

    @Test
    fun testKotlinAndroidPluginDetection() {
        doTest("idea/testData/gradle/facets/kotlinAndroidPluginDetection")
    }

    @Test
    fun testNoFacetInModuleWithoutKotlinPlugin() {
        doTest("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin")
    }

    @Test
    fun testClasspathWithDependenciesImport() {
        doTest("idea/testData/gradle/facets/classpathWithDependenciesImport")
    }

    @Test
    fun testDependenciesClasspathImport() {
        doTest("idea/testData/gradle/facets/dependenciesClasspathImport")
    }

    @Test
    fun testImplementsDependency() {
        doTest("idea/testData/gradle/facets/implementsDependency")
    }

    @Test
    fun testImplementsDependencyWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets")
    }

    @Test
    fun testAPIVersionExceedingLanguageVersion() {
        doTest("idea/testData/gradle/facets/APIVersionExceedingLanguageVersion")
    }

    @Test
    fun testCommonArgumentsImport() {
        doTest("idea/testData/gradle/facets/commonArgumentsImport")
    }

    @Test
    fun testInternalArgumentsFacetImporting() {
        doTest("idea/testData/gradle/facets/internalArgumentsFacetImporting")
    }

    @Test
    fun testNoFriendPathsAreShown() {
        doTest("idea/testData/gradle/facets/noFriendPathsAreShown")
    }
}
