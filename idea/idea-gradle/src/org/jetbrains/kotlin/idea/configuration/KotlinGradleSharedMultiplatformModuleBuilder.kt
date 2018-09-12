/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

class KotlinGradleSharedMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val commonName: String = "common"
    private var jvmTargetName: String = "jvm"
    private var jsTargetName: String = "js"
    private var nativeTargetName: String = "ios"

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val jsSourceName get() = "$jsTargetName$productionSuffix"
    private val jsTestName get() = "$jsTargetName$testSuffix"
    private val nativeSourceName get() = "$nativeTargetName$productionSuffix"
    private val nativeTestName get() = "$nativeTargetName$testSuffix"

    override fun getBuilderId() = "kotlin.gradle.multiplatform.shared"

    override fun getPresentableName() = "Kotlin (Multiplatform Library)"

    override fun getDescription() =
        "Multiplatform Gradle projects allow sharing the same Kotlin code between all three main platforms (JVM, JS, iOS)."

    override fun buildMultiPlatformPart(): String {
        return """
            kotlin {
                targets {
                    fromPreset(presets.jvm, '$jvmTargetName')
                    fromPreset(presets.js, '$jsTargetName')
                    // For ARM, preset should be changed to presets.iosArm32 or presets.iosArm64
                    // For Linux, preset should be changed to e.g. presets.linuxX64
                    // For MacOS, preset should be changed to e.g. presets.macosX64
                    fromPreset(presets.iosX64, '$nativeTargetName')
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
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-native'
                        }
                    }
                    $nativeTestName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-test-native'
                        }
                    }
                }
            }
        """.trimIndent()
    }
}