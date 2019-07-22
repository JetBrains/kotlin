/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:UseExperimental(ExperimentalCli::class)
package org.jetbrains.kliopt

import kotlin.math.exp
import kotlin.test.*

class HelpTests {
    @Test
    fun testHelpMessage() {
        val argParser = ArgParser("test")
        val mainReport by argParser.argument(ArgType.String, description = "Main report for analysis")
        val compareToReport by argParser.argument(ArgType.String, description = "Report to compare to", required = false)

        val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
        val epsValue by argParser.option(ArgType.Double, "eps", "e", "Meaningful performance changes", 1.0)
        val useShortForm by argParser.option(ArgType.Boolean, "short", "s",
                "Show short version of report", defaultValue = false)
        val renders by argParser.options(ArgType.Choice(listOf("text", "html", "teamcity", "statistics", "metrics")),
                shortName = "r", description = "Renders for showing information", defaultValue = listOf("text"), multiple = true)
        val user by argParser.option(ArgType.String, shortName = "u", description = "User access information for authorization")
        argParser.parse(arrayOf("-h"))
        val helpOutput = argParser.makeUsage().trimIndent()
        val expectedOutput = """
            Usage: test options_list
Arguments: 
    mainReport -> Main report for analysis { String }
    compareToReport -> Report to compare to (optional) { String }
Options: 
    --output, -o -> Output file { String }
    --eps, -e [1.0] -> Meaningful performance changes { Double }
    --short, -s [false] -> Show short version of report 
    --renders, -r [text] -> Renders for showing information { Value should be one of [text, html, teamcity, statistics, metrics] }
    --user, -u -> User access information for authorization { String }
    --help, -h -> Usage info 
        """.trimIndent()
        assertEquals(helpOutput, expectedOutput)
    }

    @Test
    fun testHelpForSubcommands() {
        class Summary: Subcommand("summary") {
            val exec by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Execution time way of calculation", defaultValue = "geomean")
            val execSamples by options(ArgType.String, "exec-samples",
                    description = "Samples used for execution time metric (value 'all' allows use all samples)",
                    delimiter = ",")
            val execNormalize by option(ArgType.String, "exec-normalize",
                    description = "File with golden results which should be used for normalization")
            val compile by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Compile time way of calculation", defaultValue = "geomean")
            val compileSamples by options(ArgType.String, "compile-samples",
                    description = "Samples used for compile time metric (value 'all' allows use all samples)",
                    delimiter = ",")
            val compileNormalize by option(ArgType.String, "compile-normalize",
                    description = "File with golden results which should be used for normalization")
            val codesize by option(ArgType.Choice(listOf("samples", "geomean")),
                    description = "Code size way of calculation", defaultValue = "geomean")
            val codesizeSamples by options(ArgType.String, "codesize-samples",
                    description = "Samples used for code size metric (value 'all' allows use all samples)",
                    delimiter = ",")
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
        argParser.parse(arrayOf("summary", "-h"))
        val helpOutput = argParser.makeUsage().trimIndent()
        val expectedOutput = """
            Usage: test summary options_list
Arguments: 
    mainReport -> Main report for analysis { String }
    compareToReport -> Report to compare to (optional) { String }
Options: 
    --output, -o -> Output file { String }
    --eps, -e [1.0] -> Meaningful performance changes { Double }
    --short, -s [false] -> Show short version of report 
    --renders, -r [text] -> Renders for showing information { Value should be one of [text, html, teamcity, statistics, metrics] }
    --user, -u -> User access information for authorization { String }
    --help, -h -> Usage info 
        """.trimIndent()
        assertEquals(helpOutput, expectedOutput)
    }
}
