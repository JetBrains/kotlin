/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import java.io.File
import java.util.zip.ZipFile

/**
 * Task to find out collisions before producing fat jar.
 *
 * @property configurations added to fat jar configurations
 * @property resolvingRules a map containing rules to resolve conflicts. Key - conflicting file, value - a jar to copy the file from
 * @property resolvingRulesWithRegexes a map containing rules to resolve conflicts. Key - regular expression describing conflicting file, value - a jar to copy the file from
 * @property librariesWithIgnoredClassCollisions libraries which collision in class files are ignored
 */
open class CollisionDetector : DefaultTask() {
    @InputFiles
    var configurations = listOf<Configuration>()
    @Input
    val resolvingRules = mutableMapOf<String, String>()
    @Input
    val resolvingRulesWithRegexes = mutableMapOf<Regex, String>()
    @Input
    val librariesWithIgnoredClassCollisions = mutableListOf<String>()
    val resolvedConflicts = mutableMapOf<String, File>()

    // Key - filename, value - jar file containing it.
    private val filesInfo = mutableMapOf<String, String>()

    @TaskAction
    fun run() {
        configurations.forEach { configuration ->
            configuration.files.filter { it.name.endsWith(".jar") }.forEach { processedFile ->
                ZipFile(processedFile).use { zip ->
                    zip.entries().asSequence().filterNot {
                        it.isDirectory ||
                        it.name.equals("META-INF/MANIFEST.MF", ignoreCase = true) ||
                        it.name.startsWith("META-INF/services/", ignoreCase = true)
                    }.forEach {
                        val outputPath = it.name
                        if (outputPath in filesInfo.keys) {
                            val rule = resolvingRules.getOrElse(outputPath) {
                                resolvingRulesWithRegexes.entries.firstOrNull { (key, _) -> key.matches(outputPath) }?.value
                            }
                            var ignoreJar = false
                            if (rule != null && processedFile.name.startsWith(rule)) {
                                resolvedConflicts[outputPath] = processedFile
                            } else {
                                // Skip class files from ignored libraries if version of libraries had collision are the same.
                                val versionRegex = "\\d+\\.\\d+(\\.\\d+)?(-\\w+(-\\d+)?)?".toRegex()
                                val currentVersion = versionRegex.find(processedFile.name)?.groupValues?.get(0)
                                val collisionLibVersion = versionRegex.find(filesInfo.getValue(outputPath))?.groupValues?.get(0)
                                if (outputPath.endsWith(".class") && currentVersion == collisionLibVersion) {
                                    if (processedFile.name == filesInfo[outputPath]) {
                                        ignoreJar = true
                                    } else {
                                        librariesWithIgnoredClassCollisions.forEach {
                                            if (processedFile.name.startsWith(it)) {
                                                ignoreJar = true
                                            }
                                        }
                                    }
                                }
                            }
                            if (rule == null && !ignoreJar) {
                                error("Collision is detected. File $outputPath is found in ${filesInfo[outputPath]} and ${processedFile.name}")
                            }
                        } else {
                            filesInfo[outputPath] = processedFile.name
                        }
                    }
                }
            }
        }
    }
}
