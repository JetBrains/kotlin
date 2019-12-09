/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import assertk.assertions.contains
import org.jetbrains.kotlin.ide.konan.gradle.KotlinGradleNativeMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleMobileMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleMobileSharedMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleSharedMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleWebMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class GradleMultiplatformWizardTest : AbstractGradleMultiplatformWizardTest() {

    lateinit var sdkCreationChecker: KotlinSdkCreationChecker

    override fun setUp() {
        super.setUp()
        sdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        sdkCreationChecker.removeNewKotlinSdk()
        super.tearDown()
    }

    fun testMobile() {
        // TODO: add import & tests here when we will be able to locate Android SDK automatically (see KT-27635)
        val builder = KotlinGradleMobileMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            checkSource("app/src") {
                sourceSetsSize(6)
                common("$commonMain/$kotlin/$sample/Sample.kt")
                test("$commonTest/$kotlin/$sample/SampleTests.kt")
                main("main/java/$sample/SampleAndroid.kt") {
                    contains("class MainActivity")
                }
                test("test/java/$sample/SampleTestsAndroid.kt")
                main("$iosMain/$kotlin/$sample/SampleIos.kt")
                test("$iosTest/$kotlin/$sample/SampleTestsIOS.kt")
            }
            checkSource("iosApp") {
                sourceSetsSize(3)
            }
            checkGradleConfiguration(mppPluginInside = false)
        }
    }

    fun testMobileShared() {
        val builder = KotlinGradleMobileSharedMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            checkSource("src") {
                sourceSetsSize(6)
                common("$commonMain/$kotlin/$sample/Sample.kt")
                test("$commonTest/$kotlin/$sample/SampleTests.kt")
                main("$iosMain/$kotlin/$sample/SampleIos.kt")
                test("$iosTest/$kotlin/$sample/SampleTestsNative.kt")
                main("$jvmMain/$kotlin/$sample/SampleJvm.kt")
                test("$jvmTest/$kotlin/$sample/SampleTestsJVM.kt")
            }
            checkSource {
                isExist("build.gradle") {
                    contains(iosMain)
                    contains(iosTest)
                }
            }
            checkGradleConfiguration(metadataInside = true)
            runGradleImport()
            runGradleTests("SampleTests", "SampleTestsJVM")

            if (HostManager.hostIsMac) {
                runGradleTask(builder.nativeTestName)
            }
        }
    }

    fun testNative() {
        val builder = KotlinGradleNativeMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            checkSource("src") {
                sourceSetsSize(2)
                isExist("$nativeMain/$kotlin/$sample/Sample${native.capitalize()}.kt")
                test("$nativeTest/$kotlin/$sample/SampleTests.kt")
            }
            checkGradleConfiguration()
            runGradleImport()
            runGradleTask("runReleaseExecutable${native.capitalize()}")
        }
    }

    fun testShared() {
        val builder = KotlinGradleSharedMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            checkSource("src") {
                sourceSetsSize(8)
                common("$commonMain/$kotlin/$sample/Sample.kt")
                test("$commonTest/$kotlin/$sample/SampleTests.kt")
                main("$jvmMain/$kotlin/$sample/SampleJvm.kt")
                test("$jvmTest/$kotlin/$sample/SampleTestsJVM.kt")
                main("$jsMain/$kotlin/$sample/SampleJs.kt")
                test("$jsTest/$kotlin/$sample/SampleTestsJS.kt")
                main("$nativeMain/$kotlin/$sample/Sample${native.capitalize()}.kt")
                test("$nativeTest/$kotlin/$sample/SampleTestsNative.kt")
            }
            checkSource{
                isExist("build.gradle") {
                    contains(nativeMain)
                    contains(nativeTest)
                    contains(jsMain)
                    contains(jsTest)
                }
            }
            checkGradleConfiguration(metadataInside = true)
            runGradleImport()
            runGradleTests("SampleTests", "SampleTestsJVM")
        }
    }

    fun testSharedWithQualifiedName() {
        val builder = KotlinGradleSharedMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            runGradleImport(useQualifiedModuleNames = true)
            runGradleTests("SampleTests", "SampleTestsJVM")
        }
    }

    fun testWeb() {
        val builder = KotlinGradleWebMultiplatformModuleBuilder()
        val project = builder.buildProject()

        with(project) {
            checkSource("src") {
                sourceSetsSize(6)
                common("$commonMain/$kotlin/$sample/Sample.kt")
                test("$commonTest/$kotlin/$sample/SampleTests.kt")
                main("$jvmMain/$kotlin/$sample/SampleJvm.kt")
                test("$jvmTest/$kotlin/$sample/SampleTestsJVM.kt")
                main("$jsMain/$kotlin/$sample/SampleJs.kt")
                test("$jsTest/$kotlin/$sample/SampleTestsJS.kt")
            }
            checkSource {
                isExist("build.gradle") {
                    contains(jsMain)
                    contains(jsTest)
                }
            }
            checkGradleConfiguration()
            /* TODO: return after fix KT-35095
              runGradleImport()
              runGradleTests("SampleTests", "SampleTestsJVM")*/
        }
    }
}