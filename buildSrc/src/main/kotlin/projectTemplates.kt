/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.ultimate.ReleaseType.*
import java.io.File
import java.util.*

// All files that will be patched by templating engine:
private val PATCHED_FILES = setOf("build.gradle.kts", "settings.gradle.kts", "TemplateInfo.plist")

private const val CLION_TEMPLATE_INFO_FILE = "template.info"

fun Copy.includeProjectTemplates(sourceProject: Project) {
    val templatesDir = sourceProject.file("templates")
    inputs.dir(templatesDir)

    val templateParameters = project.getTemplateParameters()
    inputs.property("${project.name}-$name-includeProjectTemplates-templateParameters", templateParameters)

    into("templates") {
        from(templatesDir)
        eachFile {
            if (sourceName in PATCHED_FILES) {
                exclude()

                try {
                    val rendered = project.renderTemplate(file, templateParameters)

                    val destinationFile = destinationDir.resolve(path)
                    destinationFile.parentFile.mkdirs()
                    destinationFile.writeText(rendered)
                } catch (e: Exception) {
                    logger.error(
                            """
                            |Error rendering template "$path" with parameters:
                            |${templateParameters.entries.joinToString(separator = "") { (name, value) -> "$name=[$value]\n" }}
                            |Caused by : $e
                            """.trimMargin())

                    throw e
                }
            }
        }
    }

    doLast {
        if (didWork) {
            // make some checks
            val destTemplatesDir = destinationDir.resolve("templates")
            project.fileTree(destTemplatesDir)
                    .filter { it.name == CLION_TEMPLATE_INFO_FILE }
                    .forEach { templateInfoFile ->
                        // check that the template info file is in the proper location
                        val templateDir = templateInfoFile.parentFile
                        check(with(templateDir.parentFile) { name == "gradle" && parentFile == destTemplatesDir }) {
                            "$CLION_TEMPLATE_INFO_FILE file is in the wrong location: $templateInfoFile"
                        }

                        // check that the files marked with "openInEditor" are actually exist
                        val properties = Properties().apply { templateInfoFile.inputStream().use { load(it) } }
                        val filesToOpen = properties["openInEditor"]?.toString()?.split(',') ?: return@forEach

                        filesToOpen.forEach {
                            val fileToOpen = templateDir.resolve(it)
                            check(fileToOpen.isFile) { "File should be opened in IDE but such file even does not exist: $fileToOpen" }
                        }
                    }
        }
    }
}

private enum class ReleaseType {
    DEV, EAP, RELEASE, SNAPSHOT
}

private fun Project.getTemplateParameters(): Map<String, String> {
    /*
     * Possible values:
     * 1.3.40-dev-1654
     * 1.3.30-eap-125
     * 1.3.30-release-170
     * 1.3-SNAPSHOT
     */
    val kotlinBuildNumber = kotlinBuildNumberByIdeaPlugin

    val (numericVersion, releaseType) = kotlinBuildNumber.toLowerCase(Locale.US)
            .split('-', limit = 3)
            .takeIf { it.size in 2..3 }
            ?.let { (numericVersion, releaseTypeName) ->
                val releaseType = ReleaseType.values().firstOrNull {
                    it.name.equals(releaseTypeName, ignoreCase = true)
                } ?: return@let null
                numericVersion to releaseType
            } ?: error("Invalid or unsupported kotlin build number format: $kotlinBuildNumber")

    val gradlePluginVersion = when (releaseType) {
        RELEASE -> numericVersion // leave only numeric part, ex: "1.3.30"
        else -> kotlinBuildNumber // otherwise use the full build number
    }

    return mapOf(
            "MPP_HOST_PLATFORM" to mppPlatform,
            "MPP_GRADLE_PLUGIN_VERSION" to gradlePluginVersion,
            "MPP_CUSTOM_PLUGIN_REPOS_4S" to customPluginRepos(releaseType, kotlinBuildNumber, 4),
            "MPP_CUSTOM_PLUGIN_REPOS_8S" to customPluginRepos(releaseType, kotlinBuildNumber, 8),
            "MPP_PLUGIN_RESOLUTION_RULES" to pluginResolutionRules(releaseType)
    ).also {
        logger.kotlinInfo {
            """
            |Using template parameters for project ${project.path} (${it.size} items):
            |${it.entries.joinToString(separator = "") { (name, value) -> "$name=[$value]\n" }}
            |""".trimMargin()
        }
    }
}

private val Project.kotlinBuildNumberByIdeaPlugin
    get() = if (isStandaloneBuild) {
        val ideaPluginForCidrBuildNumber: String by rootProject.extra
        ideaPluginForCidrBuildNumber
    } else {
        // take it from Big Kotlin
        val buildNumber: String by rootProject.extra
        buildNumber
    }

// inspired by com.intellij.openapi.util.SystemInfoRt
private val mppPlatform: String
    get() = with(hostOsName) {
        when {
            startsWith("windows") -> "mingwX64"
            startsWith("mac") -> "macosX64"
            else -> "linuxX64"
        }
    }

private fun customPluginRepos(releaseType: ReleaseType, kotlinBuildNumber: String, indentSpaces: Int): String {
    val repos = when (releaseType) {
        RELEASE -> emptyList()
        EAP -> listOf(
                "https://dl.bintray.com/kotlin/kotlin-eap",
                "https://dl.bintray.com/kotlin/kotlin-dev"
        )
        DEV -> listOf(
                "https://dl.bintray.com/kotlin/kotlin-dev",
                "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:$kotlinBuildNumber,branch:default:any/artifacts/content/maven/"
        )
        SNAPSHOT -> listOf(
                "https://oss.sonatype.org/content/repositories/snapshots"
        )
    }

    return repos.joinToString(separator = "") { "\n${" ".repeat(indentSpaces)}maven(\"$it\")" }
}

private fun pluginResolutionRules(releaseType: ReleaseType): String = when (releaseType) {
    SNAPSHOT -> """
        |
        |    resolutionStrategy {
        |        eachPlugin {
        |            if (requested.id.name == "multiplatform") {
        |                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
        |            }
        |        }
        |    }
        |
        """.trimMargin()
    else -> ""
}

// We shall not use any external templating engine in buildSrc because it will leak to Gradle build classpath.
// Templating engines provided by Groovy runtime can be used with limitations: If there is a symbol starting
// with '$' that is not one of template parameters, then engine will fail.
// So we have to use simple & reliable self-made templating.
private fun Project.renderTemplate(template: File, templateParameters: Map<String, String>): String {
    val templateText = template.readText()
    var result = templateText

    val usedParameters = mutableListOf<String>()
    templateParameters.entries.forEach { (name, value) ->
        val temp = result.replace("@@$name@@", value)
        if (result != temp) usedParameters += name
        result = temp
    }

    if (usedParameters.isEmpty())
        logger.kotlinInfo { "Template \"$template\" was not rendered. It does not include any parameters." }
    else
        logger.kotlinInfo { "Template \"$template\" was rendered with the following parameters: $usedParameters" }

    if (result.isBlank() && templateText.isNotBlank())
        error("Template rendering resulted is blank string, however template file is not blank: $template")

    return result
}
