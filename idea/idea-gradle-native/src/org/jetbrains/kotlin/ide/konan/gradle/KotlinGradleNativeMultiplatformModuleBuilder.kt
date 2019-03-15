/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.KotlinGradleAbstractMultiplatformModuleBuilder
import org.jetbrains.kotlin.konan.target.presetName
import javax.swing.Icon

class KotlinGradleNativeMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val nativePresetName = defaultNativeTarget.presetName
    private val nativeTargetName = defaultNativeTarget.userTargetName

    private val nativeSourceName get() = "$nativeTargetName$productionSuffix"
    val nativeTestName get() = "$nativeTargetName$testSuffix"

    private val nativeReleaseExecutableRunTaskame get() = ":runReleaseExecutable${nativeTargetName.capitalize()}"
    private val nativeDebugExecutableRunTaskame get() = ":runDebugExecutable${nativeTargetName.capitalize()}"

    override fun getNodeIcon(): Icon = KotlinIcons.NATIVE

    override fun getBuilderId() = "kotlin.gradle.multiplatform.native"

    override fun getPresentableName() = "Native | Gradle"

    override fun getDescription() = "Gradle-based Kotlin project for native binaries"

    override val notImportedCommonSourceSets = true

    override fun createProjectSkeleton(rootDir: VirtualFile) {
        val src = rootDir.createChildDirectory(this, "src")

        // Main module:
        src.createKotlinSampleFileWriter(nativeSourceName, nativeTargetName).use {
            it.write(
                """
                package sample

                fun hello(): String = "Hello, Kotlin/Native!"

                fun main() {
                    println(hello())
                }
                """.trimIndent()
            )
        }

        // Test module:
        src.createKotlinSampleFileWriter(nativeTestName, fileName = "SampleTests.kt").use {
            it.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTests {
                    @Test
                    fun testHello() {
                        assertTrue("Kotlin/Native" in hello())
                    }
                }
                """.trimIndent()
            )
        }
    }

    override fun buildMultiPlatformPart(): String {
        return """
            kotlin {
                // For ARM, should be changed to iosArm32 or iosArm64
                // For Linux, should be changed to e.g. linuxX64
                // For MacOS, should be changed to e.g. macosX64
                // For Windows, should be changed to e.g. mingwX64
                $nativePresetName("$nativeTargetName") {
                    binaries {
                        executable {
                            // Change to specify fully qualified name of your application's entry point:
                           entryPoint = 'sample.main'
                            // Specify command-line arguments, if necessary:
                            runTask?.args('')
                        }
                    }
                }
                sourceSets {
                    // Note: To enable common source sets please comment out 'kotlin.import.noCommonSourceSets' property
                    // in gradle.properties file and re-import your project in IDE.
                    $nativeSourceName {
                    }
                    $nativeTestName {
                    }
                }
            }

            // Use the following Gradle tasks to run your application:
            // $nativeReleaseExecutableRunTaskame - without debug symbols
            // $nativeDebugExecutableRunTaskame - with debug symbols
        """.trimIndent()
    }
}
