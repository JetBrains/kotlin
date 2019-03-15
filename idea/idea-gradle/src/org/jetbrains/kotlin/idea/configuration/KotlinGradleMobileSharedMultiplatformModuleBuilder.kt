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
    val nativeTestName get() = "$nativeTargetName$testSuffix"

    override val shouldEnableGradleMetadataPreview: Boolean = true

    override fun getBuilderId() = "kotlin.gradle.multiplatform.mobileshared"

    override fun getPresentableName() = "Mobile Shared Library | Gradle"

    override fun getDescription() =
        "Multiplatform Gradle project allowing reuse of the same Kotlin code between two mobile platforms (JVM/Android and Native)"

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
                jvm()
                // This is for iPhone emulator
                // Switch here to iosArm64 (or iosArm32) to build library for iPhone device
                iosX64("$nativeTargetName") {
                    binaries {
                        framework()
                    }
                }
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
                            implementation kotlin('stdlib')
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation kotlin('test')
                            implementation kotlin('test-junit')
                        }
                    }
                    $nativeSourceName {
                    }
                    $nativeTestName {
                    }
                }
            }

            task $nativeTestName {
                def device = project.findProperty("${nativeTargetName}Device")?.toString() ?: "iPhone 8"
                dependsOn kotlin.targets.$nativeTargetName.binaries.getExecutable('test', 'DEBUG').linkTaskName
                group = JavaBasePlugin.VERIFICATION_GROUP
                description = "Runs tests for target '$nativeTargetName' on an iOS simulator"

                doLast {
                    def binary = kotlin.targets.$nativeTargetName.binaries.getExecutable('test', 'DEBUG').outputFile
                    exec {
                        commandLine 'xcrun', 'simctl', 'spawn', device, binary.absolutePath
                    }
                }
            }

            configurations {
                compileClasspath
            }
        """.trimIndent()
    }
}