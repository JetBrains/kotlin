/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import junit.framework.TestCase
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.junit.Assert
import org.junit.Test

class CustomGradleImportTest : GradleImportingTestCase() {
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
            TestCase.assertEquals("1.1", languageLevel!!.versionString)
            TestCase.assertEquals("1.1", apiLevel!!.versionString)
        }
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
    fun testStableModuleNameWhileUsingGradle_JS() {
        loadProject("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JS")

        checkStableModuleName("project_main", "project", JsPlatform, isProduction = true)
        // Note "_test" suffix: this is current behavior of K2JS Compiler
        checkStableModuleName("project_test", "project_test", JsPlatform, isProduction = false)
    }

    @Test
    fun testStableModuleNameWhileUsingGradle_JVM() {
        loadProject("idea/testData/gradle/facets/stableModuleNameWhileUsingGradle_JVM")

        checkStableModuleName("project_main", "project", JvmPlatform, isProduction = true)
        checkStableModuleName("project_test", "project", JvmPlatform, isProduction = false)
    }

    private fun checkStableModuleName(projectName: String, expectedName: String, platform: TargetPlatform, isProduction: Boolean) {
        val module = getModule(projectName)
        val moduleInfo = if (isProduction) module.productionSourceInfo() else module.testSourceInfo()

        val resolutionFacade = KotlinCacheService.getInstance(myProject).getResolutionFacadeByModuleInfo(moduleInfo!!, platform)!!
        val moduleDescriptor = resolutionFacade.moduleDescriptor

        Assert.assertEquals("<$expectedName>", moduleDescriptor.stableName?.asString())
    }

}