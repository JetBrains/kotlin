/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.harness

import org.jetbrains.kotlin.buildtools.tests.compilation.util.runProcess
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.KnownIssues
import java.io.File
import java.nio.file.Path

/**
 * Checks that an incremental build is indistinguishable from a clean rebuild of the same sources:
 * - both builds have the same result;
 * - the incremental build reports no errors the clean rebuild does not report
 *   (it may report fewer, as it stops at the first failed iteration);
 * - the incremental build does not fall back to a non-incremental one because of an internal failure;
 * - when the builds succeed, their outputs are byte-identical.
 *   Class files differing only in the order of their constant pools or members are equal unless [KnownIssues.classFileOrder] is included.
 */
object IcOracle {
    /** Returns the description of every violated property, empty if the builds are equivalent. */
    fun check(builds: StepBuilds): List<String> {
        val [incremental, rebuild] = builds.incremental to builds.rebuild
        return buildList {
            if (incremental.icFailures.isNotEmpty()) {
                add("The incremental compilation failed internally:\n" + incremental.icFailures.joinToString("\n"))
            }
            if (incremental.crash != null || incremental.result != rebuild.result) {
                add(
                    "The incremental build ${incremental.outcome}, the rebuild ${rebuild.outcome}.\n" +
                            "Incremental build errors:\n${incremental.errors.render()}\nRebuild errors:\n${rebuild.errors.render()}" +
                            (incremental.crash?.let { "\nIncremental build crash:\n${it.stackTraceToString()}" } ?: "")
                )
                return@buildList
            }
            val unexpectedErrors = incremental.errors - rebuild.errors
            if (unexpectedErrors.isNotEmpty()) {
                add("The incremental build reported errors the rebuild did not:\n${unexpectedErrors.render()}\nRebuild errors:\n${rebuild.errors.render()}")
            }
            if (incremental.isSuccessful && rebuild.isSuccessful) {
                compareOutputs(incremental, rebuild)?.let(::add)
            }
        }
    }

    private fun compareOutputs(incremental: BuildSnapshot, rebuild: BuildSnapshot): String? {
        val missing = rebuild.outputs.keys - incremental.outputs.keys
        val extra = incremental.outputs.keys - rebuild.outputs.keys
        val different = (incremental.outputs.keys intersect rebuild.outputs.keys)
            .filterNot { incremental.outputs.getValue(it).contentEquals(rebuild.outputs.getValue(it)) }
            .associateWith { path -> classDiff(rebuild.outputDirectory.resolve(path), incremental.outputDirectory.resolve(path)) }
            .filterValues { diff -> diff == null || diff.isNotEmpty() || KnownIssues.classFileOrder }
        if (missing.isEmpty() && extra.isEmpty() && different.isEmpty()) return null
        return buildString {
            appendLine("The outputs of the incremental build differ from the outputs of the rebuild.")
            if (missing.isNotEmpty()) appendLine("Missing in the incremental build: $missing")
            if (extra.isNotEmpty()) appendLine("Stale in the incremental build: $extra")
            for ([path, diff] in different) {
                appendLine("Different: $path")
                appendLine(render(diff, path).prependIndent("    "))
            }
        }
    }

    private fun Set<String>.render(): String = if (isEmpty()) "  <none>" else joinToString("\n") { "  $it" }

    /**
     * A line diff of the `javap` dumps of two class files: for each differing class member, its lines in [expected] marked with `-`
     * and its lines in [actual] marked with `+`. Returns `null` for the other files.
     */
    private fun classDiff(expected: Path, actual: Path): Map<String, List<String>>? {
        if (!expected.toString().endsWith(".class")) return null
        val expectedMembers = members(javap(expected))
        val actualMembers = members(javap(actual))
        return (expectedMembers.keys + actualMembers.keys).associateWith { member ->
            val expectedLines = expectedMembers[member]
            val actualLines = actualMembers[member]
            when {
                expectedLines == null -> listOf("+ (only in the incremental build)")
                actualLines == null -> listOf("- (only in the rebuild)")
                else -> (expectedLines - actualLines.toSet()).map { "- $it" } + (actualLines - expectedLines.toSet()).map { "+ $it" }
            }
        }.filterValues { it.isNotEmpty() }
    }

    private fun render(diff: Map<String, List<String>>?, path: String): String {
        if (diff == null) return "(${path.substringAfterLast('.')} file)"
        if (diff.isEmpty()) return "(the javap dumps are equal up to the order of the members, the difference is in constant pool, member or attribute order)"
        // every differing member is listed, each with a limited number of lines
        return diff.flatMap { [member, lines] ->
            listOf("  $member") + lines.take(MAX_MEMBER_DIFF_LINES) +
                    listOfNotNull("  ... ${lines.size - MAX_MEMBER_DIFF_LINES} more lines".takeIf { lines.size > MAX_MEMBER_DIFF_LINES })
        }.joinToString("\n")
    }

    /** Splits a `javap` dump into the sections of the class members, keyed by their declarations, and the attributes of the class. */
    private fun members(lines: List<String>): Map<String, List<String>> {
        val members = linkedMapOf<String, MutableList<String>>()
        var current = members.getOrPut("<class>") { mutableListOf() }
        for (line in lines) {
            when {
                MEMBER_DECLARATION.matches(line) -> current = members.getOrPut(line.trim()) { mutableListOf() }
                // the end of the class body, followed by the attributes of the class
                line == "}" -> current = members.getOrPut("<class attributes>") { mutableListOf() }
                // the members are separated by blank lines, which belong to no member
                line.isNotBlank() -> current += line
            }
        }
        return members
    }

    private fun javap(classFile: Path): List<String> {
        val javaHome = System.getProperty("java.home")
        val javap = File(javaHome, "bin/javap").let {
            // in case we got java.home pointing to the JRE part, javap is located in the outer JDK part
            if (it.exists()) it else File(javaHome, "../bin/javap")
        }
        val result = runProcess(listOf(javap.absolutePath, "-v", "-p", "-c", classFile.toString()), classFile.parent)
        if (!result.isSuccessful) return listOf("<javap failed: ${result.output}>")
        return result.output.lines()
            // the header contains the path, the last modification time and the checksum of the file
            .dropWhile { !it.contains("class ") && !it.contains("interface ") }
            .filterNot { it.trimStart().startsWith("#") } // constant pool entries are numbered differently with an unchanged semantics
            .map { it.replace(CONSTANT_POOL_INDEX, "# ") }
    }

    private const val MAX_MEMBER_DIFF_LINES = 30

    /** A constant pool index with the padding that aligns the following comment, the padding depends on the width of the index. */
    private val CONSTANT_POOL_INDEX = Regex("#\\d+\\s*")

    /** `javap -v` prints the member declarations with an indentation of two spaces, and their contents deeper. */
    private val MEMBER_DECLARATION = Regex("^ {2}\\S.*;$")
}
