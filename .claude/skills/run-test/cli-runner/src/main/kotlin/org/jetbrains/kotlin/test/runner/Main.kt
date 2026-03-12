package org.jetbrains.kotlin.test.runner

import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cliArgs =
        try {
            parseCliArgs(args)
        } catch (_: HelpRequestedException) {
            return
        }

    val projectRoot = File(cliArgs.projectRoot).canonicalFile.toPath()
    val testDataPath = resolveTestDataPath(cliArgs.testDataPath, projectRoot)

    if (cliArgs.verbose) printConfig(cliArgs, projectRoot, testDataPath)

    buildTestClassesIfNeeded(cliArgs, projectRoot)
    val scannedClasses = scanAndValidate(projectRoot, cliArgs.verbose)
    val matchedTests = matchAndValidate(testDataPath, projectRoot, scannedClasses, cliArgs.verbose)

    printMatchedTests(matchedTests)

    val gradleCommand = buildGradleCommand(matchedTests, projectRoot, cliArgs.gradleArgs)
    executeOrDryRun(cliArgs, gradleCommand, projectRoot)
}

private fun printConfig(
    cliArgs: CliArgs,
    projectRoot: Path,
    testDataPath: Path,
) {
    println("[Main] Project root: $projectRoot")
    println("[Main] Test data path: $testDataPath")
    println("[Main] Dry run: ${cliArgs.dryRun}")
    println("[Main] No build: ${cliArgs.noBuild}")
    println("[Main] Gradle args: ${cliArgs.gradleArgs ?: "(none)"}")
}

private fun buildTestClassesIfNeeded(
    cliArgs: CliArgs,
    projectRoot: Path,
) {
    if (cliArgs.noBuild) {
        if (cliArgs.verbose) println("[Main] Skipping build (--no-build)")
        return
    }
    if (cliArgs.verbose) println("[Main] Building test classes...")
    val executor = GradleExecutor(projectRoot.toString(), cliArgs.verbose)
    val exitCode = executor.execute("testClasses")
    if (exitCode != 0) {
        System.err.println("Build failed with exit code $exitCode")
        exitProcess(exitCode)
    }
}

private fun scanAndValidate(
    projectRoot: Path,
    verbose: Boolean,
): Map<String, ScannedClass> {
    if (verbose) println("[Main] Scanning test classes...")
    val scannedClasses = scanTestClasses(projectRoot, verbose)
    if (scannedClasses.isEmpty()) {
        System.err.println("No test classes with @TestMetadata found under $projectRoot")
        exitProcess(1)
    }
    if (verbose) println("[Main] Found ${scannedClasses.size} annotated test classes")
    return scannedClasses
}

private fun matchAndValidate(
    testDataPath: Path,
    projectRoot: Path,
    scannedClasses: Map<String, ScannedClass>,
    verbose: Boolean,
): List<MatchedTest> {
    if (verbose) println("[Main] Matching test data path to tests...")
    val matchedTests = findMatchingTests(testDataPath, projectRoot, scannedClasses, verbose)
    if (matchedTests.isEmpty()) {
        System.err.println("No tests found matching: $testDataPath")
        exitProcess(1)
    }
    return matchedTests
}

private fun printMatchedTests(matchedTests: List<MatchedTest>) {
    println("Found ${matchedTests.size} matching test(s):")
    for (test in matchedTests) {
        val display = if (test.methodName != null) "${test.className}#${test.methodName}" else test.className
        println("  $display")
    }
}

private fun executeOrDryRun(
    cliArgs: CliArgs,
    gradleCommand: GradleCommand,
    projectRoot: Path,
) {
    if (cliArgs.dryRun) {
        println("\nDry run — would execute:")
        println("  ./gradlew ${gradleCommand.arguments.joinToString(" ")}")
        return
    }
    if (cliArgs.verbose) println("[Main] Executing tests...")
    val executor = GradleExecutor(projectRoot.toString(), cliArgs.verbose)
    val exitCode = executor.execute(*gradleCommand.arguments.toTypedArray())
    exitProcess(exitCode)
}

private fun resolveTestDataPath(
    raw: String,
    projectRoot: Path,
): Path {
    val file = File(raw)
    return if (file.isAbsolute) {
        file.canonicalFile.toPath()
    } else {
        projectRoot
            .resolve(raw)
            .toFile()
            .canonicalFile
            .toPath()
    }
}
