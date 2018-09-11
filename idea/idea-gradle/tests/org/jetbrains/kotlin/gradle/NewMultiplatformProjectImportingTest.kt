/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.*
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.junit.Test
import org.junit.runners.Parameterized

class NewMultiplatformProjectImportingTest : GradleImportingTestCase() {
    private val kotlinVersion = "1.2.70-dev-1648"

    @Test
    fun testProjectDependency() {
        createProjectSubFile(
            "settings.gradle",
            "include 'lib', 'app'"
        )
        createProjectSubFile(
            "build.gradle",
            """
                buildscript {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
                    }
                }

                allprojects {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                }
            """.trimIndent()
        )
        createProjectSubFile(
            "app/build.gradle",
            """
                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        commonMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                                implementation project(':lib')
                            }
                        }
                        main {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                            }
                        }
                        jsMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvmWithJava, 'jvm')
                        fromPreset(presets.js, 'js')
                    }
                }

                apply plugin: 'application'
                mainClassName = 'com.example.app.JvmGreeterKt'
            """.trimIndent()
        )
        createProjectSubFile(
            "lib/build.gradle",
            """
                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        commonMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                            }
                        }
                        jvmMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                            }
                        }
                        jsMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvm, 'jvm')
                        fromPreset(presets.js, 'js')
                    }
                }
            """.trimIndent()
        )

        importProject()

        checkProjectStructure() {
            allModules {
                languageVersion("1.2")
                apiVersion("1.2")
                when (module.name) {
                    "project", "app", "lib" -> additionalArguments(null)
                    else -> additionalArguments("-version")
                }
            }

            module("project")
            module("app")
            // TODO: Delete metadata modules (after KT-26253 fixed)
            module("app_metadataMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/metadataMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("app/src/metadataMain/resources", KotlinResourceRootType.Resource)
                outputPath("app/build/classes/kotlin/metadata/main", true)
            }
            module("app_metadataTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_commonTest", DependencyScope.COMPILE)
                moduleDependency("app_metadataMain", DependencyScope.COMPILE)
                sourceFolder("app/src/metadataTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("app/src/metadataTest/resources", KotlinResourceRootType.TestResource)
                outputPath("app/build/classes/kotlin/metadata/test", false)
            }
            module("app_commonMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                sourceFolder("app/src/commonMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("app/src/commonMain/resources", KotlinResourceRootType.Resource)
                inheritProjectOutput()
            }
            module("app_commonTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/commonTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("app/src/commonTest/resources", KotlinResourceRootType.TestResource)
                inheritProjectOutput()
            }
            module("app_jsMain") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_jsMain", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jsMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("app/src/jsMain/resources", KotlinResourceRootType.Resource)
                outputPath("app/build/classes/kotlin/js/main", true)
            }
            module("app_jsTest") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_jsMain", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_commonTest", DependencyScope.COMPILE)
                moduleDependency("app_jsMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jsTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("app/src/jsTest/resources", KotlinResourceRootType.TestResource)
                outputPath("app/build/classes/kotlin/js/test", false)
            }
            module("app_jvmMain") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_jvmMain", DependencyScope.COMPILE)
                moduleDependency("app_main", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/main", true)
            }
            module("app_jvmTest") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_jvmMain", DependencyScope.COMPILE)
                moduleDependency("app_test", DependencyScope.COMPILE)
                moduleDependency("app_jvmMain", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_commonTest", DependencyScope.COMPILE)
                sourceFolder("app/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/test", false)
            }
            module("app_main") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/resources", JavaResourceRootType.RESOURCE)
                inheritProjectOutput()
            }
            module("app_test") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("app_main", DependencyScope.COMPILE)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                inheritProjectOutput()
            }
            module("lib")
            module("lib_metadataMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/metadataMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("lib/src/metadataMain/resources", KotlinResourceRootType.Resource)
                outputPath("lib/build/classes/kotlin/metadata/main", true)
            }
            module("lib_metadataTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonTest", DependencyScope.COMPILE)
                moduleDependency("lib_metadataMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/metadataTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("lib/src/metadataTest/resources", KotlinResourceRootType.TestResource)
                outputPath("lib/build/classes/kotlin/metadata/test", false)
            }
            module("lib_commonMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                sourceFolder("lib/src/commonMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("lib/src/commonMain/resources", KotlinResourceRootType.Resource)
                inheritProjectOutput()
            }
            module("lib_commonTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/commonTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("lib/src/commonTest/resources", KotlinResourceRootType.TestResource)
                inheritProjectOutput()
            }
            module("lib_jsMain") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jsMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("lib/src/jsMain/resources", KotlinResourceRootType.Resource)
                outputPath("lib/build/classes/kotlin/js/main", true)
            }
            module("lib_jsTest") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonTest", DependencyScope.COMPILE)
                moduleDependency("lib_jsMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jsTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("lib/src/jsTest/resources", KotlinResourceRootType.TestResource)
                outputPath("lib/build/classes/kotlin/js/test", false)
            }
            module("lib_jvmMain") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("lib/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/main", true)
            }
            module("lib_jvmTest") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonTest", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("lib_jvmMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("lib/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/test", false)
            }
        }
    }

    private fun checkProjectStructure(body: ProjectInfo.() -> Unit = {}) {
        checkProjectStructure(myProject, projectPath, body)
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

    companion object {
        @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
        @Throws(Throwable::class)
        @JvmStatic
        fun data() = listOf(arrayOf("4.7"))
    }
}