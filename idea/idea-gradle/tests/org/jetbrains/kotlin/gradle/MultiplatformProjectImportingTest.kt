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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File

class MultiplatformProjectImportingTest : GradleImportingTestCase() {
    @Test
    fun testPlatformToCommonDependency() {
        createProjectSubFile("settings.gradle", "include ':common', ':jvm', ':js'")

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common')
                }
            }

            project('js') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common')
                }
            }
        """)

        importProject()
        assertModuleModuleDepScope("jvm_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "common_test", DependencyScope.COMPILE)
    }

    @Test
    fun testPlatformToCommonExpectedByDependency() {
        createProjectSubFile("settings.gradle", "include ':common', ':jvm', ':js'")

        val kotlinVersion = "1.2.0-beta-74"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                    maven { url 'http://dl.bintray.com/kotlin/kotlin-dev' }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    expectedBy project(':common')
                }
            }

            project('js') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    expectedBy project(':common')
                }
            }
        """)

        importProject()
        assertModuleModuleDepScope("jvm_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "common_test", DependencyScope.COMPILE)
    }

    @Test
    fun testPlatformToCommonDependencyRoot() {
        createProjectSubFile("settings.gradle", "rootProject.name = 'foo'\ninclude ':jvm', ':js'")

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            apply plugin: 'kotlin-platform-common'

            project('jvm') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':')
                }
            }

            project('js') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':')
                }
            }
        """)

        importProject()
        assertModuleModuleDepScope("jvm_main", "foo_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "foo_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "foo_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "foo_test", DependencyScope.COMPILE)
    }

    @Test
    fun testMultiProject() {
        createProjectSubFile("settings.gradle", "include ':common-lib', ':jvm-lib', ':js-lib', ':common-app', ':jvm-app', ':js-app'")

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common-lib') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm-lib') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-lib')
                }
            }

            project('js-lib') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common-lib')
                }
            }

            project('common-app') {
                apply plugin: 'kotlin-platform-common'

                dependencies {
                    compile project(':common-lib')
                }
            }

            project('jvm-app') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-app')
                    compile project(':jvm-lib')
                }
            }

            project('js-app') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common-app')
                    compile project(':js-lib')
                }
            }
        """)

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
        createProjectSubFile(
                "settings.gradle",
                "include ':common-lib1', ':common-lib2', ':jvm-lib1', ':jvm-lib2', ':jvm-app'"
        )

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common-lib1') {
                apply plugin: 'kotlin-platform-common'
            }

            project('common-lib2') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm-lib1') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-lib1')
                }
            }

            project('jvm-lib2') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-lib2')
                    compile project(':jvm-lib1')
                }
            }

            project('jvm-app') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    compile project(':jvm-lib2')
                }
            }
        """)

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
        createProjectSubFile(
                "settings.gradle",
                "include ':project1', ':project2', ':project3'"
        )

        val kotlinVersion = "1.1.51"

        createProjectSubFile("build.gradle", """
            buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('project1') {
                apply plugin: 'kotlin-platform-common'

                sourceSets {
                    custom
                }
            }

            project('project2') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'kotlin-platform-jvm'

                sourceSets {
                    custom
                }

                dependencies {
                    implement project(':project1')
                }
            }

            project('project3') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'kotlin-platform-jvm'
                apply plugin: 'kotlin'

                sourceSets {
                    custom
                }

                dependencies {
                    compile project(':project2')
                    customCompile project(':project2')
                    testCompile(project(':project2').sourceSets.test.output)
                }
            }
        """)

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
    }

    @Test
    fun testTransitiveImplementWithAndroid() {
        createProjectSubFile(
                "settings.gradle",
                "include ':project1', ':project2', ':project3'"
        )

        val kotlinVersion = "1.1.51"

        createProjectSubFile("build.gradle", """
            buildscript {
                repositories {
                    jcenter()
                    maven { url 'https://maven.google.com' }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath 'com.android.tools.build:gradle:2.3.3'
                }
            }

            project('project1') {
                apply plugin: 'kotlin-platform-common'

                sourceSets {
                    custom
                }
            }

            project('project2') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'kotlin-platform-jvm'

                sourceSets {
                    custom
                }

                dependencies {
                    implement project(':project1')
                }
            }

            project('project3') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'com.android.application'
                apply plugin: 'kotlin-android'

                sourceSets {
                    custom
                }

                android {
                    compileSdkVersion 23
                    buildToolsVersion "23.0.1"
                    defaultConfig {
                        applicationId "org.jetbrains.kotlin"
                        minSdkVersion 18
                        targetSdkVersion 26
                        versionCode 1
                        versionName "1.0"
                        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
                    }
                }

                dependencies {
                    compile project(':project2')
                    customCompile project(':project2')
                    testCompile(project(':project2').sourceSets.test.output)
                }
            }
        """)
        createProjectSubFile("local.properties", """
            sdk.dir=/${KotlinTestUtils.findAndroidSdk()}
        """)


        importProject()

        assertModuleModuleDepScope("project3", "project2", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project3", "project1", DependencyScope.COMPILE)

    }

    @Test
    fun testJsTestOutputFile() {
        createProjectSubFile(
                "settings.gradle",
                "include ':project1', ':project2', ':project3'"
        )

        val kotlinVersion = "1.1.51"

        createProjectSubFile("build.gradle", """
            buildscript {
                repositories {
                    jcenter()
                    maven { url 'https://maven.google.com' }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath 'com.android.tools.build:gradle:2.3.3'
                }
            }

            project('project1') {
                apply plugin: 'kotlin-platform-common'
            }

            project('project2') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':project1')
                }
            }
        """)

        importProject()

        TestCase.assertEquals(
                projectPath + "/project2/build/classes/test/project2_test.js",
                PathUtil.toSystemIndependentName(KotlinFacet.get (getModule("project2_main"))!!.configuration.settings.testOutputPath)
        )
        TestCase.assertEquals(
                projectPath + "/project2/build/classes/test/project2_test.js",
                PathUtil.toSystemIndependentName(KotlinFacet.get (getModule("project2_test"))!!.configuration.settings.testOutputPath)
        )
    }

    @Test
    fun testJsTestOutputFileInProjectWithAndroid() {
        createProjectSubFile(
                "settings.gradle",
                "include ':project1', ':project2', ':project3'"
        )

        val kotlinVersion = "1.1.51"

        createProjectSubFile("build.gradle", """
            buildscript {
                repositories {
                    jcenter()
                    maven { url 'https://maven.google.com' }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath 'com.android.tools.build:gradle:2.3.3'
                }
            }

            project('project1') {
                apply plugin: 'kotlin-platform-common'
            }

            project('project2') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':project1')
                }
            }

            project('project3') {
                repositories {
                    mavenCentral()
                }

                apply plugin: 'com.android.application'
                apply plugin: 'kotlin-android'

                sourceSets {
                    custom
                }

                android {
                    compileSdkVersion 23
                    buildToolsVersion "23.0.1"
                    defaultConfig {
                        applicationId "org.jetbrains.kotlin"
                        minSdkVersion 18
                        targetSdkVersion 26
                        versionCode 1
                        versionName "1.0"
                        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
                    }
                }
            }
        """)
        createProjectSubFile("local.properties", """
            sdk.dir=/${KotlinTestUtils.findAndroidSdk()}
        """)

        importProject()

        TestCase.assertEquals(
                projectPath + "/project2/build/classes/test/project2_test.js",
                PathUtil.toSystemIndependentName(KotlinFacet.get (getModule("project2"))!!.configuration.settings.testOutputPath)
        )
    }
}