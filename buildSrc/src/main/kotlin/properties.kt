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

// Note: "appcodePluginNumber" property can be used to override the default plugin number (SNAPSHOT)
val Project.appcodePluginNumber: String
    get() = findProperty("appcodePluginNumber")?.toString() ?: SNAPSHOT

val Project.appcodePluginVersionFull: String
    get() {
        val appcodeVersion: String by rootProject.extra
        return cidrPluginVersionFull("AppCode", appcodeVersion, appcodePluginNumber)
    }

// Note: "appcodePluginZipPath" property can be used to override the standard location of packed plugin artifacts
val Project.appcodePluginZipPath: File
    get() = propertyAsPath("appcodePluginZipPath")
        ?: defaultCidrPluginZipPath(appcodePluginVersionFull)

// Note: "clionPluginNumber" property can be used to override the default plugin number (SNAPSHOT)
val Project.clionPluginNumber: String
    get() = findProperty("clionPluginNumber")?.toString() ?: SNAPSHOT

val Project.clionPluginVersionFull: String
    get() {
        val clionVersion: String by rootProject.extra
        return cidrPluginVersionFull("CLion", clionVersion, clionPluginNumber)
    }

// Note: "clionPluginZipPath" property can be used to override the standard location of packed plugin artifacts
val Project.clionPluginZipPath: File
    get() = propertyAsPath("clionPluginZipPath")
        ?: defaultCidrPluginZipPath(clionPluginVersionFull)

private fun Project.cidrPluginVersionFull(productName: String, productVersion: String, cidrPluginNumber: String): String {
    val cidrPluginVersion = if (isStandaloneBuild) {
        val ideaPluginForCidrBuildNumber: String by rootProject.extra
        ideaPluginForCidrBuildNumber
    } else {
        // take it from Big Kotlin
        val buildNumber: String by rootProject.extra
        buildNumber
    }

    return "$cidrPluginVersion-$productName-$productVersion-$cidrPluginNumber"
}

private fun Project.propertyAsPath(propertyName: String): File? =
    findProperty(propertyName)?.let { File(it.toString()).canonicalFile }

private fun Project.defaultCidrPluginZipPath(cidrProductVersionFull: String): File {
    val artifactsForCidrDir: File by rootProject.extra
    return artifactsForCidrDir.resolve("kotlin-plugin-$cidrProductVersionFull.zip").canonicalFile
}

private const val SNAPSHOT = "SNAPSHOT"
