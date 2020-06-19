/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalCli::class)
package kotlinx.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlin.math.exp
import kotlin.test.*

class HelpTests {
    @Test
    fun testHelpMessage() {
        val argParser = ArgParser("test")
        val mainReport by argParser.argument(ArgType.String, description = "Main report for analysis")
        val compareToReport by argParser.argument(ArgType.String, description = "Report to compare to").optional()

        val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
        val epsValue by argParser.option(ArgType.Double, "eps", "e", "Meaningful performance changes").default(1.0)
        val useShortForm by argParser.option(ArgType.Boolean, "short", "s",
                "Show short version of report").default(false)
        val renders by argParser.option(ArgType.Choice(listOf("text", "html", "teamcity", "statistics", "metrics")),
                shortName = "r", description = "Renders for showing information").multiple().default(listOf("text"))
        val user by argParser.option(ArgType.String, shortName = "u", description = "User access information for authorization")
        argParser.parse(arrayOf("main.txt"))
        val helpOutput = argParser.makeUsage().trimIndent()
        @Suppress("CanBeVal") // can't be val in order to build expectedOutput only in run time.
        var epsDefault = 1.0
        val expectedOutput = """
Usage: test options_list
Arguments: 
    mainReport -> Main report for analysis { String }
    compareToReport -> Report to compare to (optional) { String }
Options: 
    --output, -o -> Output file { String }
    --eps, -e [$epsDefault] -> Meaningful performance changes { Double }
    --short, -s [false] -> Show short version of report 
    --renders, -r [text] -> Renders for showing information { Value should be one of [text, html, teamcity, statistics, metrics] }
    --user, -u -> User access information for authorization { String }
    --help, -h -> Usage info 
        """.trimIndent()
        assertEquals(expectedOutput, helpOutput)
    }

    @Test
    fun testHelpForSubcommands() {
        class Summary: Subcommand("summary", "Get summary information") {
            val exec by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Execution time way of calculation").default("geomean")
            val execSamples by option(ArgType.String, "exec-samples",
                    description = "Samples used for execution time metric (value 'all' allows use all samples)").delimiter(",")
            val execNormalize by option(ArgType.String, "exec-normalize",
                    description = "File with golden results which should be used for normalization")
            val compile by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Compile time way of calculation").default("geomean")
            val compileSamples by option(ArgType.String, "compile-samples",
                    description = "Samples used for compile time metric (value 'all' allows use all samples)").delimiter(",")
            val compileNormalize by option(ArgType.String, "compile-normalize",
                    description = "File with golden results which should be used for normalization")
            val codesize by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Code size way of calculation").default("geomean")
            val codesizeSamples by option(ArgType.String, "codesize-samples",
                    description = "Samples used for code size metric (value 'all' allows use all samples)").delimiter(",")
            val codesizeNormalize by option(ArgType.String, "codesize-normalize",
                    description = "File with golden results which should be used for normalization")
            val user by option(ArgType.String, shortName = "u", description = "User access information for authorization")
            val mainReport by argument(ArgType.String, description = "Main report for analysis")

            override fun execute() {
                println("Do some important things!")
            }
        }
        val action = Summary()
        // Parse args.
        val argParser = ArgParser("test")
        argParser.subcommands(action)
        argParser.parse(arrayOf("summary", "out.txt"))
        val helpOutput = action.makeUsage().trimIndent()
        val expectedOutput = """
Usage: test summary options_list
Arguments: 
    mainReport -> Main report for analysis { String }
Options: 
    --exec [geomean] -> Execution time way of calculation { Value should be one of [samples, geomean] }
    --exec-samples -> Samples used for execution time metric (value 'all' allows use all samples) { String }
    --exec-normalize -> File with golden results which should be used for normalization { String }
    --compile [geomean] -> Compile time way of calculation { Value should be one of [samples, geomean] }
    --compile-samples -> Samples used for compile time metric (value 'all' allows use all samples) { String }
    --compile-normalize -> File with golden results which should be used for normalization { String }
    --codesize [geomean] -> Code size way of calculation { Value should be one of [samples, geomean] }
    --codesize-samples -> Samples used for code size metric (value 'all' allows use all samples) { String }
    --codesize-normalize -> File with golden results which should be used for normalization { String }
    --user, -u -> User access information for authorization { String }
    --help, -h -> Usage info 
""".trimIndent()
        assertEquals(expectedOutput, helpOutput)
    }

    @Test
    fun testHelpMessageWithSubcommands() {
        abstract class CommonOptions(name: String, actionDescription: String): Subcommand(name, actionDescription) {
            val numbers by argument(ArgType.Int, "numbers", description = "Numbers").vararg()
        }
        class Summary: CommonOptions("summary", "Calculate summary") {
            val invert by option(ArgType.Boolean, "invert", "i", "Invert results")
            var result: Int = 0

            override fun execute() {
                result = numbers.sum()
                result = invert?.let { -1 * result } ?: result
            }
        }

        class Subtraction : CommonOptions("sub", "Calculate subtraction") {
            var result: Int = 0

            override fun execute() {
                result = numbers.map { -it }.sum()
            }
        }

        val summaryAction = Summary()
        val subtractionAction = Subtraction()
        val argParser = ArgParser("testParser")
        argParser.subcommands(summaryAction, subtractionAction)
        argParser.parse(emptyArray())
        val helpOutput = argParser.makeUsage().trimIndent()
        println(helpOutput)
        val expectedOutput = """
Usage: testParser options_list
Subcommands: 
    summary - Calculate summary
    sub - Calculate subtraction

Options: 
    --help, -h -> Usage info 
""".trimIndent()
        assertEquals(expectedOutput, helpOutput)
    }
}
