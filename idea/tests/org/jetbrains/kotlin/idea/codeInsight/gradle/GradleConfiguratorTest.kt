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
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
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
                val moduleGroup = ModuleSourceRootMap(myProject).toModuleGroup(module)
                // We have a Kotlin runtime in build.gradle but not in the classpath, so it doesn't make sense
                // to suggest configuring it
                assertEquals(ConfigureKotlinStatus.BROKEN, findGradleModuleConfigurator().getStatus(moduleGroup))
                // Don't offer the JS configurator if the JVM configuration exists but is broken
                assertEquals(ConfigureKotlinStatus.BROKEN, findJsGradleModuleConfigurator().getStatus(moduleGroup))
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

    @Test
    fun testListNonConfiguredModules_Configured() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'

        repositories {
            jcenter()
            mavenCentral()
        }

        dependencies {
            compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.3"
        }
        """.trimIndent())
        createProjectSubFile("app/src/main/java/foo.kt", "")

        importProject()

        runReadAction {
            assertEmpty(getCanBeConfiguredModulesWithKotlinFiles(myProject))
        }
    }

    @Test
    fun testListNonConfiguredModules_ConfiguredOnlyTest() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
        }

        apply plugin: 'java'

        repositories {
            jcenter()
            mavenCentral()
        }

        dependencies {
            testCompile "org.jetbrains.kotlin:kotlin-stdlib:1.1.3"
        }
        """.trimIndent())
        createProjectSubFile("app/src/test/java/foo.kt", "")

        importProject()

        runReadAction {
            assertEmpty(getCanBeConfiguredModulesWithKotlinFiles(myProject))
        }
    }

    @Test
    fun testAddNonKotlinLibraryGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                  """
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object: ExternalLibraryDescriptor("org.a.b", "lib", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    compile("org.a.b:lib:1.0.0")
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddLibraryGSK_WithKotlinVersion() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                                    """
                                                     |val kotlin_version: String by extra
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8", kotlin_version))
                                                     |}
                                                     |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                val stdLibVersion = KotlinWithGradleConfigurator.getKotlinStdlibVersion(myTestFixture.module)
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", stdLibVersion, stdLibVersion) {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |val kotlin_version: String by extra
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8", kotlin_version))
                 |    compile(kotlinModule("reflect", kotlin_version))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddTestLibraryGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                                    """
                                                     |dependencies {
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.TEST,
                        object : ExternalLibraryDescriptor("junit", "junit", "4.12", "4.12") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })

                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.TEST,
                        object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test", "1.1.2", "1.1.2") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    testCompile("junit:junit:4.12")
                 |    testCompile(kotlinModule("test", "1.1.2"))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddLibraryGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                                    """
                                                     |dependencies {
                                                     |    testCompile("junit:junit:4.12")
                                                     |    compile(kotlinModule("stdlib-jre8"))
                                                     |}
                                                     |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object: ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """
                 |dependencies {
                 |    testCompile("junit:junit:4.12")
                 |    compile(kotlinModule("stdlib-jre8"))
                 |    compile(kotlinModule("reflect", "1.0.0"))
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddCoroutinesSupport() {
        val buildScript = createProjectSubFile("build.gradle", """
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
            compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
        }
        """.trimIndent())

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals("""
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
                    compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
                }
                kotlin {
                    experimental {
                        coroutines "enable"
                    }
                }
                """.trimIndent(),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddCoroutinesSupportGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts", "")

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.ENABLE
                  |}""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testChangeCoroutinesSupport() {
        val buildScript = createProjectSubFile("build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            kotlin {
                experimental {
                      coroutines "error"
                }
            }
            """.trimIndent())

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals("""
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            kotlin {
                experimental {
                      coroutines "enable"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testChangeCoroutinesSupportGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                                    """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.DISABLE
                  |}
                  |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.dsl.Coroutines
                  |
                  |kotlin {
                  |    experimental.coroutines = Coroutines.ENABLE
                  |}
                  |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddLanguageVersion() {
        val buildScript = createProjectSubFile("build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            """.trimIndent())

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals("""
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.1"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddLanguageVersionGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts", "")

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                 |
                 |val compileKotlin: KotlinCompile by tasks
                 |compileKotlin.kotlinOptions {
                 |    languageVersion = "1.1"
                 |}""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testChangeLanguageVersion() {
        val buildScript = createProjectSubFile("build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.0"
                }
            }
            """.trimIndent())

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals("""
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            compileKotlin {
                kotlinOptions {
                    languageVersion = "1.1"
                }
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testChangeLanguageVersionGSK() {
        val buildScript = createProjectSubFile("build.gradle.kts",
                                                    """val compileKotlin: KotlinCompile by tasks
                                                     |compileKotlin.kotlinOptions {
                                                     |   languageVersion = "1.0"
                                                     |}
                                                     |""".trimMargin("|"))

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals(
                """val compileKotlin: KotlinCompile by tasks
                 |compileKotlin.kotlinOptions {
                 |    languageVersion = "1.1"
                 |}
                 |""".trimMargin("|"),
                LoadTextUtil.loadText(buildScript).toString())
    }

    @Test
    fun testAddLibrary() {
        val buildScript = createProjectSubFile("build.gradle", """
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
            }
            """.trimIndent())

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                        myTestFixture.module,
                        DependencyScope.COMPILE,
                        object: ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                            override fun getLibraryClassesRoots() = emptyList<String>()
                        })
            }

            FileDocumentManager.getInstance().saveAllDocuments()
        }

        assertEquals("""
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
                compile "org.jetbrains.kotlin:kotlin-stdlib-1.1.0"
                compile "org.jetbrains.kotlin:kotlin-reflect:1.0.0"
            }
            """.trimIndent(), LoadTextUtil.loadText(buildScript).toString())
    }
}
