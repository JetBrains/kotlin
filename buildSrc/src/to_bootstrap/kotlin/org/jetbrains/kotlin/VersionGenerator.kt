/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.regex.Pattern


open class VersionGenerator: DefaultTask() {
    @OutputDirectory
    fun getVersionSourceDirectory(): File {
        return getProject().file("build/generated")
    }

    @OutputFile
    open fun getVersionFile(): File? {
        return getProject().file(getVersionSourceDirectory().path + "/src/generated/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")
    }

    @Input
    open fun getKonanVersion(): String? {
        return getProject().getProperties().get("konanVersion").toString()
    }


    // TeamCity passes all configuration parameters into a build script as project properties.
    // Thus we can use them here instead of environment variables.
    @Optional
    @Input
    open fun getBuildNumber(): String? {
        val property: Any = getProject().findProperty("build.number") ?: return null
        return property.toString()
    }

    @Input
    open fun getMeta(): String {
        val konanMetaVersionProperty: Any = getProject().getProperties().get("konanMetaVersion") ?: return "MetaVersion.DEV"
        return "MetaVersion." + konanMetaVersionProperty.toString().toUpperCase()
    }

    private val versionPattern = Pattern.compile(
        "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-M(\\p{Digit}))?(?:-(\\p{Alpha}\\p{Alnum}*))?(?:-(\\d+))?$"
    )

    @TaskAction
    open fun generateVersion() {
        val matcher = versionPattern.matcher(getKonanVersion())
        require(matcher.matches()) { "Cannot parse Kotlin/Native version: \$konanVersion" }
        val major = matcher.group(1).toInt()
        val minor = matcher.group(2).toInt()
        val maintenanceStr = matcher.group(3)
        val maintenance = maintenanceStr?.toInt() ?: 0
        val milestoneStr = matcher.group(4)
        val milestone = milestoneStr?.toInt() ?: -1
        val buildNumber = getBuildNumber()
        getProject().getLogger().info("BUILD_NUMBER: " + getBuildNumber())
        var build = -1
        if (buildNumber != null) {
            val buildNumberSplit = buildNumber.split("-".toRegex()).toTypedArray()
            build = buildNumberSplit[buildNumberSplit.size - 1].toInt() // //7-dev-buildcount
        }
        try {
            PrintWriter(getVersionFile()).use { printWriter ->
                printWriter.println(
                    """package org.jetbrains.kotlin.konan
                       internal val currentCompilerVersion: CompilerVersion =
                       CompilerVersionImpl(${getMeta()}, $major, $minor,
                                           $maintenance, $milestone, $build)
                       val CompilerVersion.Companion.CURRENT: CompilerVersion
                       get() = currentCompilerVersion"""
                )
            }
        } catch (e: FileNotFoundException) {
            throw IllegalStateException(e)
        }
    }
}