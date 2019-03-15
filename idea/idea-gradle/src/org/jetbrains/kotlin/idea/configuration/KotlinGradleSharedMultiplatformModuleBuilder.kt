/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

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
    val nativeTestName get() = "$nativeTargetName$testSuffix"

    override val shouldEnableGradleMetadataPreview: Boolean = true

    override fun getBuilderId() = "kotlin.gradle.multiplatform.shared"

    override fun getPresentableName() = "Multiplatform Library | Gradle"

    override fun getDescription() =
        "Multiplatform Gradle project allowing reuse of the same Kotlin code between all three main platforms (JVM, JS, and Native)"

    override fun createProjectSkeleton(rootDir: VirtualFile) {
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
                jvm()
                js()
                // For ARM, should be changed to iosArm32 or iosArm64
                // For Linux, should be changed to e.g. linuxX64
                // For MacOS, should be changed to e.g. macosX64
                // For Windows, should be changed to e.g. mingwX64
                ${defaultNativeTarget.presetName}("${defaultNativeTarget.userTargetName}")
                sourceSets {
                    $commonSourceName {
                        dependencies {
                            implementation kotlin('stdlib-common')
                        }
                    }
                    $commonTestName {
                        dependencies {
                            implementation kotlin('test-common')
                            implementation kotlin('test-annotations-common')
                        }
                    }
                    $jvmSourceName {
                        dependencies {
                            implementation kotlin('stdlib-jdk8')
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation kotlin('test')
                            implementation kotlin('test-junit')
                        }
                    }
                    $jsSourceName {
                        dependencies {
                            implementation kotlin('stdlib-js')
                        }
                    }
                    $jsTestName {
                        dependencies {
                            implementation kotlin('test-js')
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