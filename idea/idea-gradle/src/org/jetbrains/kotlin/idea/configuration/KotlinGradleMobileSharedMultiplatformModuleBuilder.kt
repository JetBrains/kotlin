/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedWriter

class KotlinGradleMobileSharedMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val commonName: String = "common"
    private var jvmTargetName: String = "jvm"
    private var nativeTargetName: String = "ios"

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val nativeSourceName get() = "$nativeTargetName$productionSuffix"
    private val nativeTestName get() = "$nativeTargetName$testSuffix"

    override val shouldEnableGradleMetadataPreview: Boolean = true

    override fun getBuilderId() = "kotlin.gradle.multiplatform.mobileshared"

    override fun getPresentableName() = "Kotlin (Mobile Shared Library)"

    override fun getDescription() =
        "Multiplatform Gradle projects allow sharing the same Kotlin code between two mobile platforms (JVM/Android, Native)."

    override fun createProjectSkeleton(rootDir: VirtualFile) {
        val src = rootDir.createChildDirectory(this, "src")

        val commonMain = src.createKotlinSampleFileWriter(commonSourceName)
        val commonTest = src.createKotlinSampleFileWriter(commonTestName, fileName = "SampleTests.kt")
        val jvmMain = src.createKotlinSampleFileWriter(jvmSourceName, jvmTargetName)
        val jvmTest = src.createKotlinSampleFileWriter(jvmTestName, fileName = "SampleTestsJVM.kt")
        val nativeMain = src.createKotlinSampleFileWriter(nativeSourceName, nativeTargetName)
        val nativeTest = src.createKotlinSampleFileWriter(nativeTestName, fileName = "SampleTestsNative.kt")

        try {
            commonMain.write(
                """
                package sample

                expect class Sample() {
                    fun checkMe(): Int
                }

                expect object Platform {
                    fun name(): String
                }

                fun hello(): String = "Hello from ${"$"}{Platform.name()}"
            """.trimIndent()
            )

            jvmMain.write(
                """
                package sample

                actual class Sample {
                    actual fun checkMe() = 42
                }

                actual object Platform {
                    actual fun name(): String = "JVM"
                }
            """.trimIndent()
            )

            nativeMain.write(
                """
                package sample

                actual class Sample {
                    actual fun checkMe() = 7
                }

                actual object Platform {
                    actual fun name(): String = "iOS"
                }
            """.trimIndent()
            )

            commonTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTests {
                    @Test
                    fun testMe() {
                        assertTrue(Sample().checkMe() > 0)
                    }
                }
            """.trimIndent()
            )

            jvmTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsJVM {
                    @Test
                    fun testHello() {
                        assertTrue("JVM" in hello())
                    }
                }
            """.trimIndent()
            )

            nativeTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsNative {
                    @Test
                    fun testHello() {
                        assertTrue("iOS" in hello())
                    }
                }
            """.trimIndent()
            )
        } finally {
            listOf(commonMain, commonTest, jvmMain, jvmTest, nativeMain, nativeTest).forEach(BufferedWriter::close)
        }
    }

    override fun buildMultiPlatformPart(): String {
        return """
            group 'com.example'
            version '0.0.1'

            apply plugin: 'maven-publish'

            kotlin {
                targets {
                    fromPreset(presets.jvm, '$jvmTargetName')
                    // This preset is for iPhone emulator
                    // Switch here to presets.iosArm64 to build library for iPhone device
                    fromPreset(presets.iosX64, '$nativeTargetName') {
                        compilations.main.outputKinds 'FRAMEWORK'
                    }
                }
                sourceSets {
                    $commonSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                        }
                    }
                    $commonTestName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-test-common'
                            implementation 'org.jetbrains.kotlin:kotlin-test-annotations-common'
                        }
                    }
                    $jvmSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-test'
                            implementation 'org.jetbrains.kotlin:kotlin-test-junit'
                        }
                    }
                    $nativeSourceName {
                    }
                    $nativeTestName {
                    }
                }
            }

            configurations {
                compileClasspath
            }
        """.trimIndent()
    }
}