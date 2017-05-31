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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Test

class GradleConfiguratorTest : GradleImportingTestCase() {

    @Test
    fun testProjectWithModule() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
            }
        }

        apply plugin: 'kotlin'

        dependencies {
            compile "org.jetbrains.kotlin:kotlin-stdlib-0.0"   // intentionally invalid version
        }
        """.trimIndent())

        importProject()

        runInEdtAndWait {
            runWriteAction {
                // Create not configured build.gradle for project
                myProject.baseDir.createChildData(null, "build.gradle")

                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                // We have a Kotlin runtime in build.gradle but not in the classpath, so it doesn't make sense
                // to suggest configuring it
                assertEquals(ConfigureKotlinStatus.BROKEN, findGradleModuleConfigurator().getStatus(module))
                // Don't offer the JS configurator if the JVM configuration exists but is broken
                assertEquals(ConfigureKotlinStatus.BROKEN, findJsGradleModuleConfigurator().getStatus(module))
            }
        }
    }

    @Test
    fun testConfigure10() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }
        """.trimIndent())

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.0.6", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals("""
                buildscript {
                    ext.kotlin_version = '1.0.6'
                    repositories {
                        jcenter()
                        mavenCentral()
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlin_version"
                    }
                }
                apply plugin: 'kotlin'
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib:${"$"}kotlin_version"
                }
                """.trimIndent(), content)
            }
        }
    }

    private fun findGradleModuleConfigurator() = Extensions.findExtension(KotlinProjectConfigurator.EP_NAME,
                                                                          KotlinGradleModuleConfigurator::class.java)

    private fun findJsGradleModuleConfigurator() = Extensions.findExtension(KotlinProjectConfigurator.EP_NAME,
                                                                            KotlinJsGradleModuleConfigurator::class.java)

    @Test
    fun testConfigureGSK() {
        createProjectSubFile("settings.gradle", "include ':app'")
        val file = createProjectSubFile("app/build.gradle.kts", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }
        """.trimIndent())

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.1.2", collector)

                FileDocumentManager.getInstance().saveAllDocuments()
                val content = LoadTextUtil.loadText(file).toString()
                assertEquals("""
                    import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

                    val kotlin_version: String by extra
                    buildscript {
                        var kotlin_version: String by extra
                        kotlin_version = "1.1.2"
                        repositories {
                            jcenter()
                            mavenCentral()
                        }
                        dependencies {
                            classpath(kotlinModule("gradle-plugin", kotlin_version))
                        }
                    }
                    apply {
                        plugin("kotlin")
                    }
                    dependencies {
                        compile(kotlinModule("stdlib-jre8", kotlin_version))
                    }
                    repositories {
                        mavenCentral()
                    }
                    val compileKotlin: KotlinCompile by tasks
                    compileKotlin.kotlinOptions {
                        jvmTarget = "1.8"
                    }
                    val compileTestKotlin: KotlinCompile by tasks
                    compileTestKotlin.kotlinOptions {
                        jvmTarget = "1.8"
                    }
                """.trimIndent(), content)
            }
        }
    }

    @Test
    fun testListNonConfiguredModules() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'
        """.trimIndent())
        createProjectSubFile("app/src/main/java/foo.kt", "")

        importProject()

        runReadAction {
            val configurator = findGradleModuleConfigurator()

            val moduleNames = getCanBeConfiguredModulesWithKotlinFiles(myProject).map { it.name }
            assertSameElements(moduleNames, "app")

            val moduleNamesFromConfigurator = getCanBeConfiguredModules(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesFromConfigurator, "app")

            val moduleNamesWithKotlinFiles = getCanBeConfiguredModulesWithKotlinFiles(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesWithKotlinFiles, "app")
        }
    }
}
