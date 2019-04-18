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
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import java.io.File

internal const val PLATFORM_DEPS_JAR_NAME = "kotlinNative-platformDeps.jar"

// Prepare Kotlin plugin main JAR file.
fun Project.pluginJar(
        originalPluginJar: Configuration,
        patchedFilesTasks: List<Task>,
        projectsToShadow: List<String>
): Jar {
    val jarTask = tasks.findByName("jar") as Jar? ?: task<Jar>("jar")

    return jarTask.apply {
        // First, include patched files.
        for (t in patchedFilesTasks) {
            dependsOn(t)
            from(t)
        }

        // Only then include contents of original JAR file.
        // Note: If there is a file with the same path inside of JAR file as in the output of one of
        // `patchedFilesTasks`, then the file from JAR will be ignored (due to DuplicatesStrategy.EXCLUDE).
        dependsOn(originalPluginJar)
        from(zipTree(originalPluginJar.singleFile)) { exclude(PLUGIN_XML_PATH) }

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
fun Project.platformDepsJar(
        productName: String,
        platformDepsDir: File,
        platformDepsReplacementsDir: File
) = tasks.creating(Zip::class) {
    archiveFileName.value = "kotlinNative-platformDeps-$productName.jar"
    destinationDirectory.value = file("$buildDir/$name")

    val platformDepsReplacements = platformDepsReplacementsDir.walkTopDown()
            .filter { it.isFile && it.length() > 0 }
            .map { it.relativeTo(platformDepsReplacementsDir).path }
            .toList()

    inputs.property("${project.name}-$name-platformDepsReplacements-amount", platformDepsReplacements.size)

    platformDepsReplacements.forEach {
        from(platformDepsReplacementsDir) { include(it) }
    }

    from(zipTree(fileTree(platformDepsDir).matching { include(PLATFORM_DEPS_JAR_NAME) }.singleFile)) {
        exclude(PLUGIN_XML_PATH)
        platformDepsReplacements.forEach { exclude(it) }
    }

    patchJavaXmls()
}

private fun Zip.patchJavaXmls() {
    val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
    val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

    val fileToMarkers = mapOf(
            javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
            javaPluginXmlPath to listOf(
                    "implementation=\"com.intellij.spi.SPIFileTypeFactory\"",
                    "implementationClass=\"com.intellij.lang.java.JavaDocumentationProvider\""
            )
    )

    commentXmlFiles(fileToMarkers)
}
