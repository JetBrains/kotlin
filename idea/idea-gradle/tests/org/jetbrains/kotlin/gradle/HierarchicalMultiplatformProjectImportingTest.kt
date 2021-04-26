/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.ResourceKotlinRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.After
import org.junit.Before
import org.junit.Test

class HierarchicalMultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {

    @Before
    fun saveSdksBeforeTest() {
        val kotlinSdks = sdkCreationChecker?.getKotlinSdks() ?: emptyList()
        if (kotlinSdks.isNotEmpty()) {
            fail("Found Kotlin SDK before importing test. Sdk list: $kotlinSdks")
        }
    }

    @After
    fun checkSdkCreated() {
        if (sdkCreationChecker?.isKotlinSdkCreated() == false) {
            fail("Kotlin SDK was not created during import of HMPP Project.")
        }
    }

    @Test
    @PluginTargetVersions()
    fun testImportHMPPFlag() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            allModules {
                isHMPP(true)
            }
            module("my-app.commonMain")
            module("my-app.jvmAndJsMain")
        }
    }

    @Test
    @PluginTargetVersions()
    fun testImportIntermediateModules() {
        configureByFiles()
        importProject()

        checkProjectStructure {
            module("my-app")
            module("my-app.commonMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16, NativePlatforms.unspecifiedNativePlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                sourceFolder("src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/commonMain/resources", ResourceKotlinRootType)
            }

            module("my-app.commonTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16, NativePlatforms.unspecifiedNativePlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                sourceFolder("src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/commonTest/resources", TestResourceKotlinRootType)
            }

            module("my-app.jsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("my-app.jvmAndJsMain", DependencyScope.COMPILE)
                moduleDependency("my-app.linuxAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/jsMain/resources", ResourceKotlinRootType)
            }

            module("my-app.jsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                moduleDependency("my-app.commonTest", DependencyScope.TEST)
                moduleDependency("my-app.jsMain", DependencyScope.TEST)
                moduleDependency("my-app.jsMain", DependencyScope.RUNTIME)  // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("my-app.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("my-app.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("my-app.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("my-app.linuxAndJsTest", DependencyScope.TEST)
                sourceFolder("src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/jsTest/resources", TestResourceKotlinRootType)
            }

            module("my-app.jvmAndJsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("my-app.commonMain", DependencyScope.COMPILE)
                sourceFolder("src/jvmAndJsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/jvmAndJsMain/resources", ResourceKotlinRootType)
            }

            module("my-app.jvmAndJsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                moduleDependency("my-app.commonTest", DependencyScope.TEST)
                moduleDependency("my-app.jvmAndJsMain", DependencyScope.TEST)
                sourceFolder("src/jvmAndJsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/jvmAndJsTest/resources", TestResourceKotlinRootType)
            }

            module("my-app.jvmMain") {
                isHMPP(true)
                targetPlatform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("my-app.jvmAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("src/jvmMain/resources", JavaResourceRootType.RESOURCE)
            }

            module("my-app.jvmTest") {
                isHMPP(true)
                targetPlatform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: junit:junit:4.12", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-junit:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                moduleDependency("my-app.commonTest", DependencyScope.TEST)
                moduleDependency("my-app.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("my-app.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("my-app.jvmMain", DependencyScope.TEST)
                moduleDependency("my-app.jvmMain", DependencyScope.RUNTIME)  // Temporary dependency, need to remove after KT-40551 is solved
                sourceFolder("src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
            }

            module("my-app.linuxAndJsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, NativePlatforms.unspecifiedNativePlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("my-app.commonMain", DependencyScope.COMPILE)
                sourceFolder("src/linuxAndJsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/linuxAndJsMain/resources", ResourceKotlinRootType)
            }

            module("my-app.linuxAndJsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, NativePlatforms.unspecifiedNativePlatform)
                sourceFolder("src/linuxAndJsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/linuxAndJsTest/resources", TestResourceKotlinRootType)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                moduleDependency("my-app.commonTest", DependencyScope.TEST)
                moduleDependency("my-app.linuxAndJsMain", DependencyScope.TEST)
            }

            module("my-app.linuxX64Main") {
                isHMPP(true)
                targetPlatform(NativePlatforms.nativePlatformBySingleTarget(KonanTarget.LINUX_X64))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - zlib [linux_x64]", DependencyScope.PROVIDED)
                moduleDependency("my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("my-app.linuxAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/linuxX64Main/kotlin", SourceKotlinRootType)
                sourceFolder("src/linuxX64Main/resources", ResourceKotlinRootType)
            }

            module("my-app.linuxX64Test") {
                isHMPP(true)
                targetPlatform(NativePlatforms.nativePlatformBySingleTarget(KonanTarget.LINUX_X64))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - zlib [linux_x64]", DependencyScope.PROVIDED)
                moduleDependency("my-app.commonMain", DependencyScope.TEST)
                moduleDependency("my-app.commonTest", DependencyScope.TEST)
                moduleDependency("my-app.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("my-app.linuxAndJsTest", DependencyScope.TEST)
                moduleDependency("my-app.linuxX64Main", DependencyScope.TEST)
                sourceFolder("src/linuxX64Test/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/linuxX64Test/resources", TestResourceKotlinRootType)
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testJvmWithJavaOnHMPP() {
        configureByFiles()
        importProject()

        checkProjectStructure(true, false, true) {
            module("jvm-on-mpp") {}
            module("jvm-on-mpp.jvm-mod") {}


            module("jvm-on-mpp.jvm-mod.main") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.main", DependencyScope.COMPILE)
            }
            module("jvm-on-mpp.jvm-mod.test") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.main", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.jvm-mod.main", DependencyScope.COMPILE)
            }
            module("jvm-on-mpp.hmpp-mod-a") {

            }
            module("jvm-on-mpp.hmpp-mod-a.commonMain") {

            }
            module("jvm-on-mpp.hmpp-mod-a.commonTest") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)

            }
            module("jvm-on-mpp.hmpp-mod-a.jsMain") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.COMPILE)
            }
            module("jvm-on-mpp.hmpp-mod-a.jsTest") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jsMain", DependencyScope.RUNTIME)  // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsTest", DependencyScope.TEST)
            }
            module("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)


            }
            module("jvm-on-mpp.hmpp-mod-a.jvmAndJsTest") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.TEST)

            }
            module("jvm-on-mpp.hmpp-mod-a.jvmMain") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.COMPILE)

            }
            module("jvm-on-mpp.hmpp-mod-a.jvmTest") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.jvmMain", DependencyScope.RUNTIME)  // Temporary dependency, need to remove after KT-40551 is solved
            }
            module("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)

            }
            module("jvm-on-mpp.hmpp-mod-a.linuxAndJsTest") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.TEST)

            }
            module("jvm-on-mpp.hmpp-mod-a.linuxX64Main") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.COMPILE)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.COMPILE)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - zlib [linux_x64]", DependencyScope.PROVIDED)
            }
            module("jvm-on-mpp.hmpp-mod-a.linuxX64Test") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxX64Main", DependencyScope.TEST)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${gradleKotlinPluginVersion} - zlib [linux_x64]", DependencyScope.PROVIDED)
            }

            module("jvm-on-mpp.hmpp-mod-a.main") {}
            module("jvm-on-mpp.hmpp-mod-a.test") {}
        }
    }

    @Test
    @PluginTargetVersions()
    fun testPrecisePlatformsHmpp() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val anyNative = NativePlatforms.unspecifiedNativePlatform
        val linux = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.LINUX_X64)
        val macos = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.MACOS_X64)
        val js = JsPlatforms.defaultJsPlatform

        checkProjectStructure(exhaustiveModuleList = true, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app") {
                targetPlatform(jvm)
            }

            module("my-app.commonMain") { targetPlatform(jvm, anyNative) }
            module("my-app.commonTest") { targetPlatform(jvm, anyNative) }

            module("my-app.jvmAndLinuxMain") { targetPlatform(jvm, anyNative) }
            module("my-app.jvmAndLinuxTest") { targetPlatform(jvm, anyNative) }

            module("my-app.jvmMain") { targetPlatform(jvm) }
            module("my-app.jvmTest") { targetPlatform(jvm) }

            module("my-app.linuxX64Main") { targetPlatform(linux) }
            module("my-app.linuxX64Test") { targetPlatform(linux) }

            module("my-app.macosX64Main") { targetPlatform(macos) }
            module("my-app.macosX64Test") { targetPlatform(macos) }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testPrecisePlatformsWithUnrelatedModuleHmpp() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val anyNative = NativePlatforms.unspecifiedNativePlatform
        val js = JsPlatforms.defaultJsPlatform
        val linux = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.LINUX_X64)
        val macos = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.MACOS_X64)

        checkProjectStructure(exhaustiveModuleList = true, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            // root project
            module("my-app") {
                targetPlatform(jvm)
            }

            module("my-app.commonMain") { targetPlatform(jvm, anyNative) }
            module("my-app.commonTest") { targetPlatform(jvm, anyNative) }

            module("my-app.jvmAndLinuxMain") { targetPlatform(jvm, anyNative) }
            module("my-app.jvmAndLinuxTest") { targetPlatform(jvm, anyNative) }

            module("my-app.jvmMain") { targetPlatform(jvm) }
            module("my-app.jvmTest") { targetPlatform(jvm) }

            module("my-app.linuxX64Main") { targetPlatform(linux) }
            module("my-app.linuxX64Test") { targetPlatform(linux) }

            module("my-app.macosX64Main") { targetPlatform(macos) }
            module("my-app.macosX64Test") { targetPlatform(macos) }


            // submodule
            module("my-app.submodule") { targetPlatform(jvm) }

            module("my-app.submodule.commonMain") { targetPlatform(js, jvm) }
            module("my-app.submodule.commonTest") { targetPlatform(js, jvm) }

            module("my-app.submodule.jvmMain") { targetPlatform(jvm) }
            module("my-app.submodule.jvmTest") { targetPlatform(jvm) }

            module("my-app.submodule.jsMain") { targetPlatform(js) }
            module("my-app.submodule.jsTest") { targetPlatform(js) }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testOrphanSourceSet() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val js = JsPlatforms.defaultJsPlatform
        val native = NativePlatforms.unspecifiedNativePlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.commonMain") {
                // must not be (jvm, js, native)
                targetPlatform(jvm, js)
            }

            module("my-app.orphan") {
                targetPlatform(jvm, js)
            }
            module("my-app") {
                assertDiagnosticsCount<OrphanSourceSetsImportingDiagnostic>(1)
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testSourceSetIncludedIntoCompilationDirectly() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val js = JsPlatforms.defaultJsPlatform
        val native = NativePlatforms.unspecifiedNativePlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.commonMain") {
                targetPlatform(jvm, js) // must not be (jvm, js, native)
            }

            module("my-app.includedIntoJvm") {
                targetPlatform(jvm) // !
            }

            module("my-app.includedIntoJvmAndJs") {
                targetPlatform(jvm, js) // !
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testDefaultSourceSetDependsOnDefaultSourceSet() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val js = JsPlatforms.defaultJsPlatform
        val native = NativePlatforms.unspecifiedNativePlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.commonMain") {
                targetPlatform(jvm, js) // must not be (jvm, js, native)
            }

            module("my-app.intermediateBetweenJsAndCommon") {
                targetPlatform(jvm, js) // must not be (jvm, js, native)
            }

            module("my-app.jsMain") {
                targetPlatform(js) // must not be (jvm, js)
            }

            module("my-app.jvmMain") {
                targetPlatform(jvm)
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testDefaultSourceSetIncludedIntoAnotherCompilationDirectly() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val js = JsPlatforms.defaultJsPlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.jvmMain") {
                targetPlatform(jvm) // must not be (jvm, js)
            }

            module("my-app.jsMain") {
                targetPlatform(js)
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testSourceSetsWithDependsOnButNotIncludedIntoCompilation() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform
        val js = JsPlatforms.defaultJsPlatform
        val native = NativePlatforms.unspecifiedNativePlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {

            module("my-app") {
                assertDiagnosticsCount<OrphanSourceSetsImportingDiagnostic>(3)
            }

            // (jvm, js, native) is highly undesirable
            module("my-app.danglingOnJvm") {
                targetPlatform(jvm, js)
            }

            module("my-app.commonMain") {
                targetPlatform(jvm, js)
            }

            module("my-app.danglingOnCommon") {
                targetPlatform(jvm, js)
            }

            module("my-app.danglingOnJvmAndJs") {
                targetPlatform(jvm, js)
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testCustomAddToCompilationPlusDependsOn() {
        configureByFiles()
        importProject()

        val jvm = JvmPlatforms.defaultJvmPlatform

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.includedIntoJvm") {
                targetPlatform(jvm) // !
            }

            module("my-app.pseudoOrphan") {
                targetPlatform(jvm) // !
            }
        }
    }

    @Test
    @PluginTargetVersions()
    fun testCommonMainIsSingleBackend() {
        configureByFiles()
        importProject()

        val macosX64 = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.MACOS_X64)
        val linuxX64 = NativePlatforms.nativePlatformBySingleTarget(KonanTarget.LINUX_X64)

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            module("my-app.commonMain") { targetPlatform(macosX64, linuxX64) }
            module("my-app.commonTest") { targetPlatform(macosX64, linuxX64) }

            module("my-app.linuxX64Main") { targetPlatform(linuxX64) }
            module("my-app.linuxX64Test") { targetPlatform(linuxX64) }

            module("my-app.macosX64Test") { targetPlatform(macosX64) }
            module("my-app.macosX64Main") { targetPlatform(macosX64) }
        }
    }

    private fun checkProjectStructure(
        exhaustiveModuleList: Boolean = true,
        exhaustiveSourceSourceRootList: Boolean = true,
        exhaustiveDependencyList: Boolean = true,
        body: ProjectInfo.() -> Unit = {}
    ) {
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList,
            exhaustiveSourceSourceRootList,
            exhaustiveDependencyList,
            false,
            body
        )
    }

    override fun importProject() {
        val isUseQualifiedModuleNames = currentExternalProjectSettings.isUseQualifiedModuleNames
        currentExternalProjectSettings.isUseQualifiedModuleNames = true
        try {
            super.importProject(true)
        } finally {
            currentExternalProjectSettings.isUseQualifiedModuleNames = isUseQualifiedModuleNames
        }
    }

    override fun testDataDirName(): String {
        return "hierarchicalMultiplatformImport"
    }
}