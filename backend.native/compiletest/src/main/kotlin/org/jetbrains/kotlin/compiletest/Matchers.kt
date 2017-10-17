package org.jetbrains.kotlin.compiletest

import org.intellij.lang.annotations.Language
import java.io.IOException
import java.nio.file.Files


fun multilineMatch(patterns: List<String>, actualLines: List<String>) {
    fun indexOfLine(line: String): Int {
        val chunks = line.split("""\[\.\.]""".toRegex()).filter { it.isNotBlank() }

        for ((i, actual) in actualLines.withIndex()) {
            val indices = chunks.map { actual.indexOf(it) }
            if (indices.all { it != -1 }) {
                check(indices == indices.sorted())
                return i
            }
        }

        error("Can't find match for `$line` in\n${actualLines.joinToString("\n")}")
    }

    val indices = patterns.map { indexOfLine(it) }
    check(indices == indices.sorted())
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

fun lldbTest(@Language("kotlin") program: String, lldbSession: String) {
    if (!haveLldb) {
        println("Skipping test: no LLDB")
        return
    }

    val lldbSessionSpec = LldbSessionSpecification.parse(lldbSession)

    val tmpdir = Files.createTempDirectory("debugger_test")
    tmpdir.toFile().deleteOnExit()
    val source = tmpdir.resolve("main.kt")
    val output = tmpdir.resolve("program.kexe")

    val driver = ToolDriver(DistProperties.konanc, DistProperties.lldb)
    Files.write(source, program.trimIndent().toByteArray())
    driver.compile(source, output, "-g")
    val result = driver.runLldb(output, lldbSessionSpec.commands)
    lldbSessionSpec.match(result)
}

private class LldbSessionSpecification private constructor(
        val commands: List<String>,
        val patterns: List<List<String>>
) {

    fun match(output: String) {
        val blocks = output.split("""(?=\(lldb\))""".toRegex())
        check(blocks.first().startsWith("(lldb) target create"))
        val responses = blocks.drop(1)
        val headers = responses.map { it.lines().first() }
        val bodies = responses.map { it.lines().drop(1) }
        val responsesMatch = headers.size == commands.size
                && commands.zip(headers).all { (cmd, h) -> h == "(lldb) $cmd" }

        check(responsesMatch) {
            "Responses do not match commands.\nResponses: $headers\nCommands: $commands"
        }

        for ((pattern, body) in patterns.zip(bodies)) {
            multilineMatch(pattern, body)
        }
    }

    companion object {
        fun parse(spec: String): LldbSessionSpecification {
            val blocks = spec.trimIndent().split("(?=^>)".toRegex(RegexOption.MULTILINE))
            for (cmd in blocks) {
                check(cmd.startsWith(">")) { "Invalid lldb session specification" }
            }
            val commands = blocks.map { it.lines().first().substring(1).trim() }
            val patterns = blocks.map { it.lines().drop(1).filter { it.isNotBlank() } }
            return LldbSessionSpecification(commands, patterns)
        }
    }
}
