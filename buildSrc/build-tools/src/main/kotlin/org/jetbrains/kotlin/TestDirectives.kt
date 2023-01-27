/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

private const val MODULE_DELIMITER = ",\\s*"
// These patterns are copies from
// kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/test/TestFiles.java
// kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/test/KotlinTestUtils.java
private val MODULE_PATTERN: Pattern = Pattern.compile("//\\s*MODULE:\\s*([^()\\n]+)(?:\\(([^()]+(?:" +
        MODULE_DELIMITER + "[^()]+)*)\\))?\\s*(?:\\(([^()]+(?:" + MODULE_DELIMITER + "[^()]+)*)\\))?\n")
private val FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)\n")

private val DIRECTIVE_PATTERN = Pattern.compile("^//\\s*[!]?([A-Z_]+)(:[ \\t]*(.*))?$", Pattern.MULTILINE)

/**
 * Creates test files from the given source file that may contain different test directives.
 *
 * @return list of test files [TestFile] to be compiled
 */
fun buildCompileList(
        source: Path,
        outputDirectory: String,
        defaultModule: TestModule = TestModule.default()): List<TestFile> {
    val result = mutableListOf<TestFile>()
    val srcFile = source.toFile()
    // Remove diagnostic parameters in external tests.
    val srcText = srcFile.readText().replace(Regex("<!.*?!>(.*?)<!>")) { match -> match.groupValues[1] }

    var supportModule: TestModule? = if (srcText.contains("// WITH_COROUTINES")) TestModule.support() else null

    val moduleMatcher = MODULE_PATTERN.matcher(srcText)
    val fileMatcher = FILE_PATTERN.matcher(srcText)
    var nextModuleExists = moduleMatcher.find()
    var nextFileExists = fileMatcher.find()

    if (!nextModuleExists && !nextFileExists) {
        // There is only one file in the input
        if (supportModule != null) defaultModule.dependencies.add(supportModule.name)
        result.add(TestFile(srcFile.name, "$outputDirectory/${srcFile.name}", srcText, defaultModule))
    } else {
        // There are several files
        var processedChars = 0
        var module: TestModule = defaultModule

        while (nextModuleExists || nextFileExists) {
            if (nextModuleExists) {
                var moduleName = moduleMatcher.group(1)
                val moduleDependencies = moduleMatcher.group(2)
                val moduleFriends = moduleMatcher.group(3)

                if (moduleName != null) {
                    moduleName = moduleName.trim { it <= ' ' }
                    val dependencies = mutableListOf<String>().apply {
                        addAll(moduleDependencies.parseModuleList()
                                .map { if (it != "support") "${srcFile.name}.$it" else it }
                        )
                    }
                    module = TestModule(
                            "${srcFile.name}.$moduleName",
                            dependencies,
                            mutableListOf<String>().apply {
                                addAll(moduleFriends.parseModuleList().map { "${srcFile.name}.$it" })
                            }
                    )
                }
            }
            if (supportModule != null && !module.dependencies.contains("support")) {
                module.dependencies.add("support")
            }

            nextModuleExists = moduleMatcher.find()
            while (nextFileExists) {
                val fileName = fileMatcher.group(1)
                val filePath = "$outputDirectory/$fileName"
                val start = processedChars
                nextFileExists = fileMatcher.find()
                val end = when {
                    nextFileExists && nextModuleExists -> Math.min(fileMatcher.start(), moduleMatcher.start())
                    nextFileExists -> fileMatcher.start()
                    else -> srcText.length
                }
                val fileText = srcText.substring(start, end)
                processedChars = end
                if (fileName.endsWith(".kt")) {
                    result.add(TestFile(fileName, filePath, fileText, module))
                }
                if (nextModuleExists && nextFileExists && fileMatcher.start() > moduleMatcher.start()) break
            }
        }
    }
    return result
}

private fun String?.parseModuleList() = this
        ?.split(Pattern.compile(MODULE_DELIMITER), 0)
        ?: emptyList()

/**
 * Test module from the test source declared by the [MODULE_PATTERN].
 * Module should have a [name] and could have [dependencies] on other modules and [friends].
 *
 * There are 2 predefined modules:
 *  - [default] that contains all sources that don't declare a module,
 *  - [support] for a helper sources like Coroutines support.
 */
data class TestModule(
    val name: String,
    val dependencies: MutableList<String>,
    val friends: MutableList<String>
) {
    val files = mutableListOf<TestFile>()
    fun isDefaultModule() = this.name == "default" || name.endsWith(".main")
    fun isSupportModule() = this.name == "support"

    val hasVersions get() = this.files.any { it.version != null }
    fun versionFiles(version: Int) = this.files.filter { it.version == null || it.version == version }

    companion object {
        @JvmStatic fun default() = TestModule("default", mutableListOf(), mutableListOf())
        @JvmStatic fun support() = TestModule("support", mutableListOf(), mutableListOf())
    }
}

/**
 * Represent a single test file that belongs to the [module].
 */
data class TestFile(
    val name: String,
    val path: String,
    var text: String = "",
    val module: TestModule
) {
    init {
        this.module.files.add(this)
    }

    val directives: Map<String, String> by lazy {
        parseDirectives()
    }

    val version: Int? get() = this.directives["VERSION"]?.toInt()

    fun parseDirectives(): Map<String, String> {
        val newDirectives = mutableMapOf<String, String>()
        val directiveMatcher: Matcher = DIRECTIVE_PATTERN.matcher(text)
        while (directiveMatcher.find()) {
            val name = directiveMatcher.group(1)
            val value = directiveMatcher.group(3) ?: ""
            newDirectives[name] = value
        }
        return newDirectives
    }

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
