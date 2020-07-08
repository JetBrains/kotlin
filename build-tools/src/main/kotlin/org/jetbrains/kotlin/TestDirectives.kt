/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

private const val MODULE_DELIMITER = ",\\s*"
// This pattern is a copy from the kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/test/TestFiles.java
private val FILE_OR_MODULE_PATTERN: Pattern = Pattern.compile("(?://\\s*MODULE:\\s*([^()\\n]+)(?:\\(([^()]+(?:" +
        "$MODULE_DELIMITER[^()]+)*)\\))?\\s*(?:\\(([^()]+(?:$MODULE_DELIMITER[^()]+)*)\\))?\\s*)?//\\s*FILE:\\s*(.*)$",
        Pattern.MULTILINE)

/**
 * Creates test files from the given source file that may contain different test directives.
 *
 * @return list of test files [TestFile] to be compiled
 */
fun buildCompileList(source: Path, outputDirectory: String): List<TestFile> {
    val result = mutableListOf<TestFile>()
    val srcFile = source.toFile()
    // Remove diagnostic parameters in external tests.
    val srcText = srcFile.readText().replace(Regex("<!.*?!>(.*?)<!>")) { match -> match.groupValues[1] }

    if (srcText.contains("// WITH_COROUTINES")) {
        result.add(TestFile("helpers.kt", "$outputDirectory/helpers.kt",
                createTextForHelpers(true), TestModule.support))
    }

    val matcher = FILE_OR_MODULE_PATTERN.matcher(srcText)
    if (!matcher.find()) {
        // There is only one file in the input
        result.add(TestFile(srcFile.name, "$outputDirectory/${srcFile.name}", srcText))
    } else {
        // There are several files
        var processedChars = 0
        var module: TestModule = TestModule.default
        var nextFileExists = true
        while (nextFileExists) {
            var moduleName = matcher.group(1)
            val moduleDependencies = matcher.group(2)
            val moduleFriends = matcher.group(3)

            if (moduleName != null) {
                moduleName = moduleName.trim { it <= ' ' }
                module = TestModule("${srcFile.name}.$moduleName",
                        moduleDependencies.parseModuleList().map {
                            if (it != "support") "${srcFile.name}.$it" else it
                        },
                        moduleFriends.parseModuleList().map { "${srcFile.name}.$it" })
            }

            val fileName = matcher.group(4)
            val filePath = "$outputDirectory/$fileName"
            val start = processedChars
            nextFileExists = matcher.find()
            val end = if (nextFileExists) matcher.start() else srcText.length
            val fileText = srcText.substring(start, end)
            processedChars = end
            if (fileName.endsWith(".kt")) {
                result.add(TestFile(fileName, filePath, fileText, module))
            }
        }
    }
    return result
}

private fun String?.parseModuleList() = this
        ?.split(Pattern.compile(MODULE_DELIMITER), 0)
        ?: emptyList()

/**
 * Test module from the test source declared by the [FILE_OR_MODULE_PATTERN].
 * Module should have a [name] and could have [dependencies] on other modules and [friends].
 *
 * There are 2 predefined modules:
 *  - [default] that contains all sources that don't declare a module,
 *  - [support] for a helper sources like Coroutines support.
 */
data class TestModule(
        val name: String,
        val dependencies: List<String>,
        val friends: List<String>
) {
    fun isDefaultModule() = this == default || name.endsWith(".main")

    companion object {
        val default = TestModule("default", emptyList(), emptyList())
        val support = TestModule("support", emptyList(), emptyList())
    }
}

/**
 * Represent a single test file that belongs to the [module].
 */
data class TestFile(val name: String,
                    val path: String,
                    var text: String = "",
                    val module: TestModule = TestModule.default
) {
    /**
     * Writes [text] to the file created from the [path].
     */
    fun writeTextToFile() {
        Paths.get(path).takeUnless { text.isEmpty() }?.run {
            parent.toFile()
                    .takeUnless { it.exists() }
                    ?.mkdirs()
            toFile().writeText(text)
        }
    }
}
