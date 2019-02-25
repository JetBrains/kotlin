/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import java.io.File

internal val EXCLUDES_LIST_FROM_IDEA_PLUGIN = listOf(
    "lib/android-*.jar", // no need Android stuff
    "lib/kapt3-*.jar", // no annotation processing
    "lib/jps/**", // JSP plugin
    "kotlinc/**"
)

fun Project.packageCidrPlugin(
        predecessorProjectName: String,
        cidrPluginDir: File,
        pluginJarTask: Task,
        platformDepsJarTask: Task,
        platformDepsDir: File
) = tasks.creating(Copy::class) {
    into(cidrPluginDir)

    into("lib") {
        from(pluginJarTask)
        from(platformDepsJarTask)

        val otherPlatformDepsJars = fileTree(platformDepsDir) {
            include("*.jar")
            exclude(PLATFORM_DEPS_JAR_NAME)
        }.files
        from(otherPlatformDepsJars)
    }

    into("templates") {
        from(project(predecessorProjectName).file("templates"))
    }

    val ideaPluginDir = if (isStandaloneBuild) {
        // use dir where IDEA plugin has been already downloaded
        val ideaPluginForCidrDir: File by rootProject.extra
        ideaPluginForCidrDir
    } else {
        dependsOn(":ideaPlugin")
        // use IDEA plugin dir from Big Kotlin
        val ideaPluginDir: File by rootProject.extra
        ideaPluginDir
    }

    from(ideaPluginDir) {
        exclude("lib/kotlin-plugin.jar")
        exclude(EXCLUDES_LIST_FROM_IDEA_PLUGIN)
    }
}

fun Project.zipCidrPlugin(cidrPluginTaskName: String, cidrPluginZipPath: File) = tasks.creating(Zip::class) {
    val cidrPluginTask = getTasksByName(cidrPluginTaskName, true).single()

    destinationDirectory.value = cidrPluginZipPath.parentFile
    archiveFileName.value = cidrPluginZipPath.name

    from(cidrPluginTask)
    into("Kotlin")

    doLast {
        logger.lifecycle("Plugin artifacts packed to $cidrPluginZipPath")
    }
}
