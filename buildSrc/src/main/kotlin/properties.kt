/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.io.File

// Note: "appcodePluginVersion" has different format and semantics from "pluginVersion" used in IJ and AS plugins.
val Project.appcodePluginVersion: String
    get() = findProperty("appcodePluginVersion")?.toString() ?: "beta-1"

val Project.appcodePluginVersionFull: String
    get() {
        val appcodeVersion: String by rootProject.extra
        return cidrPluginVersionFull("AppCode", appcodeVersion, appcodePluginVersion)
    }

// Note: "appcodePluginZipPath" property can be used to override the standard location of packed plugin artifacts
val Project.appcodePluginZipPath: File
    get() = propertyAsPath("appcodePluginZipPath")
        ?: defaultCidrPluginZipPath(appcodePluginVersionFull)

// Note: "clionPluginVersion" has different format and semantics from "pluginVersion" used in IJ and AS plugins.
val Project.clionPluginVersion: String
    get() = findProperty("clionPluginVersion")?.toString() ?: "beta-1"

val Project.clionPluginVersionFull: String
    get() {
        val clionVersion: String by rootProject.extra
        return cidrPluginVersionFull("CLion", clionVersion, clionPluginVersion)
    }

// Note: "clionPluginZipPath" property can be used to override the standard location of packed plugin artifacts
val Project.clionPluginZipPath: File
    get() = propertyAsPath("clionPluginZipPath")
        ?: defaultCidrPluginZipPath(clionPluginVersionFull)

private fun Project.cidrPluginVersionFull(productName: String, productVersion: String, cidrPluginVersion: String): String {
    val kotlinVersion = if (isStandaloneBuild) {
        val kotlinForCidrVersion: String by rootProject.extra
        kotlinForCidrVersion
    } else {
        // take it from Big Kotlin
        val kotlinVersion: String by rootProject.extra
        kotlinVersion
    }

    return "$kotlinVersion-$productName-$cidrPluginVersion-$productVersion"
}

private fun Project.propertyAsPath(propertyName: String): File? =
    findProperty(propertyName)?.let { File(it.toString()).canonicalFile }

private fun Project.defaultCidrPluginZipPath(cidrProductVersionFull: String): File {
    val artifactsForCidrDir: File by rootProject.extra
    return artifactsForCidrDir.resolve("kotlin-plugin-$cidrProductVersionFull.zip").canonicalFile
}
