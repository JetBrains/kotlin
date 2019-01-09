/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression.STORED
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.task

fun Project.addPlatformDepsArtifacts(zipConfiguration: Configuration, mainJarConfiguration: String, otherJarsConfiguration: String) {
    val platformDepsJarName = "kotlinNative-platformDeps.jar"

    val downloadTask = task<Copy>("download" + zipConfiguration.name.capitalize()) {
        dependsOn(zipConfiguration)
        lazyFrom { zipTree(zipConfiguration.singleFile).files }
        into("$buildDir/external-deps/${zipConfiguration.name}")
    }

    // Done this way because:
    // 1. passing `provider { fileTree(downloadTask.destinationDir).matching { include(platformDepsJarName) }.singleFile }` as
    //    an artifactRef to `artifacts.add()` call does not work in stable way
    // 2. Zip is a known instace of AbstractArchiveTask that is properly recognized by `artifacts.add()` call

    // Extract platformDeps JAR file and rename it to avoid ambiguity between CLion and AppCode.
    val jarTask = task<Jar>(mainJarConfiguration) {
        dependsOn(downloadTask)
        entryCompression = STORED
        archiveName = platformDepsJarName.replace(".", "-${mainJarConfiguration.substringBefore("PlatformDeps")}.")
        destinationDir = file("$buildDir/external-deps/$mainJarConfiguration")
        lazyFrom { zipTree(fileTree(downloadTask.destinationDir).matching { include(platformDepsJarName) }.singleFile) }
    }
    addArtifact(mainJarConfiguration, jarTask, jarTask)

    // Keep the rest of JAR files from the downloaded ZIP file as a separate artifact.
    val zipTask = task<Zip>(otherJarsConfiguration) {
        dependsOn(downloadTask)
        entryCompression = STORED
        baseName = otherJarsConfiguration
        destinationDir = file("$buildDir/external-deps")
        lazyFrom { fileTree(downloadTask.destinationDir).matching { exclude(platformDepsJarName) }.files }
    }
    addArtifact(otherJarsConfiguration, zipTask, zipTask)
}

fun Project.addArtifact(fromConfiguration: Configuration, toConfiguration: String) =
    addArtifact(toConfiguration, provider { fromConfiguration.singleFile })

private fun Project.addArtifact(configurationName: String, artifactRef: Any, task: Zip? = null) {
    configurations.maybeCreate(configurationName)
    artifacts.add(configurationName, artifactRef) {
        if (task != null) builtBy(task)
    }
}
