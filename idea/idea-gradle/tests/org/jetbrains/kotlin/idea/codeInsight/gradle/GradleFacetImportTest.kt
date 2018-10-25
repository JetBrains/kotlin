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

import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import junit.framework.TestCase
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootMap
import org.jetbrains.kotlin.idea.configuration.allConfigurators
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import org.junit.Test
import java.util.*

internal fun GradleImportingTestCase.facetSettings(moduleName: String) = KotlinFacet.get(getModule(moduleName))!!.configuration.settings

internal val GradleImportingTestCase.facetSettings: KotlinFacetSettings
    get() = facetSettings("project_main")

internal val GradleImportingTestCase.testFacetSettings: KotlinFacetSettings
    get() = facetSettings("project_test")

internal fun GradleImportingTestCase.getSourceRootInfos(moduleName: String): List<Pair<String, JpsModuleSourceRootType<*>>> {
    return ModuleRootManager.getInstance(getModule(moduleName)).contentEntries.flatMap {
        it.sourceFolders.map { it.url.replace(projectPath, "") to it.rootType }
    }
}

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

    private fun assertKotlinSdk(vararg moduleNames: String) {
        val sdks = moduleNames.map { getModule(it).sdk!! }
        val refSdk = sdks.firstOrNull() ?: return
        Assert.assertTrue(refSdk.sdkType is KotlinSdkType)
        Assert.assertTrue(sdks.all { it === refSdk })
    }

    @Test
    fun testJvmImport() {
        doTest("idea/testData/gradle/facets/jvmImport")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8), platform)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }
        with(testFacetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        assertAllModulesConfigured()

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJvmImportWithPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportWithPlugin")

        assertAllModulesConfigured()
    }

    @Test
    fun testJvmImport_1_1_2() {
        doTest("idea/testData/gradle/facets/jvmImport_1_1_2")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8), platform)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }
        with(testFacetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJvmImportWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/jvmImportWithCustomSourceSets")

        with(facetSettings("project_myMain")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8), platform)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }
        with(facetSettings("project_myTest")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        assertAllModulesConfigured()

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJvmImportWithCustomSourceSets_1_1_2() {
        doTest("idea/testData/gradle/facets/jvmImportWithCustomSourceSets_1_1_2")

        with(facetSettings("project_myMain")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8), platform)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }
        with(facetSettings("project_myTest")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals(
                "-Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testCoroutineImportByOptions() {
        doTest("idea/testData/gradle/facets/coroutineImportByOptions")
        with(facetSettings) {
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testCoroutineImportByProperties() {
        doTest(
            "idea/testData/gradle/facets/coroutineImportByProperties"
        )

        with(facetSettings) {
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testJsImport() {
        doTest("idea/testData/gradle/facets/jsImport")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertTrue(platform.isJavaScript)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(true, sourceMap)
                Assert.assertEquals("plain", moduleKind)
            }
            Assert.assertEquals(
                "-main callMain",
                compilerSettings!!.additionalArguments
            )
        }

        with(testFacetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertTrue(platform.isJavaScript)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(false, sourceMap)
                Assert.assertEquals("umd", moduleKind)
            }
            Assert.assertEquals(
                "-main callTest",
                compilerSettings!!.additionalArguments
            )
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)
        assertTrue(stdlib.getFiles(OrderRootType.CLASSES).isNotEmpty())

        assertKotlinSdk("project_main", "project_test")

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testJsImportTransitive() {
        doTest("idea/testData/gradle/facets/jsImportTransitive")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isJavaScript)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val stdlib = rootManager.orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .map { it.library as LibraryEx }
            .first { "kotlin-stdlib-js" in it.name!! }
        assertEquals(JSLibraryKind, stdlib.kind)

        assertAllModulesConfigured()

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/jsImportWithCustomSourceSets")

        with(facetSettings("project_myMain")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isJavaScript)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(true, sourceMap)
                Assert.assertEquals("plain", moduleKind)
            }
            Assert.assertEquals(
                "-main callMain",
                compilerSettings!!.additionalArguments
            )
        }

        with(facetSettings("project_myTest")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertTrue(platform.isJavaScript)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(false, sourceMap)
                Assert.assertEquals("umd", moduleKind)
            }
            Assert.assertEquals(
                "-main callTest",
                compilerSettings!!.additionalArguments
            )
        }

        assertAllModulesConfigured()

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testDetectOldJsStdlib() {
        doTest("idea/testData/gradle/facets/detectOldJsStdlib")

        with(facetSettings) {
            Assert.assertTrue(platform.isJavaScript)
        }
    }

    @Test
    fun testJvmImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportByPlatformPlugin")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
        }

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJsImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/jsImportByPlatformPlugin")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isJavaScript)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val libraries = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().mapNotNull { it.library as LibraryEx }
        assertEquals(JSLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-js") == true }.kind)
        assertEquals(CommonLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-common") == true }.kind)

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testCommonImportByPlatformPlugin() {
        doTest("idea/testData/gradle/facets/commonImportByPlatformPlugin")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isCommon)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testCommonImportByPlatformPlugin_SingleModule() {
        doTest("idea/testData/gradle/facets/commonImportByPlatformPlugin_SingleModule")

        with(facetSettings("project")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isCommon)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().mapTo(HashSet()) { it.library }.single()
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project")
        )
    }

    @Test
    fun testJvmImportByKotlinPlugin() {
        doTest("idea/testData/gradle/facets/jvmImportByKotlinPlugin")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6), platform)
        }

        Assert.assertEquals(
                listOf("file:///src/main/java" to JavaSourceRootType.SOURCE,
                       "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                       "file:///src/main/resources" to JavaResourceRootType.RESOURCE),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                       "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testJsImportByKotlin2JsPlugin() {
        doTest("idea/testData/gradle/facets/jsImportByKotlin2JsPlugin")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertTrue(platform.isJavaScript)
        }

        Assert.assertEquals(
                listOf("file:///src/main/java" to KotlinSourceRootType.Source,
                       "file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/java" to KotlinSourceRootType.TestSource,
                       "file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testArgumentEscaping() {
        doTest("idea/testData/gradle/facets/argumentEscaping")

        with(facetSettings) {
            Assert.assertEquals(
                listOf("-Xbuild-file=module with spaces"),
                compilerSettings!!.additionalArgumentsAsList
            )
        }
    }

    @Test
    fun testNoPluginsInAdditionalArgs() {
        doTest("idea/testData/gradle/facets/noPluginsInAdditionalArgs")

        with(facetSettings) {
            Assert.assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
            Assert.assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.stereotype.Component",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.transaction.annotation.Transactional",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.scheduling.annotation.Async",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.cache.annotation.Cacheable",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.boot.test.context.SpringBootTest",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.validation.annotation.Validated"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    @Test
    fun testNoArgInvokeInitializers() {
        doTest("idea/testData/gradle/facets/noArgInvokeInitializers")

        with(facetSettings) {
            Assert.assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
            Assert.assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.noarg:annotation=NoArg",
                    "plugin:org.jetbrains.kotlin.noarg:invokeInitializers=true"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    @Test
    fun testAndroidGradleJsDetection() {
        doTest("idea/testData/gradle/facets/androidGradleJsDetection")

        with(facetSettings("js-module")) {
            Assert.assertTrue(platform.isJavaScript)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("js-module"))
        val stdlib = rootManager
            .orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .first { it.libraryName?.startsWith("Gradle: kotlin-stdlib-js-") ?: false }
            .library!!
        assertTrue(stdlib.getFiles(OrderRootType.CLASSES).isNotEmpty())
        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)
    }

    @Test
    fun testKotlinAndroidPluginDetection() {
        doTest("idea/testData/gradle/facets/kotlinAndroidPluginDetection")

        val kotlinFacet = KotlinFacet.get(getModule("project"))!!
        Assert.assertTrue(kotlinFacet.configuration.settings.mergedCompilerArguments!!.progressiveMode)
    }

    @Test
    fun testNoFacetInModuleWithoutKotlinPlugin() {
        doTest("idea/testData/gradle/facets/noFacetInModuleWithoutKotlinPlugin")

        Assert.assertNotNull(KotlinFacet.get(getModule("gr01_main")))
        Assert.assertNotNull(KotlinFacet.get(getModule("gr01_test")))
        Assert.assertNull(KotlinFacet.get(getModule("m1_main")))
        Assert.assertNull(KotlinFacet.get(getModule("m1_test")))
    }

    @Test
    fun testClasspathWithDependenciesImport() {
        doTest("idea/testData/gradle/facets/classpathWithDependenciesImport")

        with(facetSettings) {
            Assert.assertEquals("tmp.jar", (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    @Test
    fun testDependenciesClasspathImport() {
        doTest("idea/testData/gradle/facets/dependenciesClasspathImport")

        with(facetSettings) {
            Assert.assertEquals(null, (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    @Test
    fun testJDKImport() {
        object : WriteAction<Unit>() {
            override fun run(result: Result<Unit>) {
                val jdk = JavaSdk.getInstance().createJdk("myJDK", "my/path/to/jdk")
                ProjectJdkTable.getInstance().addJdk(jdk)
            }
        }.execute()

        try {
            createProjectSubFile(
                "build.gradle", """
                group 'Again'
                version '1.0-SNAPSHOT'

                buildscript {
                    repositories {
                        mavenCentral()
                        maven {
                            url 'http://dl.bintray.com/kotlin/kotlin-eap-1.1'
                        }
                    }

                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0")
                    }
                }

                apply plugin: 'kotlin'

                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0"
                    compile "org.apache.logging.log4j:log4j-core:2.7"
                }

                compileKotlin {
                    kotlinOptions.jdkHome = "my/path/to/jdk"
                }
            """
            )
            importProject()

            val moduleSDK = ModuleRootManager.getInstance(getModule("project_main")).sdk!!
            Assert.assertTrue(moduleSDK.sdkType is JavaSdk)
            Assert.assertEquals("myJDK", moduleSDK.name)
            Assert.assertEquals("my/path/to/jdk", moduleSDK.homePath)
        } finally {
            object : WriteAction<Unit>() {
                override fun run(result: Result<Unit>) {
                    val jdkTable = ProjectJdkTable.getInstance()
                    jdkTable.removeJdk(jdkTable.findJdk("myJDK")!!)
                }
            }.execute()
        }
    }

    @Test
    fun testImplementsDependency() {
        doTest("idea/testData/gradle/facets/implementsDependency")

        Assert.assertEquals(listOf("MultiTest_main"), facetSettings("MultiTest-jvm_main").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_test"), facetSettings("MultiTest-jvm_test").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_main"), facetSettings("MultiTest-js_main").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_test"), facetSettings("MultiTest-js_test").implementedModuleNames)
    }

    @Test
    fun testImplementsDependencyWithCustomSourceSets() {
        doTest("idea/testData/gradle/facets/implementsDependencyWithCustomSourceSets")

        Assert.assertEquals(listOf("MultiTest_myMain"), facetSettings("MultiTest-jvm_myMain").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_myTest"), facetSettings("MultiTest-jvm_myTest").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_myMain"), facetSettings("MultiTest-js_myMain").implementedModuleNames)
        Assert.assertEquals(listOf("MultiTest_myTest"), facetSettings("MultiTest-js_myTest").implementedModuleNames)
    }

    @Test
    fun testAPIVersionExceedingLanguageVersion() {
        doTest("idea/testData/gradle/facets/APIVersionExceedingLanguageVersion")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
        }

        assertAllModulesConfigured()
    }

    @Test
    fun testIgnoreProjectLanguageAndAPIVersion() {
        KotlinCommonCompilerArgumentsHolder.getInstance(myProject).update {
            languageVersion = "1.0"
            apiVersion = "1.0"
        }

        createProjectSubFile(
            "build.gradle", """
            buildscript {
                repositories {
                    mavenCentral()
                    maven {
                        url 'http://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0"
            }
        """
        )
        importProject()

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
        }

        assertAllModulesConfigured()
    }

    @Test
    fun testCommonArgumentsImport() {
        doTest("idea/testData/gradle/facets/commonArgumentsImport")

        with(facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertTrue(platform.isCommon)
            Assert.assertEquals("my/classpath", (compilerArguments as K2MetadataCompilerArguments).classpath)
            Assert.assertEquals("my/destination", (compilerArguments as K2MetadataCompilerArguments).destination)
        }

        with(facetSettings("project_test")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            Assert.assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            Assert.assertTrue(platform.isCommon)
            Assert.assertEquals("my/test/classpath", (compilerArguments as K2MetadataCompilerArguments).classpath)
            Assert.assertEquals("my/test/destination", (compilerArguments as K2MetadataCompilerArguments).destination)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        assertKotlinSdk("project_main", "project_test")

        Assert.assertEquals(
                listOf("file:///src/main/kotlin" to KotlinSourceRootType.Source,
                       "file:///src/main/resources" to KotlinResourceRootType.Resource),
                getSourceRootInfos("project_main")
        )
        Assert.assertEquals(
                listOf("file:///src/test/kotlin" to KotlinSourceRootType.TestSource,
                       "file:///src/test/resources" to KotlinResourceRootType.TestResource),
                getSourceRootInfos("project_test")
        )
    }

    @Test
    fun testInternalArgumentsFacetImporting() {
        doTest("idea/testData/gradle/facets/internalArgumentsFacetImporting")

        // Version is indeed 1.2
        Assert.assertEquals(LanguageVersion.KOTLIN_1_2, facetSettings.languageLevel)

        // We haven't lost internal argument during importing to facet
        Assert.assertEquals("-XXLanguage:+InlineClasses", facetSettings.compilerSettings?.additionalArguments)

        // Inline classes are enabled even though LV = 1.2
        Assert.assertEquals(
            LanguageFeature.State.ENABLED,
            getModule("project_main").languageVersionSettings.getFeatureSupport(LanguageFeature.InlineClasses)
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradle_JS() {
        doTest("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JS")

        checkStableModuleName("project_main", "project", JsPlatform, isProduction = true)
        // Note "_test" suffix: this is current behavior of K2JS Compiler
        checkStableModuleName("project_test", "project_test", JsPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradle_JVM() {
        doTest("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JVM")

        checkStableModuleName("project_main", "project", JvmPlatform, isProduction = true)
        checkStableModuleName("project_test", "project", JvmPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testNoFriendPathsAreShown() {
        doTest("idea/testData/gradle/facets/noFriendPathsAreShown")

        Assert.assertEquals(
            "-version",
            testFacetSettings.compilerSettings!!.additionalArguments
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testSharedLanguageVersion() {
        doTest("idea/testData/gradle/facets/sharedLanguageVersion")

        val holder = KotlinCommonCompilerArgumentsHolder.getInstance(myProject)

        holder.update { languageVersion = "1.1" }

        importProject()

        TestCase.assertEquals("1.2", holder.settings.languageVersion)
    }

    @Test
    fun testNonSharedLanguageVersion() {
        doTest("idea/testData/gradle/facets/nonSharedLanguageVersion")

        val holder = KotlinCommonCompilerArgumentsHolder.getInstance(myProject)

        holder.update { languageVersion = "1.1" }

        importProject()

        TestCase.assertEquals("1.1", holder.settings.languageVersion)
    }

    private fun checkStableModuleName(projectName: String, expectedName: String, platform: TargetPlatform, isProduction: Boolean) {
        val module = getModule(projectName)
        val moduleInfo = if (isProduction) module.productionSourceInfo() else module.testSourceInfo()

        val resolutionFacade = KotlinCacheService.getInstance(myProject).getResolutionFacadeByModuleInfo(moduleInfo!!, platform)!!
        val moduleDescriptor = resolutionFacade.moduleDescriptor

        Assert.assertEquals("<$expectedName>", moduleDescriptor.stableName?.asString())
    }

    private fun assertAllModulesConfigured() {
        runReadAction {
            for (moduleGroup in ModuleSourceRootMap(myProject).groupByBaseModules(myProject.allModules())) {
                val configurator = allConfigurators().find {
                    it.getStatus(moduleGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED
                }
                Assert.assertNull("Configurator $configurator tells that ${moduleGroup.baseModule} can be configured", configurator)
            }
        }
    }
}
