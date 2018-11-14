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

    override fun getNodeIcon(): Icon = KotlinIcons.NATIVE

    override fun getBuilderId() = "kotlin.gradle.multiplatform.native"

    override fun getPresentableName() = "Kotlin/Native"

    override fun getDescription() = "Kotlin module for native binaries"

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
                targets {
                    // For ARM, preset should be changed to presets.iosArm32 or presets.iosArm64
                    // For Linux, preset should be changed to e.g. presets.linuxX64
                    // For MacOS, preset should be changed to e.g. presets.macosX64
                    fromPreset(presets.$nativePresetName, '$nativeTargetName')

                    configure([$nativeTargetName]) {
                        // Comment to generate Kotlin/Native library (KLIB) instead of executable file:
                        compilations.main.outputKinds 'EXECUTABLE'
                        // Change to specify fully qualified name of your application's entry point:
                        compilations.main.entryPoint 'sample.main'
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

            task runProgram {
                def buildType = 'RELEASE' // Change to 'DEBUG' to run application with debug symbols.
                dependsOn kotlin.targets.$nativeTargetName.compilations.main.linkTaskName('EXECUTABLE', buildType)
                doLast {
                    def programFile = kotlin.targets.$nativeTargetName.compilations.main.getBinary('EXECUTABLE', buildType)
                    exec {
                        executable programFile
                        args ''
                    }
                }
            }
        """.trimIndent()
    }
}
