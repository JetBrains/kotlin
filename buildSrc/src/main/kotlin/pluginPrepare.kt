/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import java.util.regex.Pattern

const val pluginXmlPath = "META-INF/plugin.xml"
const val kotlinPluginXmlPath = "META-INF/KotlinPlugin.xml"

fun Project.pluginJar(body: Jar.() -> Unit): Jar {
    val jarTask = tasks.findByName("jar") as Jar? ?: task<Jar>("jar")

    return jarTask.apply {
        body()
        baseName = project.the<BasePluginConvention>().archivesBaseName
        archiveName = "kotlin-plugin.jar"
        manifest.attributes.apply {
            put("Implementation-Vendor", "JetBrains")
            put("Implementation-Title", baseName)
        }
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
    }
}

fun Zip.patchJavaXmls() {
    val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
    val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

    patchFiles(
        mapOf(
            javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
            javaPluginXmlPath to listOf("implementation=\"com.intellij.spi.SPIFileTypeFactory\"")
        )
    )
}

private fun Zip.patchFiles(fileToMarkers: Map<String, List<String>>) {
    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }
    }
    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

fun Copy.applyCidrVersionRestrictions(
    productVersion: String,
    strictProductVersionLimitation: Boolean,
    pluginVersion: String
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
                     "<version>$pluginVersion</version>")
    }
}
