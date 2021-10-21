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
    val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null

    val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

    @OptIn(ExperimentalPathApi::class)
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun writeFileIfContentChanged(file: File, newText: String, logNotChanged: Boolean = true, forbidGenerationOnTeamcity: Boolean = true) {
        val parentFile = file.parentFile
        if (!parentFile.exists()) {
            if (forbidGenerationOnTeamcity) {
                if (failOnTeamCity("Create dir `${parentFile.path}`")) return
            }
            if (parentFile.mkdirs()) {
                println("Directory created: " + parentFile.absolutePath)
            } else {
                throw IllegalStateException("Cannot create directory: $parentFile")
            }
        }
        if (!isFileContentChangedIgnoringLineSeparators(file, newText)) {
            if (logNotChanged) {
                println("Not changed: " + file.absolutePath)
            }
            return
        }
        if (forbidGenerationOnTeamcity) {
            if (failOnTeamCity("Write file `${file.toPath()}`")) return
        }
        val useTempFile = !SystemInfo.isWindows
        val targetFile = file.toPath()
        val tempFile =
            if (useTempFile) createTempDirectory(targetFile.name) / "${targetFile.name}.tmp" else targetFile
        tempFile.writeText(newText, Charsets.UTF_8)
        println("File written: ${tempFile.toAbsolutePath()}")
        if (useTempFile) {
            tempFile.moveTo(targetFile, overwrite = true)
            println("Renamed $tempFile to $targetFile")
        }
        println()
    }

    private fun failOnTeamCity(message: String): Boolean {
        if (!isTeamCityBuild) return false

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
            it.isFile && it.readText().contains(GENERATED_MESSAGE)
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
