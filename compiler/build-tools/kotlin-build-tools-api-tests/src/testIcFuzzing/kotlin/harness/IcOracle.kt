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
 *   (it may report fewer, as it stops at the first failed iteration),
 *   errors differing only in the order of their lines are equal unless [KnownIssues.classFileOrder] is included;
 * - the incremental build does not fall back to a non-incremental one because of an internal failure;
 * - when the builds succeed, their outputs are byte-identical.
 *   Class files differing only in the order of their constant pools, members or attributes are equal unless [KnownIssues.classFileOrder]
 *   is included.
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
            val rebuildErrors = rebuild.errors.mapTo(mutableSetOf(), ::comparable)
            val unexpectedErrors = incremental.errors.filterTo(mutableSetOf()) { comparable(it) !in rebuildErrors }
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

    /** A diagnostic listing declarations lists them in another order when they are read from a class file. */
    private fun comparable(error: String): String = if (KnownIssues.classFileOrder) error else error.lines().sorted().joinToString("\n")

    /**
     * A line diff of the `javap` dumps of two class files: for each differing class member, its lines in [expected] marked with `-`
     * and its lines in [actual] marked with `+`, and the entries of the constant pool which only one of them has.
     * The lines of a member are compared in order and with their repetitions, while the members, the attributes of the class
     * and the constant pool entries may be in any order: an empty diff means that the class files differ only in these orders.
     * Returns `null` for the other files.
     */
    private fun classDiff(expected: Path, actual: Path): Map<String, List<String>>? {
        if (!expected.toString().endsWith(".class")) return null
        val [expectedLines, expectedPool] = javap(expected)
        val [actualLines, actualPool] = javap(actual)
        val expectedMembers = members(expectedLines)
        val actualMembers = members(actualLines)
        val memberDiffs = (expectedMembers.keys + actualMembers.keys).associateWith { member ->
            val expectedLines = expectedMembers[member]
            val actualLines = actualMembers[member]
            when {
                expectedLines == null -> listOf("+ (only in the incremental build)")
                actualLines == null -> listOf("- (only in the rebuild)")
                expectedLines == actualLines -> emptyList()
                // e.g. the inner classes of a file with inline functions are listed in another order when it is compiled with their call sites
                member == CLASS_ATTRIBUTES && expectedLines.sorted() == actualLines.sorted() -> emptyList()
                else -> ((expectedLines - actualLines.toSet()).map { "- $it" } + (actualLines - expectedLines.toSet()).map { "+ $it" })
                    .ifEmpty { listOf("  (the same lines in a different order or number)") }
            }
        }.filterValues { it.isNotEmpty() }
        val poolDiff = multisetDiff(expectedPool, actualPool)
        return if (poolDiff.isEmpty()) memberDiffs else memberDiffs + ("<constant pool>" to poolDiff)
    }

    /** The elements whose numbers of occurrences differ, marked with `-` for [expected] and `+` for [actual]. */
    private fun multisetDiff(expected: List<String>, actual: List<String>): List<String> {
        val expectedCounts = expected.groupingBy { it }.eachCount()
        val actualCounts = actual.groupingBy { it }.eachCount()
        return (expectedCounts.keys + actualCounts.keys).flatMap { entry ->
            val difference = (actualCounts[entry] ?: 0) - (expectedCounts[entry] ?: 0)
            List(kotlin.math.abs(difference)) { if (difference > 0) "+ $entry" else "- $entry" }
        }
    }

    private fun render(diff: Map<String, List<String>>?, path: String): String {
        if (diff == null) return "(${path.substringAfterLast('.')} file)"
        if (diff.isEmpty()) return "(the class files differ only in the order of their members and of their constant pool entries)"
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
                line == "}" -> current = members.getOrPut(CLASS_ATTRIBUTES) { mutableListOf() }
                // the members are separated by blank lines, which belong to no member
                line.isNotBlank() -> current += line
            }
        }
        return members
    }

    /**
     * The `javap` dump of [classFile] without the constant pool, with the constant pool indices removed, and the entries of the constant pool,
     * whose indices are removed as well: an entry is its type with its value or with the comment describing what it refers to.
     */
    private fun javap(classFile: Path): Pair<List<String>, List<String>> {
        val javaHome = System.getProperty("java.home")
        val javap = File(javaHome, "bin/javap").let {
            // in case we got java.home pointing to the JRE part, javap is located in the outer JDK part
            if (it.exists()) it else File(javaHome, "../bin/javap")
        }
        val result = runProcess(listOf(javap.absolutePath, "-v", "-p", "-c", classFile.toString()), classFile.parent)
        if (!result.isSuccessful) return listOf("<javap failed: ${result.output}>") to emptyList()
        val [pool, body] = result.output.lines()
            // the header contains the path, the last modification time and the checksum of the file
            .dropWhile { !it.contains("class ") && !it.contains("interface ") }
            .partition { it.trimStart().startsWith("#") }
        return body.map { it.replace(CONSTANT_POOL_INDEX, "# ") } to pool.map { it.replace(CONSTANT_POOL_INDEX, "").replace(WHITESPACE, " ").trim() }
    }

    private const val MAX_MEMBER_DIFF_LINES = 30

    private const val CLASS_ATTRIBUTES = "<class attributes>"

    /** A constant pool index with the padding that aligns the following comment, the padding depends on the width of the index. */
    private val CONSTANT_POOL_INDEX = Regex("#\\d+\\s*")

    private val WHITESPACE = Regex("\\s+")

    /** `javap -v` prints the member declarations with an indentation of two spaces, and their contents deeper. */
    private val MEMBER_DECLARATION = Regex("^ {2}\\S.*;$")
}
