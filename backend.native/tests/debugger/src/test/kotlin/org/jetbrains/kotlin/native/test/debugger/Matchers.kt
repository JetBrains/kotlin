/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.test.debugger

import org.intellij.lang.annotations.Language
import org.junit.Assert.fail
import java.io.IOException
import java.nio.file.Files

/**
 * An integration test for debug info.
 *
 * It works by compiling a given [programText] with debug info,
 * then launching lldb, feeding it commands from [lldbSession]
 * and matching the output.
 *
 * [lldbSession] specifies both lldb commands and expected output
 * using a DLS, which looks like this:
 *
 *     > b main.kt:5
 *     Breakpoint 1: [..]
 *     > r
 *     Process [..] stopped
 *     [..] at main.kt:5, [..] stop reason = breakpoint [..]
 *     > fr var
 *     (int) a = 92
 *     (int) b = 2
 *
 * It consists of blocks of the form
 *
 *     > lldb command
 *     response line pattern
 *     another pattern
 *
 * Command after `>` is passed to lldb exactly. The output of the command
 * is then matched against a set of patterns. Matching is done line by line:
 * for every pattern, there must be a matching line in the output, but you don't
 * have to specify a pattern for every line. In particular, it's possible not to
 * specify any patterns at all:
 *
 *     > b main.kt:2
 *     > r
 *     > n
 *     [..] at main.kt:3, [..] stop reason = step over
 *
 * The patterns themselves are simple. The only special symbol is `[..]` which
 * means arbitrary substring, which can help match random data, timings, and OS-dependent output.
 * For example, to match
 *
 *     Current executable set to '/tmp/debugger_test7458723719928260513/program.kexe' (x86_64).
 *
 * one writes
 *
 *     Current executable set to [..]program.kexe[..]
 */
fun lldbTest(@Language("kotlin") programText: String, lldbSession: String) {
    if (!haveLldb) {
        println("Skipping test: no LLDB")
        return
    }

    if (!isOsxDevToolsEnabled) {
        println("""Development tools aren't available.
                   |Please consider to execute:
                   |  ${DistProperties.devToolsSecurity} -enable
                   |or
                   |  csrutil disable
                   |to run lldb tests""".trimMargin())
        return
    }
    val lldbSessionSpec = LldbSessionSpecification.parse(lldbSession)

    val tmpdir = Files.createTempDirectory("debugger_test")
    tmpdir.toFile().deleteOnExit()
    val source = tmpdir.resolve("main.kt")
    val output = tmpdir.resolve("program.kexe")

    val driver = ToolDriver(DistProperties.konanc, DistProperties.lldb, DistProperties.dwarfDump, DistProperties.lldbPrettyPrinters)
    Files.write(source, programText.trimIndent().toByteArray())
    driver.compile(source, output, "-g")
    val result = driver.runLldb(output, lldbSessionSpec.commands)
    lldbSessionSpec.match(result)
}

private val isOsxDevToolsEnabled: Boolean by lazy {
    //TODO: add OSX checks.
    val rawStatus = subprocess(DistProperties.devToolsSecurity, "-status")
    println("> status: $rawStatus")

    val r = Regex("^.*\\ (enabled|disabled).$")
    r.find(rawStatus.stdout)?.destructured?.component1() == "enabled"
}

fun dwarfDumpTest(@Language("kotlin") programText: String, flags: List<String>, test:List<DwarfTag>.()->Unit) {
    if (!haveDwarfDump) {
        println("Skipping test: no dwarfdump")
        return
    }


    with(Files.createTempDirectory("dwarfdump_test")) {
        toFile().deleteOnExit()
        val source = resolve("main.kt")
        val output = resolve("program.kexe")

        val driver = ToolDriver(
                DistProperties.konanc,
                DistProperties.lldb,
                DistProperties.dwarfDump,
                DistProperties.lldbPrettyPrinters)
        Files.write(source, programText.trimIndent().toByteArray())
        driver.compile(source, output, "-g", *flags.toTypedArray())
        driver.runDwarfDump(output, test)
    }
}

private val haveDwarfDump: Boolean by lazy {
    val version = try {
        subprocess(DistProperties.dwarfDump, "--version")
                .takeIf { it.process.exitValue() == 0 }
                ?.stdout
    } catch (e: IOException) {
        null
    }

    if (version == null) {
        println("No LLDB found")
    } else {
        println("Using $version")
    }

    version != null
}

private val haveLldb: Boolean by lazy {
    val lldbVersion = try {
        subprocess(DistProperties.lldb, "-version")
                .takeIf { it.process.exitValue() == 0 }
                ?.stdout
    } catch (e: IOException) {
        null
    }

    if (lldbVersion == null) {
        println("No LLDB found")
    } else {
        println("Using $lldbVersion")
    }

    lldbVersion != null
}

private class LldbSessionSpecification private constructor(
        val commands: List<String>,
        val patterns: List<List<String>>
) {

    fun match(output: String) {
        val blocks = output.split("""(?=\(lldb\))""".toRegex()).filterNot(String::isEmpty)
        check(blocks[0].startsWith("(lldb) target create")) { "Missing block \"target create\". Got: ${blocks[0]}" }
        check(blocks[1].startsWith("(lldb) command script import")) {
            "Missing block \"command script import\". Got: ${blocks[0]}"
        }
        val responses = blocks.drop(2)
        val executedCommands = responses.map { it.lines().first() }
        val bodies = responses.map { it.lines().drop(1) }
        val responsesMatch = executedCommands.size == commands.size
                && commands.zip(executedCommands).all { (cmd, h) -> h == "(lldb) $cmd" }

        if (!responsesMatch) {
            val message = """
                |Responses do not match commands.
                |
                |COMMANDS: |$commands
                |RESPONSES: |$executedCommands
                |
                |FULL SESSION:
                |$output
            """.trimMargin()
            fail(message)
        }

        for ((patternBody, command) in patterns.zip(bodies).zip(executedCommands)) {
            val (pattern, body) = patternBody
            val mismatch = findMismatch(pattern, body)
            if (mismatch != null) {
                val message = """
                    |Wrong LLDB output.
                    |
                    |COMMAND: $command
                    |PATTERN: $mismatch
                    |OUTPUT:
                    |${body.joinToString("\n")}
                    |
                    |FULL SESSION:
                    |$output
                """.trimMargin()
                fail(message)
            }
        }
    }

    private fun findMismatch(patterns: List<String>, actualLines: List<String>): String? {
        val indices = mutableListOf<Int>()
        for (pattern in patterns) {
            val idx = actualLines.indexOfFirst { match(pattern, it) }
            if (idx == -1) {
                return pattern
            }
            indices += idx
        }
        check(indices == indices.sorted())
        return null
    }

    private fun match(pattern: String, line: String): Boolean {
        val chunks = pattern.split("""\s*\[\.\.]\s*""".toRegex())
                .filter { it.isNotBlank() }
                .map { it.trim() }
        check(chunks.isNotEmpty())
        val trimmedLine = line.trim()

        val indices = chunks.map { trimmedLine.indexOf(it) }
        if (indices.any { it == -1 } || indices != indices.sorted()) return false
        if (!(trimmedLine.startsWith(chunks.first()) || pattern.startsWith("[..]"))) return false
        if (!(trimmedLine.endsWith(chunks.last()) || pattern.endsWith("[..]"))) return false
        return true
    }

    companion object {
        fun parse(spec: String): LldbSessionSpecification {
            val blocks = spec.trimIndent()
                    .split("(?=^>)".toRegex(RegexOption.MULTILINE))
                    .filterNot(String::isEmpty)
            for (cmd in blocks) {
                check(cmd.startsWith(">")) { "Invalid lldb session specification: $cmd" }
            }
            val commands = blocks.map { it.lines().first().substring(1).trim() }
            val patterns = blocks.map { it.lines().drop(1).filter { it.isNotBlank() } }
            return LldbSessionSpecification(commands, patterns)
        }
    }
}
