/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

class KotlinGradleWebMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val commonName: String = "common"
    private var jvmTargetName: String = "jvm"
    private var jsTargetName: String = "js"

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val jsSourceName get() = "$jsTargetName$productionSuffix"
    private val jsTestName get() = "$jsTargetName$testSuffix"

    override fun getBuilderId() = "kotlin.gradle.multiplatform.web"

    override fun getPresentableName() = "Kotlin (JS Client/JVM Server)"

    override fun getDescription() =
        "Multiplatform Gradle projects allow reusing the same Kotlin code between JS Client and JVM Server."

    override fun buildMultiPlatformPart(): String {
        return """
            kotlin {
                targets {
                    fromPreset(presets.jvm, '$jvmTargetName')
                    fromPreset(presets.js, '$jsTargetName')
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
                }
            }
        """.trimIndent()
    }
}