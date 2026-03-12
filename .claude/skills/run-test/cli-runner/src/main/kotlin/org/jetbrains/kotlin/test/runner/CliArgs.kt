package org.jetbrains.kotlin.test.runner

import kotlin.system.exitProcess

data class CliArgs(
    val projectRoot: String,
    val testDataPath: String,
    val dryRun: Boolean = false,
    val noBuild: Boolean = false,
    val verbose: Boolean = false,
    val gradleArgs: String? = null,
)

fun parseCliArgs(args: Array<String>): CliArgs {
    val parsed = RawParsedArgs()
    parsed.parseFrom(args)
    return parsed.toCliArgs()
}

private class RawParsedArgs {
    var projectRoot: String? = null
    var testDataPath: String? = null
    var dryRun = false
    var noBuild = false
    var verbose = false
    var gradleArgs: String? = null

    fun parseFrom(args: Array<String>) {
        var i = 0
        while (i < args.size) {
            i = processArg(args, i)
            i++
        }
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun processArg(
        args: Array<String>,
        index: Int,
    ): Int {
        var i = index
        when (args[i]) {
            "--help" -> {
                printUsage()
                throw HelpRequestedException()
            }

            "--project-root" -> {
                i++
                requireArgValue(args, i, "--project-root")
                projectRoot = args[i]
            }

            "--test-data-path" -> {
                i++
                requireArgValue(args, i, "--test-data-path")
                testDataPath = args[i]
            }

            "--dry-run" -> {
                dryRun = true
            }

            "--no-build" -> {
                noBuild = true
            }

            "--verbose" -> {
                verbose = true
            }

            "--gradle-args" -> {
                i++
                requireArgValue(args, i, "--gradle-args")
                gradleArgs = args[i]
            }

            else -> {
                System.err.println("Unknown argument: ${args[i]}")
                printUsage()
                exitWithError()
            }
        }
        return i
    }

    fun toCliArgs(): CliArgs {
        val root = projectRoot
        val path = testDataPath
        if (root == null || path == null) {
            if (root == null) System.err.println("Missing required argument: --project-root")
            if (path == null) System.err.println("Missing required argument: --test-data-path")
            printUsage()
            exitWithError()
        }
        return CliArgs(root, path, dryRun, noBuild, verbose, gradleArgs)
    }
}

class HelpRequestedException : RuntimeException("Help requested")

private fun requireArgValue(
    args: Array<String>,
    index: Int,
    name: String,
) {
    if (index >= args.size) {
        System.err.println("$name requires a value")
        printUsage()
        exitWithError()
    }
}

private fun exitWithError(): Nothing {
    exitProcess(1)
}

private fun printUsage() {
    println(
        """
        |Usage: kotlin-test-runner [options]
        |
        |Options:
        |  --project-root <path>   Path to the Kotlin project root (required)
        |  --test-data-path <path> Path to the test data file (required)
        |  --dry-run               Print what would be executed without running
        |  --no-build              Skip the Gradle build step
        |  --verbose               Enable verbose output
        |  --gradle-args <args>    Additional arguments to pass to Gradle
        |  --help                  Show this help message
        """.trimMargin(),
    )
}
