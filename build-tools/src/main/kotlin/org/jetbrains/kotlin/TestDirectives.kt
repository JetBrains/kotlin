/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Creates files from the given source file that may contain different test directives.
 *
 * @return list of file names to be compiled
 */
fun KonanTest.buildCompileList(outputDirectory: String): List<String> {
    val result = mutableListOf<String>()
    val srcFile = project.file(source)
    // Remove diagnostic parameters in external tests.
    val srcText = srcFile.readText().replace(Regex("<!.*?!>(.*?)<!>")) { match -> match.groupValues[1] }

    if (srcText.contains("// WITH_COROUTINES")) {
        val coroutineHelpersFileName = "$outputDirectory/helpers.kt"
        createFile(coroutineHelpersFileName, createTextForHelpers(true))
        result.add(coroutineHelpersFileName)
    }

    val filePattern = Pattern.compile("(?m)// *FILE: *(.*)")
    val matcher = filePattern.matcher(srcText)

    if (!matcher.find()) {
        // There is only one file in the input
        val filePath = "$outputDirectory/${srcFile.name}"
        registerKtFile(result, filePath, srcText)
    } else {
        // There are several files
        var processedChars = 0
        while (true) {
            val filePath = "$outputDirectory/${matcher.group(1)}"
            val start = processedChars
            val nextFileExists = matcher.find()
            val end = if (nextFileExists) matcher.start() else srcText.length
            val fileText = srcText.substring(start, end)
            processedChars = end
            registerKtFile(result, filePath, fileText)
            if (!nextFileExists) break
        }
    }
    return result
}

internal fun createFile(file: String, text: String) = Paths.get(file).run {
    parent.toFile()
            .takeUnless { it.exists() }
            ?.mkdirs()
    toFile().writeText(text)
}

internal fun registerKtFile(sourceFiles: MutableList<String>, newFilePath: String, newFileContent: String) {
    createFile(newFilePath, newFileContent)
    if (newFilePath.endsWith(".kt")) {
        sourceFiles.add(newFilePath)
    }
}
