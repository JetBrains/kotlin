/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get
import java.io.File

internal val excludesListFromIdeaPlugin = listOf(
    "lib/android-*.jar", // no need Android stuff
    "lib/kapt3-*.jar", // no annotation processing
    "lib/jps/**", // JSP plugin
    "kotlinc/**"
)

fun Project.cidrPlugin(product: String, cidrPluginDir: String) = tasks.creating(Copy::class) {

    val cidrPluginTaskName = product.toLowerCase() + "Plugin"
    val childCurrentPluginTasks = getTasksByName(cidrPluginTaskName, true) - this

    dependsOn(childCurrentPluginTasks)
    dependsOn(ideaPluginPackagingTask)

    into(cidrPluginDir)
    from(ideaPluginDir) {
        exclude("lib/kotlin-plugin.jar")
        exclude(excludesListFromIdeaPlugin)
    }
}

fun Project.zipCidrPlugin(product: String, productVersion: String) = tasks.creating(Zip::class) {

    val cidrPluginTaskName = product.toLowerCase() + "Plugin"

    // Note: "appcodePluginVersion" and "clionPluginVersion" have different format and semantics from
    // "pluginVersion" used in IJ and AS plugins.
    val cidrPluginVersionPropertyName = product.toLowerCase() + "PluginVersion"
    val cidrPluginVersion = findProperty(cidrPluginVersionPropertyName)?.toString() ?: "beta-1"

    // Note: "appcodePluginZipPath" and "clionPluginZipPath" properties can be used to override
    // the standard location of packed plugin artifacts
    val cidrPluginZipPathPropertyName = product.toLowerCase() + "PluginZipPath"
    val destPath = findProperty(cidrPluginZipPathPropertyName)?.toString()
        ?: "$artifactsDir/kotlin-plugin-$kotlinVersion-$product-$cidrPluginVersion-$productVersion.zip"
    val destFile = File(destPath)

    destinationDir = destFile.parentFile
    archiveName = destFile.name

    from(tasks[cidrPluginTaskName])
    into("Kotlin")

    doLast {
        logger.lifecycle("Plugin artifacts packed to $archivePath")
    }
}
