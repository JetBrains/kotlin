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
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jsSourceName get() = "$jsTargetName$productionSuffix"

    override fun getBuilderId() = "kotlin.gradle.multiplatform.web"

    override fun getPresentableName() = "Kotlin (Multiplatform - Web)"

    override fun getDescription() =
        "Multiplatform web projects allow reusing the same code between JVM & JS platforms supported by Kotlin. Such projects are built with Gradle."

    override fun buildMultiPlatformPart(): String {
        return """
            kotlin {
                targets {
                    fromPreset(presets.jvmWithJava, '$jvmTargetName')
                    fromPreset(presets.js, '$jsTargetName')
                }
                sourceSets {
                    $commonSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                        }
                    }
                    $jvmSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
                        }
                    }
                    $jsSourceName {
                        dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                        }
                    }
                }
            }
        """.trimIndent()
    }
}