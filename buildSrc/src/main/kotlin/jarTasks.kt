/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.util.regex.Pattern
import java.io.File

internal const val PLATFORM_DEPS_JAR_NAME = "kotlinNative-platformDeps.jar"

// Prepare Kotlin plugin main JAR file.
fun Project.pluginJar(
        originalPluginJar: Configuration,
        pluginXmlPrepareTask: Task,
        projectsToShadow: List<String>
): Jar {
    val jarTask = tasks.findByName("jar") as Jar? ?: task<Jar>("jar")

    return jarTask.apply {
        dependsOn(originalPluginJar)
        from(zipTree(originalPluginJar.singleFile)) { exclude(PLUGIN_XML_PATH) }

        dependsOn(pluginXmlPrepareTask)
        from(pluginXmlPrepareTask)

        for (p in projectsToShadow) {
            dependsOn("$p:classes")
            from(getMainSourceSetOutput(p)) { exclude(PLUGIN_XML_PATH) }
        }

        archiveBaseName.value = project.the<BasePluginConvention>().archivesBaseName
        archiveFileName.value = "kotlin-plugin.jar"
        manifest.attributes.apply {
            put("Implementation-Vendor", "JetBrains")
            put("Implementation-Title", archiveBaseName.value)
        }
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
    }
}

// Prepare patched "platformDeps" JAR file.
fun Project.platformDepsJar(productName: String, platformDepsDir: File) = tasks.creating(Zip::class) {
    archiveFileName.value = "kotlinNative-platformDeps-$productName.jar"
    destinationDirectory.value = file("$buildDir/$name")
    from(zipTree(fileTree(platformDepsDir).matching { include(PLATFORM_DEPS_JAR_NAME) }.singleFile)) { exclude(PLUGIN_XML_PATH) }
    patchJavaXmls()
}

private fun Zip.patchJavaXmls() {
    val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
    val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

    patchFiles(
        mapOf(
            javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
            javaPluginXmlPath to listOf(
                "implementation=\"com.intellij.spi.SPIFileTypeFactory\"",
                "implementationClass=\"com.intellij.lang.java.JavaDocumentationProvider\""
            )
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
