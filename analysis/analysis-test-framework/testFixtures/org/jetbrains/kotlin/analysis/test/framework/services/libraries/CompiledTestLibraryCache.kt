/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*

/**
 * Libraries compiled by [CliTestModuleCompiler] in this JVM, by the content of their compilation inputs.
 *
 * Several test suites run on the same test data, and the tests of a test data directory often share library modules, so a test JVM
 * would otherwise compile the same libraries many times. The whole JVM runs the same compiler, so the same inputs give the same library.
 *
 * The inputs are in per-test temporary directories, whose paths differ between tests. So the key of a library takes the content
 * of such paths instead of the paths themselves. A restored library still contains the source paths of the test which compiled it,
 * e.g., in the IR of a KLIB.
 */
@OptIn(ExperimentalPathApi::class)
internal object CompiledTestLibraryCache {
    private val temporaryRoots: List<String> by lazy {
        val temporaryRoot = File(System.getProperty("java.io.tmpdir"))
        listOf(temporaryRoot.absolutePath, temporaryRoot.canonicalPath).distinct()
    }

    /**
     * Matches paths in the temporary directory up to a separator of paths or option values.
     */
    private val temporaryPathRegex: Regex by lazy {
        val excludedCharacters = Regex.escape("${File.pathSeparatorChar},=")
        Regex(temporaryRoots.joinToString("|", prefix = "(?:", postfix = ")") { Regex.escape(it) } + "[^$excludedCharacters\\s]*")
    }

    /**
     * Copies of the compiled libraries, in the temporary directory, which the test task deletes after the run.
     */
    private val cacheDirectory: Path by lazy { Files.createTempDirectory("compiledTestLibraries") }

    private val libraries = ConcurrentHashMap<String, Path>()

    fun computeKey(
        compiler: CliTestModuleCompiler,
        sourceDirectories: List<Path>,
        options: List<String>,
        classpath: List<String>,
        libraryName: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.updateText(compiler::class.java.name)
        digest.updateText(libraryName)

        for ([index, directory] in sourceDirectories.withIndex()) {
            digest.updateText("<sources-$index>")
            digest.updateWithContent(directory)
        }

        for (option in options) {
            digest.updateText("<option>")
            digest.updateText(option.withNormalizedPaths(sourceDirectories))
        }

        for (entry in classpath) {
            digest.updateText("<classpath>")
            digest.updateText(entry.withNormalizedPaths(sourceDirectories))
        }

        return BigInteger(1, digest.digest()).toString(16)
    }

    /**
     * Copies the library compiled for [key] to [outputPath]. Returns `false` if there is no such library.
     */
    fun restore(key: String, outputPath: Path): Boolean {
        val library = libraries[key] ?: return false
        library.copyToRecursively(outputPath, followLinks = false, overwrite = true)
        return true
    }

    /**
     * Stores a copy of the library compiled for [key] at [outputPath], so that [outputPath] can be deleted with its test.
     */
    fun store(key: String, outputPath: Path) {
        if (!outputPath.exists()) return

        libraries.computeIfAbsent(key) {
            val copy = cacheDirectory / key
            outputPath.copyToRecursively(copy, followLinks = false, overwrite = true)
            copy
        }
    }

    private fun String.withNormalizedPaths(sourceDirectories: List<Path>): String {
        var result = this
        for ([index, directory] in sourceDirectories.withIndex()) {
            for (form in setOf(directory.toString(), directory.absolutePathString(), directory.toFile().canonicalPath)) {
                result = result.replace(form, "<sources-$index>")
            }
        }

        // Other per-test paths, such as the libraries of other test modules
        return temporaryPathRegex.replace(result) { match ->
            val contentDigest = MessageDigest.getInstance("SHA-256").apply { updateWithContent(Path(match.value)) }
            "<content-${BigInteger(1, contentDigest.digest()).toString(16)}>"
        }
    }

    private fun MessageDigest.updateWithContent(path: Path) {
        when {
            path.isRegularFile() -> update(path.readBytes())
            path.isDirectory() -> {
                val files = path.walk().filter { it.isRegularFile() }.sortedBy { it.relativeTo(path).invariantSeparatorsPathString }
                for (file in files) {
                    updateText(file.relativeTo(path).invariantSeparatorsPathString)
                    update(file.readBytes())
                }
            }
            else -> updateText("<missing>")
        }
    }

    private fun MessageDigest.updateText(text: String) {
        update(text.toByteArray())
        update(0)
    }
}
