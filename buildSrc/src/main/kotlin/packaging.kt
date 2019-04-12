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
import java.net.URL

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
        val templatesDir = project(predecessorProjectName).file("templates")
        inputs.dir(templatesDir)
        from(templatesDir)
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

fun Project.zipCidrPlugin(cidrPluginTask: Task, cidrPluginZipPath: File) = tasks.creating(Zip::class) {
    destinationDirectory.value = cidrPluginZipPath.parentFile
    archiveFileName.value = cidrPluginZipPath.name

    from(cidrPluginTask)
    into("Kotlin")

    doLast {
        logger.lifecycle("Plugin artifacts packed to $cidrPluginZipPath")
    }
}

fun Project.cidrUpdatePluginsXml(
        pluginXmlTask: Task,
        cidrProductFriendlyVersion: String,
        cidrPluginZipPath: File,
        cidrCustomPluginRepoUrl: URL
) = tasks.creating {
    dependsOn(pluginXmlTask)

    val updatePluginsXmlFile = cidrPluginZipPath.parentFile.resolve("updatePlugins-$cidrProductFriendlyVersion.xml")
    outputs.file(updatePluginsXmlFile)

    val cidrPluginZipDeploymentUrl = URL(cidrCustomPluginRepoUrl, cidrPluginZipPath.name)
    inputs.property("${project.name}-$name-cidrPluginZipDeploymentUrl", cidrPluginZipDeploymentUrl)

    doLast {
        val extractedData = pluginXmlTask.outputs
                .files
                .asFileTree
                .singleFile
                .extractXmlElements(setOf("id", "version", "description", "idea-version"))

        val id by extractedData
        val version by extractedData
        val description by extractedData

        val ideaVersion = extractedData.getValue("idea-version").second
        val sinceBuild = ideaVersion.getValue("since-build")
        val untilBuild = ideaVersion.getValue("until-build")

        updatePluginsXmlFile.writeText("""
            <plugins>
                <plugin id="${id.first}" url="$cidrPluginZipDeploymentUrl" version="${version.first}">
                    <idea-version since-build="$sinceBuild" until-build="$untilBuild"/>
                    <description>${description.first}</description>
                </plugin>
            </plugins>
           """.trimIndent()
        )

        logger.lifecycle("Custom plugin repository XML descriptor written to $updatePluginsXmlFile")
    }
}
