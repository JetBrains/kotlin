/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.versions

// WARNING! Content of this class is filled during the build (see 'writePluginVersion' in ide/android-studio-native/build.gradle.kts)
// DON'T MODIFY MANUALLY!
object MobileMultiplatformPluginVersionsInfo {
    const val compiledAgainstKotlin: String = "1.4.255-SNAPSHOT"
    const val pluginVersion: String = "0.1-SNAPSHOT"

    private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
    fun isDevelopment() =
        compiledAgainstKotlin.endsWith(SNAPSHOT_SUFFIX)
                || pluginVersion.endsWith(SNAPSHOT_SUFFIX)
}