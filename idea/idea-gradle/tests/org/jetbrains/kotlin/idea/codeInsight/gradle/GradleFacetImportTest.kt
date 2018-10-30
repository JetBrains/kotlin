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
import org.junit.Assert
import org.junit.Test
import java.util.*

internal fun GradleImportingTestCase.facetSettings(moduleName: String) = kotlinFacet(moduleName)!!.configuration.settings

private fun GradleImportingTestCase.kotlinFacet(moduleName: String) =
    KotlinFacet.get(getModule(moduleName))

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
        loadProject("idea/testData/gradle/facets/jvmImport")

        checkFacet("idea/testData/gradle/facets/jvmImport", getModule("project_main"))
        checkFacet("idea/testData/gradle/facets/jvmImport", getModule("project_test"))


    }

    @Test
    fun testJvmImportWithPlugin() {
        loadProject("idea/testData/gradle/facets/jvmImportWithPlugin")

        assertAllModulesConfigured()
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

        val rootManager = ModuleRootManager.getInstance(getModule("project_main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)
        assertTrue(stdlib.getFiles(OrderRootType.CLASSES).isNotEmpty())

        assertKotlinSdk("project_main", "project_test")

        assertAllModulesConfigured()
    }

    @Test
    fun testJsImportTransitive() {
        loadProject("idea/testData/gradle/facets/jsImportTransitive")
        checkFacet("idea/testData/gradle/facets/jsImportTransitive", getModule("project_main"))
        assertAllModulesConfigured()
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        loadProject("idea/testData/gradle/facets/jsImportWithCustomSourceSets")
        checkFacet("idea/testData/gradle/facets/jsImportWithCustomSourceSets", getModule("project_myMain"))
        checkFacet("idea/testData/gradle/facets/jsImportWithCustomSourceSets", getModule("project_myTest"))

        assertAllModulesConfigured()
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
    fun testJDKImport() {
        // TODO: wtf
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

        assertAllModulesConfigured()
    }

    @Test
    fun testIgnoreProjectLanguageAndAPIVersion() {
        // TODO: wtf
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
        loadProject("idea/testData/gradle/facets/commonArgumentsImport")
        checkFacet("idea/testData/gradle/facets/commonArgumentsImport", getModule("project_main"))
        checkFacet("idea/testData/gradle/facets/commonArgumentsImport", getModule("project_test"))

        assertKotlinSdk("project_main", "project_test")
    }

    @Test
    fun testInternalArgumentsFacetImporting() {
        loadProject("idea/testData/gradle/facets/internalArgumentsFacetImporting")
        checkFacet("idea/testData/gradle/facets/internalArgumentsFacetImporting", getModule("project_main"))

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradle_JS() {
        loadProject("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JS")

        checkStableModuleName("project_main", "project", JsPlatform, isProduction = true)
        // Note "_test" suffix: this is current behavior of K2JS Compiler
        checkStableModuleName("project_test", "project_test", JsPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradle_JVM() {
        loadProject("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JVM")

        checkStableModuleName("project_main", "project", JvmPlatform, isProduction = true)
        checkStableModuleName("project_test", "project", JvmPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testNoFriendPathsAreShown() {
        loadProject("idea/testData/gradle/facets/noFriendPathsAreShown")
        checkFacet("idea/testData/gradle/facets/noFriendPathsAreShown", getModule("project_main"))

        assertAllModulesConfigured()
    }

    @Test
    fun testSharedLanguageVersion() {
        loadProject("idea/testData/gradle/facets/sharedLanguageVersion")

        val holder = KotlinCommonCompilerArgumentsHolder.getInstance(myProject)

        holder.update { languageVersion = "1.1" }

        importProject()

        TestCase.assertEquals("1.2", holder.settings.languageVersion)
    }

    @Test
    fun testNonSharedLanguageVersion() {
        loadProject("idea/testData/gradle/facets/nonSharedLanguageVersion")

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
