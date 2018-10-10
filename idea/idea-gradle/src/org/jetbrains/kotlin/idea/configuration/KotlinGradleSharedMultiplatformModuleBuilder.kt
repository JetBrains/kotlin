/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.konan.target.presetName
import java.io.BufferedWriter

class KotlinGradleSharedMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val commonName: String = "common"
    private var jvmTargetName: String = "jvm"
    private var jsTargetName: String = "js"
    private var nativeTargetName: String = defaultNativeTarget.userTargetName

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val jsSourceName get() = "$jsTargetName$productionSuffix"
    private val jsTestName get() = "$jsTargetName$testSuffix"
    private val nativeSourceName get() = "$nativeTargetName$productionSuffix"
    private val nativeTestName get() = "$nativeTargetName$testSuffix"

    override val shouldEnableGradleMetadataPreview: Boolean = true

    override fun getBuilderId() = "kotlin.gradle.multiplatform.shared"

    override fun getPresentableName() = "Kotlin (Multiplatform Library)"

    override fun getDescription() =
        "Multiplatform Gradle projects allow sharing the same Kotlin code between all three main platforms (JVM, JS, Native)."

    override fun createProjectSkeleton(module: Module, rootDir: VirtualFile) {
        val src = rootDir.createChildDirectory(this, "src")

        val commonMain = src.createKotlinSampleFileWriter(commonSourceName)
        val commonTest = src.createKotlinSampleFileWriter(commonTestName, fileName = "SampleTests.kt")
        val jvmMain = src.createKotlinSampleFileWriter(jvmSourceName, jvmTargetName)
        val jvmTest = src.createKotlinSampleFileWriter(jvmTestName, fileName = "SampleTestsJVM.kt")
        val jsMain = src.createKotlinSampleFileWriter(jsSourceName, jsTargetName)
        val jsTest = src.createKotlinSampleFileWriter(jsTestName, fileName = "SampleTestsJS.kt")
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
                    val name: String
                }

                fun hello(): String = "Hello from ${"$"}{Platform.name}"
            """.trimIndent()
            )

            jvmMain.write(
                """
                package sample

                actual class Sample {
                    actual fun checkMe() = 42
                }

                actual object Platform {
                    actual val name: String = "JVM"
                }
            """.trimIndent()
            )

            jsMain.write(
                """
                package sample

                actual class Sample {
                    actual fun checkMe() = 12
                }

                actual object Platform {
                    actual val name: String = "JS"
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
                    actual val name: String = "Native"
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

            jsTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsJS {
                    @Test
                    fun testHello() {
                        assertTrue("JS" in hello())
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
                        assertTrue("Native" in hello())
                    }
                }
            """.trimIndent()
            )
        } finally {
            listOf(commonMain, commonTest, jvmMain, jvmTest, jsMain, jsTest, nativeMain, nativeTest).forEach(BufferedWriter::close)
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
                    fromPreset(presets.js, '$jsTargetName')
                    // For ARM, preset should be changed to presets.iosArm32 or presets.iosArm64
                    // For Linux, preset should be changed to e.g. presets.linuxX64
                    // For MacOS, preset should be changed to e.g. presets.macosX64
                    fromPreset(presets.${defaultNativeTarget.presetName}, '${defaultNativeTarget.userTargetName}')
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
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-test'
                            implementation 'org.jetbrains.kotlin:kotlin-test-junit'
                        }
                    }
                    $jsSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                        }
                    }
                    $jsTestName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-test-js'
                        }
                    }
                    $nativeSourceName {
                    }
                    $nativeTestName {
                    }
                }
            }
        """.trimIndent()
    }
}