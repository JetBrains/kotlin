/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.junit.After
import org.junit.Before
import org.junit.Test

class HierarchicalMultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {
    private fun kotlinVersion() = if (gradleKotlinPluginVersion == MINIMAL_SUPPORTED_VERSION) "1.3.50" else gradleKotlinPluginVersion

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
    fun testImportHMPPFlag() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveModuleList = false, exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false) {
            allModules {
                isHMPP(true)
            }
            module("project.my-app.commonMain")
            module("project.my-app.jvmAndJsMain")
        }
    }

    @Test
    fun testImportIntermediateModules() {
        configureByFiles()
        importProject()

        checkProjectStructure {
            module("my-app")
            module("project.my-app.commonMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16, KonanPlatforms.defaultKonanPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                sourceFolder("src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/commonMain/resources", ResourceKotlinRootType)
            }

            module("project.my-app.commonTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16, KonanPlatforms.defaultKonanPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                sourceFolder("src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/commonTest/resources", TestResourceKotlinRootType)
            }

            module("project.my-app.jsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${kotlinVersion()}", DependencyScope.COMPILE)
                moduleDependency("project.my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.my-app.jvmAndJsMain", DependencyScope.COMPILE)
                moduleDependency("project.my-app.linuxAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/jsMain/resources", ResourceKotlinRootType)
            }

            module("project.my-app.jsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-js:${kotlinVersion()}", DependencyScope.TEST)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                moduleDependency("project.my-app.commonTest", DependencyScope.TEST)
                moduleDependency("project.my-app.jsMain", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxAndJsTest", DependencyScope.TEST)
                sourceFolder("src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/jsTest/resources", TestResourceKotlinRootType)
            }

            module("project.my-app.jvmAndJsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                moduleDependency("project.my-app.commonMain", DependencyScope.COMPILE)
                sourceFolder("src/jvmAndJsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/jvmAndJsMain/resources", ResourceKotlinRootType)
            }

            module("project.my-app.jvmAndJsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                moduleDependency("project.my-app.commonTest", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmAndJsMain", DependencyScope.TEST)
                sourceFolder("src/jvmAndJsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/jvmAndJsTest/resources", TestResourceKotlinRootType)
            }

            module("project.my-app.jvmMain") {
                isHMPP(true)
                targetPlatform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion()}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("project.my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.my-app.jvmAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("src/jvmMain/resources", JavaResourceRootType.RESOURCE)
            }

            module("project.my-app.jvmTest") {
                isHMPP(true)
                targetPlatform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: junit:junit:4.12", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-junit:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                moduleDependency("project.my-app.commonTest", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmAndJsMain", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmAndJsTest", DependencyScope.TEST)
                moduleDependency("project.my-app.jvmMain", DependencyScope.TEST)
                sourceFolder("src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
            }

            module("project.my-app.linuxAndJsMain") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, KonanPlatforms.defaultKonanPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                moduleDependency("project.my-app.commonMain", DependencyScope.COMPILE)
                sourceFolder("src/linuxAndJsMain/kotlin", SourceKotlinRootType)
                sourceFolder("src/linuxAndJsMain/resources", ResourceKotlinRootType)
            }

            module("project.my-app.linuxAndJsTest") {
                isHMPP(true)
                targetPlatform(JsPlatforms.defaultJsPlatform, KonanPlatforms.defaultKonanPlatform)
                sourceFolder("src/linuxAndJsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/linuxAndJsTest/resources", TestResourceKotlinRootType)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                moduleDependency("project.my-app.commonTest", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxAndJsMain", DependencyScope.TEST)
            }

            module("project.my-app.linuxX64Main") {
                isHMPP(true)
                targetPlatform(KonanPlatforms.defaultKonanPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.COMPILE)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - zlib [linux_x64]", DependencyScope.PROVIDED)
                moduleDependency("project.my-app.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.my-app.linuxAndJsMain", DependencyScope.COMPILE)
                sourceFolder("src/linuxX64Main/kotlin", SourceKotlinRootType)
                sourceFolder("src/linuxX64Main/resources", ResourceKotlinRootType)
            }

            module("project.my-app.linuxX64Test") {
                isHMPP(true)
                targetPlatform(KonanPlatforms.defaultKonanPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-annotations-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-test-common:${kotlinVersion()}", DependencyScope.TEST)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - zlib [linux_x64]", DependencyScope.PROVIDED)
                moduleDependency("project.my-app.commonMain", DependencyScope.TEST)
                moduleDependency("project.my-app.commonTest", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxAndJsTest", DependencyScope.TEST)
                moduleDependency("project.my-app.linuxX64Main", DependencyScope.TEST)
                sourceFolder("src/linuxX64Test/kotlin", TestSourceKotlinRootType)
                sourceFolder("src/linuxX64Test/resources", TestResourceKotlinRootType)
            }
        }
    }

    @Test
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
                libraryDependency("Kotlin/Native ${kotlinVersion()} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - zlib [linux_x64]", DependencyScope.PROVIDED)
            }
            module("jvm-on-mpp.hmpp-mod-a.linuxX64Test") {
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.commonTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsMain", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxAndJsTest", DependencyScope.TEST)
                moduleDependency("jvm-on-mpp.hmpp-mod-a.linuxX64Main", DependencyScope.TEST)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - builtin [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - iconv [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - linux [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - posix [linux_x64]", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Kotlin/Native ${kotlinVersion()} - zlib [linux_x64]", DependencyScope.PROVIDED)
            }
            module("jvm-on-mpp.hmpp-mod-a.main") {}
            module("jvm-on-mpp.hmpp-mod-a.test") {}
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
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject()
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
            currentExternalProjectSettings.isUseQualifiedModuleNames = isUseQualifiedModuleNames
        }
    }

    override fun testDataDirName(): String {
        return "hierarchicalMultiplatformImport"
    }
}