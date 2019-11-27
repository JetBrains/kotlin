/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.After
import org.junit.Before
import org.junit.Test

class NewMultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {

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
            fail("Kotlin SDK was not created during import of MPP Project.")
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.10+")
    fun testProjectDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure() {
            allModules {
                languageVersion("1.3")
                apiVersion("1.3")
                when (module.name) {
                    "project", "app", "lib" -> additionalArguments(null)
                    "app_jvmMain", "app_jvmTest", "lib_jvmMain", "lib_jvmTest" ->
                        additionalArguments(
                            if (VersionComparatorUtil.compare(gradleKotlinPluginVersion, "1.3.50") < 0)
                                "-version"
                            else
                                "-Xallow-no-source-files"
                        )
                    else -> additionalArguments("-version")
                }
            }

            module("project")
            module("app")
            module("app_commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/commonMain/resources", ResourceKotlinRootType)
                inheritProjectOutput()
            }
            module("app_commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                sourceFolder("app/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/commonTest/resources", TestResourceKotlinRootType)
                inheritProjectOutput()
            }
            module("app_jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("lib_jsMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/jsMain/resources", ResourceKotlinRootType)
                outputPath("app/build/classes/kotlin/js/main", true)
            }
            module("app_jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("lib_jsMain", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonTest", DependencyScope.TEST)
                moduleDependency("app_jsMain", DependencyScope.TEST)
                sourceFolder("app/src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/jsTest/resources", TestResourceKotlinRootType)
                outputPath("app/build/classes/kotlin/js/test", false)
            }
            module("app_jvmMain") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_jvmMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_main", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/main", true)
            }
            module("app_jvmTest") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_jvmMain", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_test", DependencyScope.TEST)
                moduleDependency("app_jvmMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonTest", DependencyScope.TEST)
                sourceFolder("app/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/test", false)
            }
            module("app_main") {
                platform(JvmPlatforms.jvm18)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/resources", JavaResourceRootType.RESOURCE)
                inheritProjectOutput()
            }
            module("app_test") {
                platform(JvmPlatforms.jvm18)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_main", DependencyScope.TEST)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                inheritProjectOutput()
            }
            module("lib")
            module("lib_commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                sourceFolder("lib/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("lib/src/commonMain/resources", ResourceKotlinRootType)
                inheritProjectOutput()
            }
            module("lib_commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                sourceFolder("lib/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("lib/src/commonTest/resources", TestResourceKotlinRootType)
                inheritProjectOutput()
            }
            module("lib_jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("lib/src/jsMain/resources", ResourceKotlinRootType)
                outputPath("lib/build/classes/kotlin/js/main", true)
            }
            module("lib_jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("lib_commonTest", DependencyScope.TEST)
                moduleDependency("lib_jsMain", DependencyScope.TEST)
                sourceFolder("lib/src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("lib/src/jsTest/resources", TestResourceKotlinRootType)
                outputPath("lib/build/classes/kotlin/js/test", false)
            }
            module("lib_jvmMain") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("lib/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/main", true)
            }
            module("lib_jvmTest") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_commonTest", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("lib_jvmMain", DependencyScope.TEST)
                sourceFolder("lib/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("lib/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/test", false)
            }
        }
    }

    @Test
    fun testFileCollectionDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false
        ) {
            module("project_jvmMain") {
                libraryDependencyByUrl("file://$projectPath/a", DependencyScope.COMPILE)
                libraryDependencyByUrl("file://$projectPath/b", DependencyScope.COMPILE)
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
                moduleDependency("project_main", DependencyScope.COMPILE)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.30+")
    fun testUnresolvedDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false
        ) {
            module("project")
            module("project_commonMain")
            module("project_commonTest")
            module("project_jvmMain")
            module("project_jvmTest")
            module("project_main")
            module("project_test")
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.30+")
    fun testAndroidDependencyOnMPP() {
        configureByFiles()
        createProjectSubFile(
            "local.properties",
            "sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}"
        )
        importProject()

        checkProjectStructure {
            module("project")
            module("app") {
                libraryDependency("Gradle: android-android-26", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.core:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.core:runtime:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:livedata-core:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:runtime:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:viewmodel:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout:1.1.3@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout-solver:1.1.3@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-core:3.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-idling-resource:3.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:monitor:1.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:runner:1.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support:animated-vector-drawable:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:appcompat-v7:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-annotations:27.1.1@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-compat:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-ui:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-utils:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-fragment:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-vector-drawable:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.google.code.findbugs:jsr305:2.0.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: com.squareup:javawriter:2.1.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: javax.inject:javax.inject:1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: junit:junit:4.12@jar", DependencyScope.TEST)
                libraryDependency("Gradle: net.sf.kxml:kxml2:2.3.0@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-core:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-integration:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-library:1.3@jar", DependencyScope.TEST)
                if (gradleKotlinPluginVersion != MINIMAL_SUPPORTED_VERSION) {
                    libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-android-extensions-runtime:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                }
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-jdk7:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0@jar", DependencyScope.COMPILE)
                moduleDependency("shared", DependencyScope.COMPILE)
                moduleDependency("shared_androidMain", DependencyScope.COMPILE)
                moduleDependency("shared_androidTest", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
            }
            module("shared")
            module("shared_commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                sourceFolder("shared/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("shared/src/commonMain/resources", ResourceKotlinRootType)
            }
            module("shared_commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                sourceFolder("shared/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("shared/src/commonTest/resources", TestResourceKotlinRootType)
            }
            module("shared_androidMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/androidMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("shared/src/androidMain/resources", JavaResourceRootType.RESOURCE)
            }
            module("shared_androidTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("shared_androidMain", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/androidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("shared/src/androidTest/resources", JavaResourceRootType.TEST_RESOURCE)
            }
            var nativeVersion = gradleKotlinPluginVersion
            module("shared_iOSMain") {
                libraryDependency("Kotlin/Native $nativeVersion - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/iOSMain/kotlin", SourceKotlinRootType)
                sourceFolder("shared/src/iOSMain/resources", ResourceKotlinRootType)
            }
            module("shared_iOSTest") {
                libraryDependency("Kotlin/Native $nativeVersion - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("shared_iOSMain", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/iOSTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("shared/src/iOSTest/resources", TestResourceKotlinRootType)
            }
        }
    }

    @Test
    fun testTestTasks() {
        val files = configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")
            module("common")
            module("jvm")
            module("js")

            module("project_commonMain")
            module("project_commonTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
            }

            module("project_jvmMain") {
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
            }

            module("project_jvmTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
                moduleDependency("project_commonTest", DependencyScope.TEST)
                moduleDependency("project_jvmMain", DependencyScope.TEST)
            }
        }

        val commonTestFile = files.find { it.path.contains("commonTest") }!!
        val commonTasks = findTasksToRun(commonTestFile)
        if (commonTasks != null) {
            assertEquals(listOf(":cleanJvmTest", ":jvmTest"), commonTasks)
        }

        val jvmTestFile = files.find { it.path.contains("jvmTest") }!!
        val jvmTasks = findTasksToRun(jvmTestFile)
        if (jvmTasks != null) {
            assertEquals(listOf(":cleanJvmTest", ":jvmTest"), jvmTasks)
        }
    }


    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.50+")
    fun testImportTestsAndTargets() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false, exhaustiveTestsList = true) {
            module("project")
            module("project_commonMain")
            module("project_commonTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")
            }
            module("project_jsMain")
            module("project_jsTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
            }
            module("project_jvmMain")
            module("project_jvmTest") {
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")
            }
        }
    }


    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.50+")
    fun testSingleAndroidTarget() {
        configureByFiles()
        importProject()
        checkProjectStructure(exhaustiveDependencyList = false) {
            module("app") {
                sourceFolder("app/src/androidAndroidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidAndroidTestDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidDebug/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidDebugAndroidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidDebugUnitTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidRelease/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidReleaseUnitTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTest/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestDebug/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestRelease/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/debug/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/debug/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/release/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/release/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testDebug/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testRelease/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testRelease/kotlin", JavaSourceRootType.TEST_SOURCE)

                sourceFolder("app/src/androidAndroidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidAndroidTestDebug/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidDebug/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidDebugAndroidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidDebugUnitTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidMain/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidRelease/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidReleaseUnitTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTestDebug/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTestRelease/resources", JavaResourceRootType.TEST_RESOURCE)
            }
            module("app_commonMain") {
                sourceFolder("app/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/commonMain/resources", ResourceKotlinRootType)
            }
            module("app_commonTest") {
                sourceFolder("app/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/commonTest/resources", TestResourceKotlinRootType)
            }
            module("project")
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.10+")
    fun testDependencyOnRoot() {
        configureByFiles()
        importProject()
        checkProjectStructure(exhaustiveSourceSourceRootList = false) {

            module("project")
            module("project_commonMain")
            module("project_commonTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
            }
            module("project_jvmMain") {
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
            }
            module("project_jvmTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
                moduleDependency("project_commonTest", DependencyScope.TEST)
                moduleDependency("project_jvmMain", DependencyScope.TEST)
            }

            module("subproject")
            module("subproject_commonMain") {
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
            }
            module("subproject_commonTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
                moduleDependency("subproject_commonMain", DependencyScope.TEST)
            }
            module("subproject_jvmMain") {
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
                moduleDependency("subproject_commonMain", DependencyScope.COMPILE)
                moduleDependency("project_jvmMain", DependencyScope.COMPILE)
            }
            module("subproject_jvmTest") {
                moduleDependency("project_commonMain", DependencyScope.TEST)
                moduleDependency("subproject_commonMain", DependencyScope.TEST)
                moduleDependency("subproject_commonTest", DependencyScope.TEST)
                moduleDependency("project_jvmMain", DependencyScope.TEST)
                moduleDependency("subproject_jvmMain", DependencyScope.TEST)
            }
        }

    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.10+")
    fun testNestedDependencies() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")
            module("aaa")
            module("aaa_commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("bbb_commonMain", DependencyScope.COMPILE)
                moduleDependency("ccc_commonMain", DependencyScope.COMPILE)
            }
            module("aaa_commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("aaa_commonMain", DependencyScope.TEST)
                moduleDependency("bbb_commonMain", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
            }
            module("aaa_jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("aaa_commonMain", DependencyScope.COMPILE)
                moduleDependency("bbb_commonMain", DependencyScope.COMPILE)
                moduleDependency("bbb_jvmMain", DependencyScope.COMPILE)
                moduleDependency("ccc_commonMain", DependencyScope.COMPILE)
                moduleDependency("ccc_jvmMain", DependencyScope.COMPILE)
            }
            module("aaa_jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("aaa_commonMain", DependencyScope.TEST)
                moduleDependency("aaa_commonTest", DependencyScope.TEST)
                moduleDependency("aaa_jvmMain", DependencyScope.TEST)
                moduleDependency("bbb_commonMain", DependencyScope.TEST)
                moduleDependency("bbb_jvmMain", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
                moduleDependency("ccc_jvmMain", DependencyScope.TEST)
            }
            module("bbb")
            module("bbb_commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("ccc_commonMain", DependencyScope.COMPILE)
            }
            module("bbb_commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("bbb_commonMain", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
            }
            module("bbb_jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("bbb_commonMain", DependencyScope.COMPILE)
                moduleDependency("ccc_commonMain", DependencyScope.COMPILE)
                moduleDependency("ccc_jvmMain", DependencyScope.COMPILE)
            }
            module("bbb_jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("bbb_commonMain", DependencyScope.TEST)
                moduleDependency("bbb_commonTest", DependencyScope.TEST)
                moduleDependency("bbb_jvmMain", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
                moduleDependency("ccc_jvmMain", DependencyScope.TEST)
            }
            module("ccc")
            module("ccc_commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
            }
            module("ccc_commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
            }
            module("ccc_jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("ccc_commonMain", DependencyScope.COMPILE)
            }
            module("ccc_jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("ccc_commonMain", DependencyScope.TEST)
                moduleDependency("ccc_commonTest", DependencyScope.TEST)
                moduleDependency("ccc_jvmMain", DependencyScope.TEST)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.20+")
    fun testDetectAndroidSources() {
        configureByFiles()
        createProjectSubFile(
            "local.properties",
            "sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}"
        )
        importProject(true)
        checkProjectStructure(exhaustiveModuleList = false, exhaustiveDependencyList = false, exhaustiveSourceSourceRootList = false) {
            module("multiplatformb") {
                sourceFolder("multiplatformb/src/androidMain/kotlin", JavaSourceRootType.SOURCE)


            }
        }
    }

    /**
     * This test is inherited form testPlatformToCommonExpectedByInComposite and actually tests
     * dependencies in multiplatform project included in composite build
     */
    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.20+")
    fun testPlatformToCommonExpByInComposite() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")
            module("project.commonMain")
            module("project.commonTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
            }
            module("toInclude")
            module("toInclude.commonMain")
            module("toInclude.commonTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
            }
            module("toInclude.jsMain") {
                moduleDependency("toInclude.commonMain", DependencyScope.COMPILE)
            }

            module("toInclude.jsTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
                moduleDependency("toInclude.commonTest", DependencyScope.TEST)
                moduleDependency("toInclude.jsMain", DependencyScope.TEST)
            }
            module("toInclude.jvmMain") {
                moduleDependency("toInclude.commonMain", DependencyScope.COMPILE)
            }
            module("toInclude.jvmTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
                moduleDependency("toInclude.commonTest", DependencyScope.TEST)
                moduleDependency("toInclude.jvmMain", DependencyScope.TEST)
            }
        }
    }

    /**
     * Test case for issue https://youtrack.jetbrains.com/issue/KT-29757
     */
    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.40+")
    fun testJavaTransitiveOnMPP() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("project") {}
            module("project.jvm") {}
            module("project.jvm.main") {
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
            }
            module("project.jvm.test") {
                moduleDependency("project.jvm.main", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
            }
            module("project.mpp") {}
            module("project.mpp.commonMain") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
            }
            module("project.mpp.commonTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp.commonMain", DependencyScope.TEST)
            }
            module("project.mpp.jvmMain") {
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
            }
            module("project.mpp.jvmTest") {
                moduleDependency("project.mpp.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp.commonTest", DependencyScope.TEST, true)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.TEST)
                moduleDependency("project.mpp.jvmMain", DependencyScope.TEST)
            }

            module("project.mpp-base") {}
            module("project.mpp-base.commonMain") {}
            module("project.mpp-base.commonTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
            }
            module("project.mpp-base.jvmMain") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
            }
            module("project.mpp-base.jvmTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.TEST)
                moduleDependency("project.mpp-base.commonTest", DependencyScope.TEST, true)
            }
        }
    }

    /**
     * Test case for issue https://youtrack.jetbrains.com/issue/KT-28822
     */
    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.41+")
    fun testImportBeforeBuild() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("mpp-jardep") {}
            module("mpp-jardep.java-project") {}
            module("mpp-jardep.java-project.main") {
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)

            }
            module("mpp-jardep.java-project.test") {
                moduleDependency("mpp-jardep.java-project.main", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)
            }

            module("mpp-jardep.library1") {}
            module("mpp-jardep.library1.commonMain") {}
            module("mpp-jardep.library1.commonTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST)

            }
            module("mpp-jardep.library1.jvmMain") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)

            }
            module("mpp-jardep.library1.jvmTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library1.commonTest", DependencyScope.TEST, true)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.TEST)
            }

            module("mpp-jardep.library2") {}
            module("mpp-jardep.library2.commonMain") {}
            module("mpp-jardep.library2.commonTest") {
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.TEST)

            }
            module("mpp-jardep.library2.jvmMain") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)

            }
            module("mpp-jardep.library2.jvmTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library2.commonTest", DependencyScope.TEST, true)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.TEST)
            }
        }
    }


    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.20+")
    fun testProductionOnTestFlag() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(false, false, false ) {
            module("project.javaModule.test") {
                moduleDependency("project.mppModule.jvmTest", DependencyScope.COMPILE, true)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.30+")
    fun testJvmWithJava() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("jvm-on-mpp") {}
            module("jvm-on-mpp.jvm-mod") {}
            module("jvm-on-mpp.jvm-mod.main") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.COMPILE, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.main", DependencyScope.COMPILE, false)
            }
            module("jvm-on-mpp.jvm-mod.test") {
                moduleDependency("jvm-on-mpp.jvm-mod.main", DependencyScope.COMPILE, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.COMPILE, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.main", DependencyScope.COMPILE, false)
            }

            module("jvm-on-mpp.mpp-mod-a") {
            }
            module("jvm-on-mpp.mpp-mod-a.commonMain") {
            }
            module("jvm-on-mpp.mpp-mod-a.commonTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, false)
            }
            module("jvm-on-mpp.mpp-mod-a.jsMain") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, false)
            }
            module("jvm-on-mpp.mpp-mod-a.jsTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.jsMain", DependencyScope.TEST, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonTest", DependencyScope.TEST, true)
            }
            module("jvm-on-mpp.mpp-mod-a.jvmMain") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, false)
            }
            module("jvm-on-mpp.mpp-mod-a.jvmTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonTest", DependencyScope.TEST, true)
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.TEST, false)
            }

            //At the moment this is 'fake' source roots and they have no explicit dependencies.
            module("jvm-on-mpp.mpp-mod-a.main") {
            }
            module("jvm-on-mpp.mpp-mod-a.test") {
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersion = "4.0+", pluginVersion = "1.3.30+")
    fun testCommonTestTargetPlatform() {
        configureByFiles()
        importProject(true)
        checkProjectStructure(true, false, false) {
            module("KotlinMPPL") {}
            module("com.example.KotlinMPPL.commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
            }
            module("com.example.KotlinMPPL.commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
            }
            module("com.example.KotlinMPPL.jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
            }
            module("com.example.KotlinMPPL.jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
            }
        }
    }

    private fun checkProjectStructure(
        exhaustiveModuleList: Boolean = true,
        exhaustiveSourceSourceRootList: Boolean = true,
        exhaustiveDependencyList: Boolean = true,
        exhaustiveTestsList: Boolean = false,
        body: ProjectInfo.() -> Unit = {}
    ) {
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList,
            exhaustiveSourceSourceRootList,
            exhaustiveDependencyList,
            exhaustiveTestsList,
            body)
    }

    fun importProject(useQualifiedNames: Boolean) {
        val isUseQualifiedModuleNames = currentExternalProjectSettings.isUseQualifiedModuleNames
        currentExternalProjectSettings.isUseQualifiedModuleNames = useQualifiedNames
        try {
            importProject()
        } finally {
            currentExternalProjectSettings.isUseQualifiedModuleNames = isUseQualifiedModuleNames
        }
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

    override fun testDataDirName(): String {
        return "newMultiplatformImport"
    }
}