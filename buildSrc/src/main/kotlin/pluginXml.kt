/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.the
import java.io.File

internal const val PLUGIN_XML_PATH = "META-INF/plugin.xml"

// Extract plugin.xml from the original Kotlin plugin, patch this file to exclude non-CIDR stuff and version information,
// and then save under new name KotlinPlugin.xml.
fun Project.prepareKotlinPluginXml(originalPluginJar: Configuration) = tasks.creating {
    val kotlinPluginXmlPath = "META-INF/KotlinPlugin.xml"

    inputs.files(originalPluginJar)
    outputs.dir("$buildDir/$name")

    doFirst {
        val placeholderRegex = Regex(
                """<!-- CIDR-PLUGIN-PLACEHOLDER-START -->(.*)<!-- CIDR-PLUGIN-PLACEHOLDER-END -->""",
                RegexOption.DOT_MATCHES_ALL
        )

        val excludeRegex = Regex(
                """<!-- CIDR-PLUGIN-EXCLUDE-START -->(.*?)<!-- CIDR-PLUGIN-EXCLUDE-END -->""",
                RegexOption.DOT_MATCHES_ALL
        )

        val ideaVersionRegex = Regex("""<idea-version[^/>]+/>""".trimMargin())

        val versionRegex = Regex("""<version>([^<]+)</version>""")

        zipTree(inputs.files.singleFile)
                .matching { include(PLUGIN_XML_PATH) }
                .singleFile
                .readText()
                .replace(placeholderRegex, "<depends>com.intellij.modules.cidr.lang</depends>")
                .replace(excludeRegex, "")
                .replace(ideaVersionRegex, "") // IDEA version to be specified in CLion or AppCode plugin.xml file.
                .replace(versionRegex, "") // Version to be specified in CLion or AppCode plugin.xml file.
                .also { pluginXmlText ->
                    val pluginXmlFile = File(outputs.files.singleFile, kotlinPluginXmlPath)
                    pluginXmlFile.parentFile.mkdirs()
                    pluginXmlFile.writeText(pluginXmlText)
                }
    }
}

// Prepare plugin.xml file with the actual version information (kotlin, cidr, plugin).
fun Project.preparePluginXml(
        predecessorProjectName: String,
        productVersion: String,
        strictProductVersionLimitation: Boolean,
        cidrPluginVersionFull: String
) = tasks.creating(Copy::class) {
    dependsOn("$predecessorProjectName:assemble")

    inputs.property("${project.name}-$name-strictProductVersionLimitation", strictProductVersionLimitation)
    inputs.property("${project.name}-$name-cidrPluginVersionFull", cidrPluginVersionFull)
    outputs.dir("$buildDir/$name")

    val predecessorProjectResources: File = project(predecessorProjectName)
            .the<JavaPluginConvention>()
            .sourceSets.getByName("main")
            .output
            .resourcesDir as File

    from(predecessorProjectResources, Action { include(PLUGIN_XML_PATH) })
    into(outputs.files.singleFile)

    applyCidrVersionRestrictions(productVersion, strictProductVersionLimitation, cidrPluginVersionFull)
}

private fun Copy.applyCidrVersionRestrictions(
        productVersion: String,
        strictProductVersionLimitation: Boolean,
        cidrPluginVersionFull: String
) {
    val dotsCount = productVersion.count { it == '.' }
    check(dotsCount in 1..2) {
        "Wrong CIDR product version format: $productVersion"
    }

    // private product versions don't have two dots
    val privateProductVersion = dotsCount == 1

    val applyStrictProductVersionLimitation = if (privateProductVersion && strictProductVersionLimitation) {
        // it does not make sense for private versions to apply strict version limitation
        logger.warn("Non-public CIDR product version [$productVersion] has been specified. The corresponding `versions.<product>.strict` property will be ignored.")
        false
    } else strictProductVersionLimitation

    val sinceBuild = if (privateProductVersion)
        productVersion
    else
        productVersion.substringBeforeLast('.')

    val untilBuild = if (applyStrictProductVersionLimitation)
    // if `strict` then restrict plugin to the same single version of CLion or AppCode
        "$sinceBuild.*"
    else
        productVersion.substringBefore('.') + ".*"

    filter {
        it
                .replace("<!--idea_version_placeholder-->",
                        "<idea-version since-build=\"$sinceBuild\" until-build=\"$untilBuild\"/>")
                .replace("<!--version_placeholder-->",
                        "<version>$cidrPluginVersionFull</version>")
    }
}
