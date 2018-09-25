/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import org.jetbrains.kotlin.idea.configuration.KotlinGradleAbstractMultiplatformModuleBuilder

class KotlinGradleNativeMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private var nativeTargetName: String = "ios"

    override fun getBuilderId() = "kotlin.gradle.multiplatform.native"

    override fun getPresentableName() = "Kotlin/Native"

    override fun getDescription() = "Kotlin module for iOS target"

    override val notImportedCommonSourceSets = true

    override fun buildMultiPlatformPart(): String {
        return """
            kotlin {
                targets {
                    // For ARM, preset should be changed to presets.iosArm32 or presets.iosArm64
                    // For Linux, preset should be changed to e.g. presets.linuxX64
                    // For MacOS, preset should be changed to e.g. presets.macosX64
                    fromPreset(presets.iosX64, '$nativeTargetName')
                }
            }
        """.trimIndent()
    }
}
