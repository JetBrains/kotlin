/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object GeneratorsFileUtil {
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun writeFileIfContentChanged(file: File, newText: String, logNotChanged: Boolean = true) {
        val parentFile = file.parentFile
        if (!parentFile.exists()) {
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
        val useTempFile = !SystemInfo.isWindows
        val tempFile =
            if (useTempFile) File(createTempDir(file.name), file.name + ".tmp") else file
        tempFile.writeText(newText, Charsets.UTF_8)
        println("File written: " + tempFile.absolutePath)
        if (useTempFile) {
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            println("Renamed $tempFile to $file")
        }
        println()
    }

    fun isFileContentChangedIgnoringLineSeparators(file: File, content: String): Boolean {
        val currentContent: String = try {
            StringUtil.convertLineSeparators(file.readText(Charsets.UTF_8))
        } catch (ignored: Throwable) {
            return true
        }
        return StringUtil.convertLineSeparators(content) != currentContent
    }
}