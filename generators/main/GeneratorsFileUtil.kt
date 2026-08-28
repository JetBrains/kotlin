/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.io.IOException
import kotlin.io.path.*

object GeneratorsFileUtil {
    private val isTeamCityBuild: Boolean =
        System.getProperty("teamcity", "false").toBoolean() || System.getenv("TEAMCITY_VERSION") != null

    /**
     * Makes generators write the new content instead of reporting a "Re-generation needed!" build problem.
     *
     * Enabled during version branching, when every version-dependent generator produces a diff at once. See KT-88942.
     *
     * The env variable covers generators not launched through the `generator`/`generatedSourcesTask` build conventions.
     */
    private val isRegenerationCheckDisabled: Boolean =
        System.getProperty("kotlin.build.disable.regeneration.check", "false").toBoolean() ||
                System.getenv("KOTLIN_BUILD_DISABLE_REGENERATION_CHECK") != null

    val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

    const val GENERATED_MESSAGE_PREFIX = "// This file was generated automatically. See "
    const val GENERATED_MESSAGE_SUFFIX = "// DO NOT MODIFY IT MANUALLY."

    @OptIn(ExperimentalPathApi::class)
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun writeFileIfContentChanged(file: File, newText: String, logNotChanged: Boolean = true, forbidGenerationOnTeamcity: Boolean = true): Boolean {
        val parentFile = file.parentFile
        if (!parentFile.exists()) {
            if (forbidGenerationOnTeamcity) {
                if (failOnTeamCity("Create dir `${parentFile.path}`")) return false
            }
            if (parentFile.mkdirs()) {
                println("Directory created: " + parentFile.absolutePath)
            } else if (!parentFile.exists()) {
                throw IllegalStateException("Cannot create directory: $parentFile")
            }
        }
        if (!isFileContentChangedIgnoringLineSeparators(file, newText)) {
            if (logNotChanged) {
                println("Not changed: " + file.absolutePath)
            }
            return false
        }
        if (forbidGenerationOnTeamcity) {
            if (failOnTeamCity("Write file `${file.toPath()}`")) return false
        }
        val useTempFile = !SystemInfo.isWindows
        val targetFile = file.toPath()
        val tempFile =
            if (useTempFile) createTempDirectory(targetFile.name) / "${targetFile.name}.tmp" else targetFile
        tempFile.writeText(newText, Charsets.UTF_8)
        if (useTempFile) {
            tempFile.moveTo(targetFile, overwrite = true)
        }
        println("File written: ${targetFile.toAbsolutePath()}")
        return true
    }

    private fun failOnTeamCity(message: String): Boolean {
        if (!isTeamCityBuild || isRegenerationCheckDisabled) return false

        fun String.escapeForTC(): String = StringBuilder(length).apply {
            for (char in this@escapeForTC) {
                append(
                    when (char) {
                        '|' -> "||"
                        '\'' -> "|'"
                        '\n' -> "|n"
                        '\r' -> "|r"
                        '[' -> "|["
                        ']' -> "|]"
                        else -> char
                    }
                )
            }
        }.toString()

        val fullMessage = "[Re-generation needed!] $message\n" +
                "Run correspondent (check the log above) Gradle task locally and commit changes."

        println("##teamcity[buildProblem description='${fullMessage.escapeForTC()}']")
        return true
    }

    fun isFileContentChangedIgnoringLineSeparators(file: File, content: String): Boolean {
        val currentContent: String = try {
            StringUtil.convertLineSeparators(file.readText(Charsets.UTF_8))
        } catch (ignored: Throwable) {
            return true
        }
        return StringUtil.convertLineSeparators(content) != currentContent
    }

    fun collectPreviouslyGeneratedFiles(generationPath: File): List<File> {
        return generationPath.walkTopDown().filter {
            it.isFile && it.readText().let { GENERATED_MESSAGE_PREFIX in it && GENERATED_MESSAGE_SUFFIX in it }
        }.toList()
    }

    fun removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles: List<File>, generatedFiles: List<File>) {
        val generatedFilesPath = generatedFiles.mapTo(mutableSetOf()) { it.absolutePath }

        for (file in previouslyGeneratedFiles) {
            if (file.absolutePath !in generatedFilesPath) {
                if (failOnTeamCity("File delete `${file.absolutePath}`")) continue
                println("Deleted: ${file.absolutePath}")
                file.delete()
            }
        }
    }
}
