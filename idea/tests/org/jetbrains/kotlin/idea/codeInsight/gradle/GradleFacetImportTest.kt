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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.junit.Assert
import org.junit.Test
import java.io.File

class GradleFacetImportTest : GradleImportingTestCase() {
    private fun facetSettings(moduleName: String) = KotlinFacet.get(getModule(moduleName))!!.configuration.settings

    private val facetSettings: KotlinFacetSettings
        get() = facetSettings("project_main")

    private val testFacetSettings: KotlinFacetSettings
        get() = facetSettings("project_test")

    @Test
    fun testJvmImport() {
        createProjectSubFile("build.gradle", """
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
            }

            compileKotlin {
                kotlinOptions.jvmTarget = "1.7"
                kotlinOptions.freeCompilerArgs = ["-Xsingle-module", "-Xdump-declarations-to", "tmp"]
            }

            compileTestKotlin {
                kotlinOptions.jvmTarget = "1.6"
                kotlinOptions.apiVersion = "1.0"
                kotlinOptions.freeCompilerArgs = ["-Xdump-declarations-to", "tmpTest"]
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_8], targetPlatformKind)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("-Xdump-declarations-to=tmp -Xsingle-module",
                                compilerSettings!!.additionalArguments)
        }
        with (testFacetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], targetPlatformKind)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("-Xdump-declarations-to=tmpTest",
                                compilerSettings!!.additionalArguments)
        }
    }

    @Test
    fun testJvmImportWithCustomSourceSets() {
        createProjectSubFile("build.gradle", """
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

            sourceSets {
                myMain {
                    kotlin {
                        srcDir 'src'
                    }
                }
                myTest {
                    kotlin {
                        srcDir 'test'
                    }
                }
            }

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0"
            }

            compileMyMainKotlin {
                kotlinOptions.jvmTarget = "1.7"
                kotlinOptions.freeCompilerArgs = ["-Xsingle-module", "-Xdump-declarations-to", "tmp"]
            }

            compileMyTestKotlin {
                kotlinOptions.jvmTarget = "1.6"
                kotlinOptions.apiVersion = "1.0"
                kotlinOptions.freeCompilerArgs = ["-Xdump-declarations-to", "tmpTest"]
            }
        """)
        importProject()

        with (facetSettings("project_myMain")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_8], targetPlatformKind)
            Assert.assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("-Xdump-declarations-to=tmp -Xsingle-module",
                                compilerSettings!!.additionalArguments)
        }
        with (facetSettings("project_myTest")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], targetPlatformKind)
            Assert.assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            Assert.assertEquals("-Xdump-declarations-to=tmpTest",
                                compilerSettings!!.additionalArguments)
        }
    }

    @Test
    fun testCoroutineImportByOptions() {
        createProjectSubFile("build.gradle", """
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
            }

            kotlin {
                experimental {
                    coroutines 'enable'
                }
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testCoroutineImportByProperties() {
        createProjectSubFile("gradle.properties", "kotlin.coroutines=enable")
        createProjectSubFile("build.gradle", """
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
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testJsImport() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.0"
            }

            compileKotlin2Js {
                kotlinOptions.sourceMap = true
                kotlinOptions.freeCompilerArgs = ["-module-kind", "plain", "-main", "callMain"]
            }

            compileTestKotlin2Js {
                kotlinOptions.apiVersion = "1.0"
                kotlinOptions.freeCompilerArgs = ["-module-kind", "umd", "-main", "callTest"]
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(true, sourceMap)
                Assert.assertEquals("plain", moduleKind)
            }
            Assert.assertEquals("-main callMain",
                                compilerSettings!!.additionalArguments)
        }

        with (testFacetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(false, sourceMap)
                Assert.assertEquals("umd", moduleKind)
            }
            Assert.assertEquals("-main callTest",
                                compilerSettings!!.additionalArguments)
        }
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin2js'

            sourceSets {
                myMain {
                    kotlin {
                        srcDir 'src'
                    }
                }
                myTest {
                    kotlin {
                        srcDir 'test'
                    }
                }
            }

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.0"
            }

            compileMyMainKotlin2Js {
                kotlinOptions.sourceMap = true
                kotlinOptions.freeCompilerArgs = ["-module-kind", "plain", "-main", "callMain"]
            }

            compileMyTestKotlin2Js {
                kotlinOptions.apiVersion = "1.0"
                kotlinOptions.freeCompilerArgs = ["-module-kind", "umd", "-main", "callTest"]
            }
        """)
        importProject()

        with (facetSettings("project_myMain")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(true, sourceMap)
                Assert.assertEquals("plain", moduleKind)
            }
            Assert.assertEquals("-main callMain",
                                compilerSettings!!.additionalArguments)
        }

        with (facetSettings("project_myTest")) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.0", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
            with(compilerArguments as K2JSCompilerArguments) {
                Assert.assertEquals(false, sourceMap)
                Assert.assertEquals("umd", moduleKind)
            }
            Assert.assertEquals("-main callTest",
                                compilerSettings!!.additionalArguments)
        }
    }

    @Test
    fun testDetectOldJsStdlib() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.0.6")
                }
            }

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-js-library:1.0.6"
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
        }
    }

    @Test
    fun testJvmImportByPlatformPlugin() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin-platform-jvm'
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], targetPlatformKind)
        }
    }

    @Test
    fun testJsImportByPlatformPlugin() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin-platform-js'
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
        }
    }

    @Test
    fun testCommonImportByPlatformPlugin() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin-platform-common'
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Common, targetPlatformKind)
        }
    }

    @Test
    fun testJvmImportByKotlinPlugin() {
        createProjectSubFile("build.gradle", """
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
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], targetPlatformKind)
        }
    }

    @Test
    fun testJsImportByKotlin2JsPlugin() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin2js'
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", languageLevel!!.versionString)
            Assert.assertEquals("1.1", apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
        }
    }

    @Test
    fun testArgumentEscaping() {
        createProjectSubFile("build.gradle", """
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

            apply plugin: 'kotlin-platform-jvm'

            compileKotlin {
                kotlinOptions.jdkHome = "c:\\program files\\jdk1.8"
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(
                    listOf("-jdk-home", "c:/program files/jdk1.8", "-Xmulti-platform"),
                    compilerSettings!!.additionalArgumentsAsList
            )
        }
    }

    @Test
    fun testNoPluginsInAdditionalArgs() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-allopen:1.1.0")
                }
            }

            apply plugin: 'kotlin'
            apply plugin: "kotlin-spring"
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(
                    "-version",
                    compilerSettings!!.additionalArguments
            )
            Assert.assertEquals(
                    listOf("plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.stereotype.Component",
                           "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.transaction.annotation.Transactional",
                           "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.scheduling.annotation.Async",
                           "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.cache.annotation.Cacheable"),
                    compilerArguments!!.pluginOptions.toList()
            )
        }
    }

    @Test
    fun testAndroidGradleJsDetection() {
        createProjectSubFile("android-module/build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    jcenter()
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:2.3.0"
                }
            }

            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 23
                buildToolsVersion "23.0.1"

                defaultConfig {
                    minSdkVersion 11
                    targetSdkVersion 23
                    versionCode 1002003
                    versionName version
                }

                dataBinding {
                    enabled = true
                }

                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_7
                    targetCompatibility JavaVersion.VERSION_1_7
                }

                buildTypes {
                    debug {
                        applicationIdSuffix ".debug"
                        versionNameSuffix "-debug"
                    }
                    release {
                        minifyEnabled true
                        shrinkResources true
                    }
                }
            }
        """)
        createProjectSubFile("android-module/src/main/AndroidManifest.xml", """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                      xmlns:tools="http://schemas.android.com/tools"
                      package="my.test.project" >
            </manifest>
        """)
        createProjectSubFile("js-module/build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                    maven {
                        url 'http://dl.bintray.com/kotlin/kotlin-dev'
                    }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.2-eap-44")
                }
            }

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.0"
            }
        """)
        createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenLocal()
                    maven {
                        url='https://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                    jcenter()
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:2.3.0"
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
                }
            }

            ext {
                androidBuildToolsVersion = '23.0.1'
            }

            allprojects {
                repositories {
                    mavenLocal()
                    maven {
                        url='https://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                    jcenter()
                }
            }
        """)
        createProjectSubFile("settings.gradle", """
            rootProject.name = "android-js-test"
            include ':android-module'
            include ':js-module'
        """)
        createProjectSubFile("local.properties", """
            sdk.dir=/${StringUtil.escapeBackSlashes(File(homePath).parent + "/dependencies/androidSDK")}
        """)
        importProject()

        with (facetSettings("js-module")) {
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
        }
    }

    @Test
    fun testKotlinAndroidPluginDetection() {
        createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    jcenter()
                    maven {
                        url='https://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:2.3.0"
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
                }
            }

            apply plugin: 'com.android.application'
            apply plugin: 'kotlin-android'

            android {
                compileSdkVersion 23
                buildToolsVersion "23.0.1"

                defaultConfig {
                    minSdkVersion 11
                    targetSdkVersion 23
                    versionCode 1002003
                    versionName version
                }

                dataBinding {
                    enabled = true
                }

                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_7
                    targetCompatibility JavaVersion.VERSION_1_7
                }

                buildTypes {
                    debug {
                        applicationIdSuffix ".debug"
                        versionNameSuffix "-debug"
                    }
                    release {
                        minifyEnabled true
                        shrinkResources true
                    }
                }
            }
        """)
        createProjectSubFile("local.properties", """
            sdk.dir=/${StringUtil.escapeBackSlashes(File(homePath).parent + "/dependencies/androidSDK")}
        """)
        createProjectSubFile("src/main/AndroidManifest.xml", """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                      xmlns:tools="http://schemas.android.com/tools"
                      package="my.test.project" >
            </manifest>
        """)
        importProject()

        Assert.assertNotNull(KotlinFacet.get(getModule("project")))
    }

    @Test
    fun testNoFacetInModuleWithoutKotlinPlugin() {
        createProjectSubFile("build.gradle", """
            group 'gr01'
            version '1.0-SNAPSHOT'

            apply plugin: 'java'
            apply plugin: 'kotlin'

            sourceCompatibility = 1.8

            repositories {
                mavenCentral()
            }

            buildscript {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.1"
                }
            }
            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-jre8:1.1.1"
            }
        """)
        createProjectSubFile("settings.gradle", """
            rootProject.name = 'gr01'
            include 'm1'
        """)
        createProjectSubFile("m1/build.gradle", """
            group 'gr01'
            version '1.0-SNAPSHOT'

            apply plugin: 'java'

            sourceCompatibility = 1.8

            repositories {
                mavenCentral()
            }

            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            dependencies {
                testCompile group: 'junit', name: 'junit', version: '4.11'
            }
        """)
        importProject()

        Assert.assertNotNull(KotlinFacet.get(getModule("gr01_main")))
        Assert.assertNotNull(KotlinFacet.get(getModule("gr01_test")))
        Assert.assertNull(KotlinFacet.get(getModule("m1_main")))
        Assert.assertNull(KotlinFacet.get(getModule("m1_test")))
    }
}
