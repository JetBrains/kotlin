/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.*
import org.jetbrains.kotlin.analysis.test.data.manager.report.ModuleReport
import org.jetbrains.kotlin.analysis.test.data.manager.report.RunStatus
import org.jetbrains.kotlin.analysis.test.data.manager.report.TEST_DATA_MANAGER_JSON
import org.jetbrains.kotlin.analysis.test.data.manager.report.TestCounts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.*

private val ALL_MODULES = listOf(":mod-a", ":mod-a:nested", ":mod-b")

/** A module as the fake Gradle lists it, with its directory and its declared [testData], relative to the repository. */
private class Listed(
    val project: String,
    val directory: String = project.removePrefix(":").replace(':', '/'),
    val testData: List<String> = listOf("$directory/testData"),
)

private const val PREFIX = "-P$TEST_DATA_MANAGER_OPTIONS_PREFIX."

/**
 * What the fake Gradle does for a module, the way the runner does it: writes the report [report] makes of a passed one,
 * or [raw] as the report text, and prints its path.
 */
private class StubModule(val raw: String? = null, val report: (ModuleReport) -> ModuleReport = { it })

/**
 * A build of the fake Gradle: [modules] by project path, a module without an entry writes no report; [stray] also prints
 * a report of another run of that project, [output] is printed after the reports; [failed] overrides whether the build
 * failed, by default when a report did; [failures] are the failures of the build.
 */
private class Stub(
    val modules: Map<String, StubModule> = emptyMap(),
    val stray: String? = null,
    val output: String? = null,
    val failed: Boolean? = null,
    val failures: List<GradleFailure> = emptyList(),
)

private fun stubOf(vararg modules: String): Stub = Stub(modules.associateWith { StubModule() })

private fun tests(discovered: Int, passed: Int, failed: Int): TestCounts = TestCounts(discovered, passed, failed, skipped = 0)

/** The report of a run of [project] in which its one test passed. */
private fun passedReport(runId: String, project: String): ModuleReport = ModuleReport(
    schemaVersion = ModuleReport.SCHEMA_VERSION,
    runId = runId,
    project = project,
    mode = TestDataManagerMode.CHECK,
    status = RunStatus.PASSED,
    options = ModuleReport.Options(testDataPaths = null, testClassPattern = null, goldenOnly = false, incremental = false),
    runErrors = emptyList(),
    tests = tests(1, 1, 0),
    problems = emptyList(),
    testErrors = emptyList(),
    fileActions = emptyList(),
    recoveredTests = emptyList(),
)

private fun problem(
    kind: TestDataProblemKind,
    file: String,
    test: String,
    testDataPath: String? = null,
): ModuleReport.Problem = ModuleReport.Problem(
    kind = kind,
    file = file,
    comparedFile = null,
    actualFile = null,
    test = test,
    testDataPath = testDataPath,
    variantChain = emptyList(),
)

private fun testError(
    test: String,
    exception: String,
    message: String?,
    testDataPath: String? = null,
): ModuleReport.TestError = ModuleReport.TestError(
    test = test,
    testDataPath = testDataPath,
    variantChain = emptyList(),
    exception = exception,
    message = message,
    stackTrace = emptyList(),
)

private fun fileAction(
    kind: FileActionKind,
    file: String,
): ModuleReport.FileAction = ModuleReport.FileAction(
    kind = kind,
    file = file,
    testDataPath = file,
)

private fun ModuleReport.failed(
    problems: List<ModuleReport.Problem> = emptyList(),
    testErrors: List<ModuleReport.TestError> = emptyList(),
): ModuleReport = copy(
    status = RunStatus.FAILED,
    problems = problems,
    testErrors = testErrors,
)

private fun ModuleReport.withRunErrors(vararg runErrors: String): ModuleReport = copy(
    status = RunStatus.ERROR,
    runErrors = runErrors.toList(),
)

/** Tests of [TestDataManagerCli] in a temporary repository with the managed modules [ALL_MODULES] and a fake Gradle. */
class TestDataManagerCliTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private lateinit var root: Path
    private var stderr = ""

    private val gradle = FakeGradle()

    /**
     * When a Ctrl+C comes: `run`, as the run starts, before any build; `start`, while the [interruptedBuild] is being
     * started; or `build`, while the wrapper waits for it.
     */
    private var interruptAt: String? = null

    /** Which build of a run a Ctrl+C comes in: 1 lists the modules, 2 runs the tests. */
    private var interruptedBuild = 2

    /** What the wrapper does on a Ctrl+C at the moment. */
    private var onInterrupt: ((Int) -> Unit)? = null

    @BeforeEach
    fun setUp() {
        root = temporaryDirectory.resolve("repo").createDirectories().toRealPath()
    }

    private fun write(path: String, content: String): Path =
        root.resolve(path).also { it.createParentDirectories().writeText(content) }

    /** Delivers Ctrl+C presses to the wrapper while it starts the build, in the order of [interruptsAtStart]. */
    private var interruptsAtStart: List<Int> = emptyList()

    private fun cli(cwd: Path = root, out: PrintStream, err: PrintStream): TestDataManagerCli =
        TestDataManagerCli(root, cwd, gradle, FakeInterrupts(), out, err)

    /** Runs the wrapper with [args]; returns its exit code and output, keeps its stderr in [stderr]. */
    private fun run(stub: Stub, vararg args: String, cwd: Path = root): Pair<Int, String> {
        gradle.stub = stub
        gradle.builds = 0
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val exitCode = cli(cwd, PrintStream(out, true), PrintStream(err, true)).run(args.toList())
        // Windows separates the lines by CRLF
        stderr = err.toString().replace("\r\n", "\n")
        return exitCode to out.toString().replace("\r\n", "\n")
    }

    /** Runs the wrapper with [args]; returns its exit code and the JSON summary. */
    private fun runJson(stub: Stub, vararg args: String): Pair<Int, Summary> {
        val [exitCode, output] = run(stub, *args, "--json")
        return exitCode to readSummary(output)
    }

    private fun readSummary(text: String): Summary = TEST_DATA_MANAGER_JSON.decodeFromString(Summary.serializer(), text)

    /** The text output, with the run id and the file paths replaced. */
    private fun runText(stub: Stub, vararg args: String): Pair<Int, String> {
        val [exitCode, output] = run(stub, *args)
        return exitCode to output.replace(Regex("(Gradle log|Summary): .*"), "$1: <path>").replace(Regex("runId \\S+"), "runId <id>")
    }

    /** The repository with the modules the fake Gradle lists. */
    private fun repository(): Repository =
        Repository(root, gradle.listed.map { ManagedModule(it.project, it.directory, it.testData) }.sortedBy { it.project })

    /** The modules the `--path` values [paths] select, and the errors of the paths. */
    private fun select(vararg paths: String, cwd: String = ""): Pair<List<String>, List<String>> = repository().planRun(
        requestedModules = emptyList(),
        paths = paths.toList(),
        failed = null,
        cwd = root.resolve(cwd),
    ).let { it.modules to it.errors }

    private fun projects(summary: Summary, includeSkipped: Boolean = true): List<String> =
        summary.modules.filter { includeSkipped || it.status != ModuleStatus.SKIPPED }.map { it.project }

    /** The Gradle command line of the last build that ran the tests. */
    private fun gradleArgs(): List<String> = gradle.arguments!!

    private inner class FakeGradle : GradleLauncher {
        var stub = Stub()

        /** The modules [MANAGED_MODULE_TASK] prints. */
        var listed: List<Listed> = ALL_MODULES.map(::Listed)

        /** How [MANAGED_MODULE_TASK] fails, if it does. */
        var listingFailure: GradleFailure? = null

        /** What the build listing the modules prints after them. */
        var listingOutput: String? = null

        /** How many builds the run started. */
        var builds = 0

        /** The command line of the last build that listed the modules. */
        var listingArguments: List<String>? = null

        /** The command line of the last build that ran the tests. */
        var arguments: List<String>? = null
        var cancelled = false
        var abandoned = false

        override fun start(arguments: List<String>, output: OutputStream): RunningBuild {
            val index = ++builds
            if (index == interruptedBuild) {
                if (interruptAt == "start") onInterrupt!!(1)
                interruptsAtStart.forEach(onInterrupt!!)
            }

            val printer = PrintStream(output, true)
            if (MANAGED_MODULE_TASK in arguments) {
                listingArguments = arguments
                listingOutput?.let(printer::println)
                if (listingFailure == null) {
                    for (module in listed) {
                        val directories = listOf(module.directory) + module.testData
                        val fields = listOf(module.project) + directories.map { root.resolve(it).toString() }
                        printer.println(MANAGED_MODULE_PREFIX + fields.joinToString("\t"))
                    }
                }

                return build(GradleOutcome(failed = listingFailure != null, failures = listOfNotNull(listingFailure)), index)
            }

            this.arguments = arguments
            val tasks = arguments.filter { !it.startsWith("-") }
            val runId = arguments.single { it.startsWith("-P$TEST_DATA_MANAGER_OPTIONS_RUN_ID=") }.substringAfter('=')
            var failed = false
            for (task in tasks) {
                val project = task.substringBeforeLast(':')
                val entry = stub.modules[project] ?: continue
                val report = entry.report(passedReport(runId, project))
                failed = failed || report.status != RunStatus.PASSED
                val file = root.resolve(project.removePrefix(":").replace(':', '/')).resolve("build/test-data-manager/$runId.json")
                file.createParentDirectories().writeText(entry.raw ?: encode(report))
                printer.println("Test data manager report: $file")
            }

            stub.stray?.let { project ->
                val file = root.resolve("build/other.json").createParentDirectories()
                file.writeText(encode(passedReport("other-run", project)))
                printer.println("Test data manager report: $file")
            }

            stub.output?.let(printer::println)
            return build(GradleOutcome(failed = stub.failed ?: failed, failures = stub.failures), index)
        }

        private fun encode(report: ModuleReport): String = TEST_DATA_MANAGER_JSON.encodeToString(ModuleReport.serializer(), report)

        private fun build(outcome: GradleOutcome, index: Int): RunningBuild {
            return object : RunningBuild {
                override fun cancel() {
                    cancelled = true
                }

                override fun abandon() {
                    abandoned = true
                }

                override fun await(): GradleOutcome {
                    if (interruptAt == "build" && index == interruptedBuild) onInterrupt!!(1)
                    return outcome
                }
            }
        }
    }

    private inner class FakeInterrupts : Interrupts {
        override fun <T> handling(onInterrupt: (count: Int) -> Unit, block: () -> T): T {
            this@TestDataManagerCliTest.onInterrupt = onInterrupt
            if (interruptAt == "run") onInterrupt(1)
            return block()
        }
    }

    @Test
    fun `runs every managed module with the options`() {
        write("mod-a/nested/testData/file.kt", "")
        write("mod-b/testData/file.kt", "")
        val stub = stubOf(*ALL_MODULES.toTypedArray())
        val [exitCode, summary] = runJson(stub, "update", "--path", "mod-a,mod-b/testData/", "--incremental", "--verbose")
        assertEquals(0 to RunStatus.PASSED, exitCode to summary.status)
        assertEquals(tests(3, 3, 0), summary.tests)
        assertEquals(
            listOf(
                "-q", "--continue", ":mod-a:updateTestData", ":mod-a:nested:updateTestData", ":mod-b:updateTestData",
                "${PREFIX}testDataPath=mod-a,mod-b/testData/", "${PREFIX}incremental=true",
                "${PREFIX}runId=${summary.runId}", "${PREFIX}output=verbose",
            ),
            gradleArgs(),
        )

        // A repeated `--path` is the same as one with commas
        val commaArgs = gradleArgs().map { it.replace(summary.runId, "<id>") }
        val [_, repeated] = runJson(stub, "update", "--path", "mod-a", "--path", "mod-b/testData/", "--incremental", "--verbose")
        assertEquals(commaArgs, gradleArgs().map { it.replace(repeated.runId, "<id>") })
    }

    @Test
    fun `requested modules only`() {
        for (gradleArg in listOf(listOf("--gradle-arg=--offline"), listOf("--gradle-arg", "--offline"))) {
            val [exitCode, summary] = runJson(stubOf(":mod-b"), "check", "--module", ":mod-b", *gradleArg.toTypedArray())
            assertEquals(0 to listOf(":mod-b"), exitCode to projects(summary))
            assertEquals(listOf("-q", "--continue", ":mod-b:checkTestData"), gradleArgs().take(3))
            assertEquals("--offline", gradleArgs().last())
            // They may change what the build configures, so the modules are listed with them too
            assertEquals(listOf("-q", MANAGED_MODULE_TASK, "--offline"), gradle.listingArguments)
        }
    }

    @Test
    fun `requested modules are project paths`() {
        val [exitCode, summary] = runJson(
            stubOf(":mod-a:nested", ":mod-b"), "check", "--module", "mod-b", "--module", "mod-a/nested/", "--module", ":mod-b:",
            "--module", "mod-a:nested",
        )
        assertEquals(0 to RunStatus.PASSED, exitCode to summary.status)
        // Sorted, so the same modules in another order reuse the configuration cache entry of the build
        assertEquals(listOf(":mod-a:nested", ":mod-b"), projects(summary))
        assertEquals(listOf("-q", "--continue", ":mod-a:nested:checkTestData", ":mod-b:checkTestData"), gradleArgs().take(4))
        assertEquals(listOf(":mod-b", ":mod-a:nested", ":mod-b"), listOf("./mod-b", "mod-a\\nested", "::mod-b").map(::projectPath))

        for (invalid in listOf(":", "./")) {
            assertEquals(2, run(Stub(), "check", "--module", invalid).first, invalid)
            assertTrue("argument --module: '$invalid' is not a module path" in stderr, stderr)
        }
    }

    @Test
    fun `modules with custom project directories`() {
        gradle.listed += Listed(":custom-plugin", "plugins/custom")
        write("plugins/custom/testData/file.kt", "")
        val stub = stubOf(*(ALL_MODULES + ":custom-plugin").toTypedArray())
        val [exitCode, summary] = runJson(stub, "update", "--path", "plugins/custom/testData")
        assertEquals(0 to RunStatus.PASSED, exitCode to summary.status)
        assertTrue(":custom-plugin:updateTestData" in gradleArgs())
        assertEquals(
            ":custom-plugin" to "plugins/custom/build.gradle.kts",
            summary.modules.first().let { (project, buildFile) -> project to buildFile },
        )

        // A directory given as `--module` is the project in it
        val [directoryExitCode, directorySummary] = runJson(stub, "check", "--module", "plugins/custom")
        assertEquals(0 to listOf(":custom-plugin"), directoryExitCode to projects(directorySummary))
        assertEquals(listOf("-q", "--continue", ":custom-plugin:checkTestData"), gradleArgs().take(3))

        // Any spelling of the directory, and the project path itself, are the same module
        val [spellingsExitCode, spellingsSummary] = runJson(
            stub, "check", "--module", "plugins/other/../custom/", "--module", root.resolve("plugins/custom").toString(),
            "--module", ":custom-plugin",
        )
        assertEquals(0 to listOf(":custom-plugin"), spellingsExitCode to projects(spellingsSummary))
    }

    @Test
    fun `a failure to list the modules fails the run`() {
        val failure =
            GradleFailure(":mod-a:$MANAGED_MODULE_TASK", "Execution failed for task ':mod-a:$MANAGED_MODULE_TASK'.\n> No test-inputs-check")
        gradle.listingFailure = failure
        val [exitCode, summary] = runJson(stubOf(":mod-a"), "check")
        assertEquals(
            2 to listOf("Gradle failed to list the modules with managed test data, see the Gradle failures"),
            exitCode to summary.runErrors,
        )
        assertEquals(listOf(failure.text), summary.gradleFailures)
        assertEquals(emptyList<Any>(), summary.modules)
        assertNull(gradle.arguments)
    }

    @Test
    fun `a Ctrl+C while the modules are listed ends the run`() {
        interruptedBuild = 1
        for (moment in listOf("build", "start")) {
            interruptAt = moment
            gradle.cancelled = false
            val [exitCode, summary] = runJson(stubOf(":mod-a"), "check")
            assertEquals(Triple(130, RunStatus.ERROR, true), Triple(exitCode, summary.status, summary.interrupted), moment)
            assertTrue(gradle.cancelled, moment)
            assertNull(gradle.arguments, moment)
        }
    }

    @Test
    fun `a Ctrl+C while no build runs ends the run before the next one`() {
        interruptAt = "run"
        val [exitCode, summary] = runJson(stubOf(":mod-a"), "check")
        assertEquals(Triple(130, true, 0), Triple(exitCode, summary.interrupted, gradle.builds))
        assertEquals(listOf("Interrupted before Gradle finished; the modules without a report didn't finish"), summary.runErrors)
    }

    @Test
    fun `only the build of the tests writes reports`() {
        val report = root.resolve("mod-a/build/test-data-manager/fixed.json").createParentDirectories()
        report.writeText(TEST_DATA_MANAGER_JSON.encodeToString(ModuleReport.serializer(), passedReport("fixed", ":mod-a")))
        gradle.listingOutput = "Test data manager report: $report"
        val [exitCode, summary] = runJson(Stub(), "check", "--module", ":mod-a", "--run-id", "fixed")
        assertEquals(2 to listOf(ModuleStatus.MISSING), exitCode to summary.modules.map { it.status })
    }

    @Test
    fun `run id names the reports and the log`() {
        val [exitCode, summary] = runJson(stubOf(":mod-b"), "check", "--module", ":mod-b", "--run-id", "my-run.1")
        assertEquals(0 to "my-run.1", exitCode to summary.runId)
        assertEquals(root.resolve("build/test-data-manager/my-run.1.gradle.log").toString(), summary.gradleLog)
        assertEquals(root.resolve("build/test-data-manager/my-run.1.summary.json").toString(), summary.summary)
        assertEquals(root.resolve("mod-b/build/test-data-manager/my-run.1.json").toString(), summary.modules.single().report)

        for (invalid in listOf("..", "../x", "a b", "")) {
            assertEquals(2, run(Stub(), "check", "--run-id", invalid).first, invalid)
        }
    }

    @Test
    fun `the log and the summary are announced before gradle runs`() {
        val [_, summary] = runJson(stubOf(":mod-a", ":mod-b"), "check", "--module", ":mod-a", "--module", ":mod-b")
        assertEquals(
            "Gradle log: ${summary.gradleLog}\nSummary, written when the run ends: ${summary.summary}\n" +
                    "Running checkTestData in :mod-a, :mod-b\n",
            stderr,
        )
    }

    @Test
    fun `interrupted runs wait for gradle and keep the finished reports`() {
        // While the build is being started, the build is cancelled once it has started
        for (moment in listOf("build", "start")) {
            interruptAt = moment
            gradle.cancelled = false
            val stub = Stub(mapOf(":mod-a" to StubModule()), failed = true)
            val [exitCode, summary] = runJson(stub, "check", "--module", ":mod-a", "--module", ":mod-b")
            assertEquals(Triple(130, RunStatus.ERROR, true), Triple(exitCode, summary.status, summary.interrupted), moment)
            assertTrue(gradle.cancelled, moment)
            assertEquals(listOf("Interrupted before Gradle finished; the modules without a report didn't finish"), summary.runErrors)
            assertEquals(emptyList<Any>() to emptyList<Any>(), summary.gradleFailures to summary.compilerErrors)
            assertEquals(listOf("PASSED", "MISSING"), summary.modules.map { it.status.name })
            assertEquals(summary, readSummary(Path(summary.summary).readText()))
            assertTrue("Interrupted, waiting for Gradle to cancel the build" in stderr, stderr)
        }
    }

    @Test
    fun `a second Ctrl+C while the build starts stops waiting for it, whatever order the callbacks run in`() {
        interruptsAtStart = listOf(2, 1)
        val [exitCode, summary] = runJson(stubOf(":mod-a"), "check", "--module", ":mod-a")
        assertEquals(130 to true, exitCode to summary.interrupted)
        assertTrue(gradle.abandoned)
    }

    @Test
    fun `gradle failures and compiler errors are reported`() {
        val output = """
            e: file:///mod-b/src/Foo.kt:1:1 Unresolved reference 'x'.
            mod-b/src/Bar.java:3: error: cannot find symbol
        """.trimIndent()
        val reportFailure = GradleFailure(
            ":mod-a:checkTestData",
            "Execution failed for task ':mod-a:checkTestData'.\n> Process 'command 'java'' finished with non-zero exit value 1",
        )
        val compilationFailure = GradleFailure(
            ":mod-b:compileTestKotlin",
            "Execution failed for task ':mod-b:compileTestKotlin'.\n> A failure occurred while executing GradleKotlinCompilerWorkAction\n" +
                    "   > Compilation error. See log for more details",
        )
        val problem = problem(TestDataProblemKind.MISMATCH, "mod-a/testData/file.txt", "FooTestGenerated.testFile")
        val stub = Stub(
            modules = mapOf(":mod-a" to StubModule { it.failed(problems = listOf(problem)) }),
            output = output,
            failures = listOf(reportFailure, compilationFailure),
        )
        val [exitCode, summary] = runJson(stub, "check", "--module", ":mod-a", "--module", ":mod-b")
        assertEquals(2 to RunStatus.ERROR, exitCode to summary.status)
        assertEquals(listOf(compilationFailure.text), summary.gradleFailures)
        assertEquals(output.lines(), summary.compilerErrors)

        val [_, text] = runText(stub, "check", "--module", ":mod-a", "--module", ":mod-b")
        val expected = """
            ERROR No report from :mod-b, see the Gradle failures
            GRADLE FAILURE Execution failed for task ':mod-b:compileTestKotlin'.
              > A failure occurred while executing GradleKotlinCompilerWorkAction
                 > Compilation error. See log for more details
            e: file:///mod-b/src/Foo.kt:1:1 Unresolved reference 'x'.
            mod-b/src/Bar.java:3: error: cannot find symbol
            MISMATCH mod-a/testData/file.txt (FooTestGenerated.testFile)
        """.trimIndent()
        assertTrue(expected in text, text)

        // Only the failure of a task whose report failed: nothing to add
        val onlyReportFailure = Stub(modules = stub.modules, failures = listOf(reportFailure))
        val [onlyExitCode, onlySummary] = runJson(onlyReportFailure, "check", "--module", ":mod-a")
        assertEquals(
            Triple(1, emptyList<Any>(), emptyList<Any>()),
            Triple(onlyExitCode, onlySummary.gradleFailures, onlySummary.compilerErrors),
        )
    }

    @Test
    fun `many compiler errors are cut`() {
        val errors = (0 until 25).joinToString("\n") { "e: file:///Foo.kt:$it:1 Error" }
        assertEquals(listOf("e: file:///Foo.kt:19:1 Error", "... 5 more compiler errors"), compilerErrors(errors).drop(19))
    }

    @Test
    fun `problems errors and recovered tests are merged`() {
        val created = fileAction(FileActionKind.CREATED, "mod-a/nested/testData/x.txt")
        val stub = Stub(
            mapOf(
                ":mod-a" to StubModule { report ->
                    report.failed(
                        problems = listOf(problem(TestDataProblemKind.MISMATCH, "mod-a/testData/file.txt", "FooTestGenerated.testFile")),
                        testErrors = listOf(
                            testError(
                                test = "FooTestGenerated.testBroken",
                                exception = "java.lang.IllegalStateException",
                                message = "Broken\nsecond line",
                            ),
                        ),
                    ).copy(
                        recoveredTests = listOf(
                            ModuleReport.RecoveredTest(
                                test = "FooTestGenerated.testFlaky",
                                passedInPass = 2,
                                failures = listOf("java.lang.Error"),
                            ),
                        ),
                        tests = tests(3, 1, 2),
                    )
                },
                ":mod-a:nested" to StubModule { it.copy(fileActions = listOf(created)) },
                // Problems in a passed report still fail the run
                ":mod-b" to StubModule {
                    it.copy(problems = listOf(problem(TestDataProblemKind.MISSING_FILE, "mod-b/testData/y.txt", "BarTestGenerated.testY")))
                },
            ),
        )
        val [exitCode, summary] = runJson(stub, "check")
        assertEquals(1 to RunStatus.FAILED, exitCode to summary.status)
        assertEquals(tests(5, 3, 2), summary.tests)
        assertEquals(
            Summary.Counts(
                problems = mapOf(TestDataProblemKind.MISMATCH to 1, TestDataProblemKind.MISSING_FILE to 1),
                testErrors = 1,
                recoveredTests = 1,
                fileActions = mapOf(FileActionKind.CREATED to 1),
            ),
            summary.counts,
        )
        assertEquals(
            """
                Test data manager CHECK: FAILED, runId <id>
                5 tests: 3 passed, 2 failed, 0 skipped
                MISMATCH mod-a/testData/file.txt (FooTestGenerated.testFile)
                MISSING_FILE mod-b/testData/y.txt (BarTestGenerated.testY)
                TEST ERROR FooTestGenerated.testBroken: java.lang.IllegalStateException: Broken
                RECOVERED FooTestGenerated.testFlaky (passed in pass 2): java.lang.Error
                CREATED mod-a/nested/testData/x.txt
                Gradle log: <path>
                Summary: <path>

            """.trimIndent(),
            runText(stub, "check").second,
        )
    }

    @Test
    fun `text lists are capped`() {
        val problems =
            (0 until 55).map { problem(TestDataProblemKind.MISMATCH, "mod-a/testData/%02d.txt".format(it), "FooTestGenerated.test") }
        val stub = Stub(mapOf(":mod-a" to StubModule { it.failed(problems = problems) }))
        val [exitCode, output] = runText(stub, "check", "--module", ":mod-a")
        val lines = output.lines().dropLast(1)
        assertEquals(1, exitCode)
        assertEquals(
            listOf("MISMATCH mod-a/testData/49.txt (FooTestGenerated.test)", "... and 5 more, see the module reports"),
            lines.takeLast(4).take(2),
        )
        assertEquals(50, lines.count { it.startsWith("MISMATCH") })

        // A mass update: the files changed across the modules
        val actions = Stub(ALL_MODULES.associateWith { module ->
            val directory = module.removePrefix(":").replace(':', '/')
            val actions = (0 until 40).map { fileAction(FileActionKind.UPDATED, "$directory/testData/%02d.txt".format(it)) }
            StubModule { it.copy(fileActions = actions) }
        })
        val [updateExitCode, updateOutput] = runText(actions, "update")
        val updateLines = updateOutput.lines().dropLast(1)
        assertEquals(0, updateExitCode)
        assertEquals(50, updateLines.count { it.startsWith("UPDATED") })
        assertEquals(
            listOf("UPDATED mod-a/nested/testData/09.txt", "... and 70 more, see the module reports"),
            updateLines.takeLast(4).take(2),
        )

        // Run errors, one per path that doesn't exist; the summary keeps them all
        val paths = (0 until 60).joinToString(",") { "nope/%02d".format(it) }
        assertEquals(60, runJson(Stub(), "check", "--path", paths).second.runErrors.size)
        val errorLines = runText(Stub(), "check", "--path", paths).second.lines().dropLast(1)
        assertEquals(50, errorLines.count { it.startsWith("ERROR") })
        assertEquals(
            listOf(
                "ERROR testDataPath 'nope/49' does not exist; paths are relative to the repository root $root",
                "... and 10 more, see the summary",
            ),
            errorLines.takeLast(4).take(2),
        )
    }

    @Test
    fun `summary file is the json output and its path is the last line`() {
        val [_, summary] = runJson(stubOf(":mod-b"), "check", "--module", ":mod-b")
        val summaryFile = Path(summary.summary)
        assertEquals(root.resolve("build/test-data-manager/${summary.runId}.summary.json"), summaryFile)
        assertEquals(summary, readSummary(summaryFile.readText()))
        assertEquals(
            listOf(
                Summary.ModuleEntry(
                    project = ":mod-b",
                    status = ModuleStatus.PASSED,
                    tests = tests(1, 1, 0),
                    report = root.resolve("mod-b/build/test-data-manager/${summary.runId}.json").toString(),
                    buildFile = "mod-b/build.gradle.kts",
                ),
            ),
            summary.modules,
        )
        assertTrue(runText(stubOf(":mod-b"), "check", "--module", ":mod-b").second.endsWith("Summary: <path>\n"))
    }

    @Test
    fun `keeps the most recent runs`() {
        val directory = root.resolve("build/test-data-manager")
        fun touch(name: String, millis: Long) {
            write("build/test-data-manager/$name", "{}").setLastModifiedTime(FileTime.fromMillis(millis))
        }

        for (index in 0 until 6) {
            for (suffix in listOf(".summary.json", ".gradle.log")) touch("old-$index$suffix", index * 1000L)
        }

        // A run killed before its summary
        touch("killed.gradle.log", 0)
        // A run still going, with only a log yet: kept however many runs are newer
        touch("running.gradle.log", System.currentTimeMillis() - 60_000)
        val [_, summary] = runJson(stubOf(":mod-b"), "check", "--module", ":mod-b")
        assertEquals(
            ((2 until 6).flatMap { listOf("old-$it.gradle.log", "old-$it.summary.json") } +
                    listOf("${summary.runId}.gradle.log", "${summary.runId}.summary.json", "running.gradle.log")).sorted(),
            directory.listDirectoryEntries().map { it.name }.sorted(),
        )
    }

    @Test
    fun `rerun runs the test data files of the failed tests`() {
        for (file in listOf("mod-a/testData/file.kt", "mod-a/testData/sub/file.kt", "mod-b/testData/file.kt")) write(file, "")
        val failed = Stub(
            mapOf(
                ":mod-a" to StubModule { report ->
                    report.failed(
                        problems = listOf(
                            problem(TestDataProblemKind.MISMATCH, "mod-a/testData/file.txt", "A.testFile", "mod-a/testData/file.kt"),
                            problem(TestDataProblemKind.MISMATCH, "mod-a/testData/file.js.txt", "B.testFile", "mod-a/testData/file.kt"),
                        ),
                        testErrors = listOf(testError("A.testBroken", "E", message = null, testDataPath = "mod-a/testData/sub/file.kt")),
                    )
                },
                ":mod-a:nested" to StubModule(),
                ":mod-b" to StubModule(),
            ),
        )
        assertEquals(1, runJson(failed, "check", "--run-id", "first").first)

        val [exitCode, summary] = runJson(stubOf(":mod-a"), "update", "--rerun", "first")
        assertEquals(0 to listOf(":mod-a"), exitCode to projects(summary, includeSkipped = false))
        assertTrue("${PREFIX}testDataPath=mod-a/testData/file.kt,mod-a/testData/sub/file.kt" in gradleArgs(), gradleArgs().toString())
        assertTrue("Rerunning 2 test data files of the run first\n" in stderr, stderr)
        // The module reports of the run being rerun survive their rotation too
        assertTrue("${PREFIX}rerunId=first" in gradleArgs())
        assertNotEquals("first", summary.runId)

        // A test data file deleted since fails the run before Gradle
        root.resolve("mod-a/testData/sub/file.kt").deleteExisting()
        gradle.arguments = null
        val [deletedExitCode, deletedSummary] = runJson(stubOf(":mod-a"), "update", "--rerun", "first")
        assertEquals(2, deletedExitCode)
        assertTrue("testDataPath 'mod-a/testData/sub/file.kt' does not exist" in deletedSummary.runErrors.first())
        assertNull(gradle.arguments)

        // The run being rerun is kept however many runs follow
        repeat(KEPT_RUNS + 1) { runJson(stubOf(":mod-a"), "check", "--rerun", "first") }
        assertTrue(root.resolve("build/test-data-manager/first.summary.json").exists())
    }

    @Test
    fun `rerun fails when the failures are unknown`() {
        fun errors(stub: Stub): Pair<Int, Any?> {
            runJson(stub, "check", "--module", ":mod-a", "--module", ":mod-b", "--run-id", "first")
            val [exitCode, summary] = runJson(Stub(), "check", "--rerun", "first")
            return exitCode to summary.runErrors
        }

        assertEquals(2 to listOf("The run 'first' has no failed tests to rerun"), errors(stubOf(":mod-a", ":mod-b")))
        val conflicts = StubModule { it.withRunErrors("Variant chain conflicts").copy(tests = tests(0, 0, 0)) }
        assertEquals(
            2 to listOf(":mod-a: the run 'first' failed there with run errors, so its failures are unknown: Variant chain conflicts"),
            errors(Stub(mapOf(":mod-a" to conflicts, ":mod-b" to StubModule()))),
        )
        assertEquals(2 to listOf(":mod-b: no report in the run 'first', so its failures are unknown"), errors(stubOf(":mod-a")))
        val failedWithoutPath = StubModule { it.failed(testErrors = listOf(testError("A.testInit", "E", message = null))) }
        assertEquals(
            2 to listOf(":mod-a: A.testInit failed without a test data path, so it can't be rerun"),
            errors(Stub(mapOf(":mod-a" to failedWithoutPath, ":mod-b" to StubModule()))),
        )

        val [exitCode, summary] = runJson(Stub(), "check", "--rerun", "nope")
        assertEquals(2, exitCode)
        assertTrue(summary.runErrors.first().startsWith("No summary of the run 'nope' to rerun: "))

        assertEquals(2, run(Stub(), "check", "--rerun", "first", "--path", "mod-a").first)
    }

    @Test
    fun `no managed module is an error`() {
        gradle.listed = emptyList()
        val [exitCode, summary] = runJson(Stub(), "check")
        assertEquals(2 to listOf("No module applies the test-data-manager plugin in $root"), exitCode to summary.runErrors)
        assertNull(gradle.arguments)
    }

    @Test
    fun `module run errors fail the run`() {
        val invalid = StubModule { it.withRunErrors("Variant chain conflicts") }
        val [exitCode, summary] = runJson(Stub(ALL_MODULES.associateWith { invalid }), "check")
        assertEquals(2, exitCode)
        assertEquals(ALL_MODULES.map { "$it: Variant chain conflicts" }, summary.runErrors)
    }

    @Test
    fun `paths select the modules with test data in or under them`() {
        gradle.listed = listOf(
            Listed(":mod-a"),
            Listed(":mod-a:nested", testData = listOf("shared/testData/name.ext")),
            Listed(":mod-b", testData = listOf("shared/testData/b")),
        )
        for (file in listOf("mod-a/testData/nested/file.kt", "shared/testData/name.ext/file.kt", "shared/testData/b/file.kt")) {
            write(file, "")
        }

        assertEquals(listOf(":mod-a:nested", ":mod-b") to emptyList<String>(), select("shared/testData"))
        assertEquals(listOf(":mod-b") to emptyList<String>(), select("shared/testData/b/file.kt"))
        assertEquals(listOf(":mod-b") to emptyList<String>(), select("shared/testData/b/file"))
        assertEquals(listOf(":mod-a:nested") to emptyList<String>(), select("shared/testData/name"))
        assertEquals(listOf(":mod-a") to emptyList<String>(), select("mod-a/testData/nested/"))
        assertEquals(listOf(":mod-a", ":mod-b") to emptyList<String>(), select("mod-a/testData/nested,shared/testData/b"))
        assertEquals(ALL_MODULES to emptyList<String>(), select(root.toString()))

        val [exitCode, summary] = runJson(stubOf(":mod-b"), "check", "--path", "shared/testData/b")
        assertEquals(0 to RunStatus.PASSED, exitCode to summary.status)
        assertEquals(
            listOf(":mod-b" to "PASSED", ":mod-a" to "SKIPPED", ":mod-a:nested" to "SKIPPED"),
            summary.modules.map { (project, status) -> project to status.name },
        )
        assertEquals(listOf("-q", "--continue", ":mod-b:checkTestData"), gradleArgs().take(3))

        val [_, text] = runText(stubOf(":mod-b"), "check", "--path", "shared/testData/b")
        assertTrue("\nSkipped 2 modules without test data under the paths, see the summary\n" in text, text)

        // Explicit modules run as requested
        val [moduleExitCode, moduleSummary] = runJson(stubOf(":mod-a"), "check", "--path", "shared/testData/b", "--module", ":mod-a")
        assertEquals(0 to listOf(":mod-a"), moduleExitCode to projects(moduleSummary))

        // A skipped module has no report, even if its task ran as an extra Gradle argument
        val [_, extraSummary] = runJson(
            stubOf(":mod-a", ":mod-b"), "check", "--path", "shared/testData/b", "--gradle-arg", ":mod-a:checkTestData",
        )
        assertEquals(
            listOf(":mod-b" to true, ":mod-a" to false, ":mod-a:nested" to false),
            extraSummary.modules.map { (project, report) -> project to (report != null) },
        )
    }

    private fun unmatched(entry: String): String = unmatchedEntryError(entry)

    @Test
    fun `paths outside the test data of every module fail the run`() {
        gradle.listed = listOf(Listed(":mod-a"), Listed(":mod-a:nested", testData = listOf("mod-a/testData")), Listed(":mod-b"))
        for (file in listOf("mod-a/testData/file.kt", "mod-b/testData/file.kt", "shared/testData/file.kt", "file.kt")) write(file, "")

        assertEquals(emptyList<String>() to listOf(unmatched("file.kt")), select("file.kt"))

        // The runner, which would report it as matching none of its tests, doesn't get the entry
        val stub = Stub(mapOf(":mod-b" to StubModule { it.copy(unmatchedTestDataPaths = listOf("shared/")) }))
        val [exitCode, summary] = runJson(stub, "check", "--path", "mod-b/testData", "--path", "shared/")
        assertEquals(2 to listOf(unmatched("shared/")), exitCode to summary.runErrors)
        assertEquals(listOf("-q", "--continue", ":mod-b:checkTestData"), gradleArgs().take(3))
        assertTrue("${PREFIX}testDataPath=mod-b/testData" in gradleArgs(), gradleArgs().toString())

        gradle.arguments = null
        val [noneExitCode, noneSummary] = runJson(Stub(), "check", "--path", "shared/testData")
        assertEquals(2 to listOf(unmatched("shared/testData")), noneExitCode to noneSummary.runErrors)
        assertEquals(emptyList<Any>(), noneSummary.modules)
        assertNull(gradle.arguments)
    }

    @Test
    fun `paths that do not exist fail before gradle`() {
        write("mod-a/testData/file.kt", "")
        val [exitCode, summary] = runJson(Stub(), "check", "--path", "nope", "--path", "mod-a/testData/file")
        assertEquals(
            2 to listOf("testDataPath 'nope' does not exist; paths are relative to the repository root $root"),
            exitCode to summary.runErrors,
        )
        assertEquals(emptyList<Any>(), summary.modules)
        assertFalse("Skipped" in runText(Stub(), "check", "--path", "nope").second)
        assertNull(gradle.arguments)

        fun missing(path: String, suggestions: String = ""): String =
            "testDataPath '$path' does not exist; paths are relative to the repository root $root$suggestions"

        assertEquals(
            emptyList<String>() to listOf(missing("testData", ". Did you mean 'mod-a/testData'?")),
            select("testData", cwd = "mod-a"),
        )
        // Relative to a module, as the runner suggests for its own module
        write("mod-b/testData/other.kt", "")
        assertEquals(
            emptyList<String>() to listOf(missing("testData/other.kt", ". Did you mean 'mod-b/testData/other.kt'?")),
            select("testData/other.kt"),
        )
        assertEquals(
            emptyList<String>() to listOf(missing("testData/", ". Did you mean 'mod-a/testData' or 'mod-b/testData'?")),
            select("testData/"),
        )
        assertEquals(emptyList<String>() to listOf("testDataPath '../x' is outside the repository root $root"), select("../x"))
        assertEquals(emptyList<String>() to listOf(EMPTY_PATH_ERROR), select(" , "))
        // With explicit modules too, which skip the selection
        val [emptyExitCode, emptySummary] = runJson(stubOf(":mod-a"), "check", "--path", "", "--module", ":mod-a")
        assertEquals(2 to listOf(EMPTY_PATH_ERROR), emptyExitCode to emptySummary.runErrors)
        assertNull(gradle.arguments)
    }

    @Test
    fun `absolute paths through a symbolic link are inside`() {
        write("mod-a/testData/file.kt", "")
        // Windows creates links only with a privilege
        val link = try {
            Files.createSymbolicLink(temporaryDirectory.resolve("checkout"), root)
        } catch (e: IOException) {
            assumeTrue(false, "Symbolic links can't be created: $e")
            return
        }

        assertEquals(listOf(":mod-a") to emptyList<String>(), select(link.resolve("mod-a/testData/file.kt").toString()))
        // The runner, which doesn't resolve the link, gets the path relative to the repository
        for (module in listOf(listOf("--module", ":mod-a"), emptyList())) {
            runJson(stubOf(":mod-a"), "check", "--path", "$link/mod-a/testData/", *module.toTypedArray())
            assertTrue("${PREFIX}testDataPath=mod-a/testData/" in gradleArgs(), gradleArgs().toString())
        }

        // A file named without its extension doesn't exist itself
        assertEquals(listOf(":mod-a") to emptyList<String>(), select(link.resolve("mod-a/testData/file").toString()))
        // `..` after a link leaves the target of the link, the file exists or not
        val nestedLink = Files.createSymbolicLink(
            temporaryDirectory.resolve("nested"),
            root.resolve("mod-a/testData").createDirectories().resolve("nested").createDirectories(),
        )
        for (path in listOf("$nestedLink/../file.kt", "$nestedLink/../file")) {
            assertEquals(listOf(":mod-a") to emptyList<String>(), select(path), path)
        }
        // So is a module directory
        assertEquals(":mod-a:nested", repository().requestedProject(link.resolve("mod-a/nested").toString()))
        val outside = temporaryDirectory.resolve("file.kt").createFile().toRealPath()
        assertEquals(
            emptyList<String>() to listOf("testDataPath '$outside' is outside the repository root $root"),
            select(outside.toString()),
        )
    }

    @Test
    fun `an entry no test of the modules that ran is in fails the run, while the others run`() {
        for (file in listOf("mod-a/testData/file.kt", "mod-a/testData/file.txt", "mod-b/testData/file.kt")) write(file, "")
        val paths = "mod-a/testData/file.kt,mod-a/testData/file.txt,mod-b/testData/file.kt"
        val stub = Stub(
            mapOf(
                ":mod-a" to StubModule { it.copy(unmatchedTestDataPaths = listOf("mod-a/testData/file.txt", "mod-b/testData/file.kt")) },
                // An entry another module has tests of is fine
                ":mod-b" to StubModule { it.copy(unmatchedTestDataPaths = listOf("mod-a/testData/file.kt", "mod-a/testData/file.txt")) },
            )
        )
        val [exitCode, summary] = runJson(stub, "check", "--path", paths)
        assertEquals(2 to listOf(unmatchedPathError("mod-a/testData/file.txt")), exitCode to summary.runErrors)
        assertEquals(listOf(":mod-a", ":mod-b"), projects(summary, includeSkipped = false))

        // A module without a report may have had the tests of the entry
        val [missingExitCode, missingSummary] = runJson(Stub(mapOf(":mod-a" to stub.modules.getValue(":mod-a"))), "check", "--path", paths)
        assertEquals(
            2 to listOf(":mod-b: no report for this run; the task didn't run or its JVM crashed, see the Gradle log"),
            missingExitCode to missingSummary.runErrors,
        )
    }

    @Test
    fun `new test data files and output files select the module of their directory`() {
        // The runner, which knows its tests, reports that none matches
        for (file in listOf("mod-a/testData/new.kt", "mod-a/testData/file.txt", "mod-a/testData/file.knm.txt")) {
            write(file, "")
            assertEquals(listOf(":mod-a") to emptyList<String>(), select(file), file)
        }
    }

    @Test
    fun `missing unreadable and stray reports`() {
        val [exitCode, summary] = runJson(
            Stub(
                mapOf(":mod-a" to StubModule(), ":mod-a:nested" to StubModule(raw = "{\"runId\": \"cut")),
                stray = ":mod-b",
            ),
            "check",
        )
        assertEquals(2 to RunStatus.ERROR, exitCode to summary.status)
        assertEquals(listOf("PASSED", "MISSING", "MISSING"), summary.modules.map { it.status.name })
        val missing = listOf(":mod-a:nested", ":mod-b")
        assertEquals(
            missing.map { "$it: no report for this run; the task didn't run or its JVM crashed, see the Gradle log" },
            summary.runErrors,
        )
    }

    @Test
    fun `filters matching nothing are an error`() {
        val empty = StubModule { it.copy(tests = tests(0, 0, 0)) }
        val stub = Stub(ALL_MODULES.associateWith { empty })
        val hint = "A module finds its managed tests only in the packages of `testDataManager { testPackages }` (`org.jetbrains.kotlin` " +
                "by default): if the tests exist, check their packages there, in "
        val [exitCode, summary] = runJson(stub, "check", "--class-pattern", ".*Nope.*")
        assertEquals(
            2 to listOf(
                "No managed test in any module matches the filters. " + hint +
                        "mod-a/build.gradle.kts, mod-a/nested/build.gradle.kts, mod-b/build.gradle.kts",
            ),
            exitCode to summary.runErrors,
        )

        // With `--path`, only the modules that ran are named, and the entries no test is in are named as well
        write("mod-b/testData/file.txt", "")
        val unmatched = StubModule { it.copy(tests = tests(0, 0, 0), unmatchedTestDataPaths = listOf("mod-b/testData/file.txt")) }
        val [pathExitCode, pathSummary] = runJson(Stub(mapOf(":mod-b" to unmatched)), "check", "--path", "mod-b/testData/file.txt")
        assertEquals(
            2 to listOf(
                unmatchedPathError("mod-b/testData/file.txt"),
                "No managed test in any module matches the filters. " + hint + "mod-b/build.gradle.kts",
            ),
            pathExitCode to pathSummary.runErrors,
        )
        assertEquals(
            listOf(
                ":mod-b" to "mod-b/build.gradle.kts",
                ":mod-a" to "mod-a/build.gradle.kts",
                ":mod-a:nested" to "mod-a/nested/build.gradle.kts",
            ),
            pathSummary.modules.map { (project, buildFile) -> project to buildFile },
        )

        // Too many modules to name: the summary has the build file of each
        assertEquals(
            "No managed test in any module matches the filters. " + hint +
                    "the build file of each module that ran (`buildFile` of the modules in the summary)",
            noMatchError((0..MAX_NAMED_BUILD_FILES).map { "mod-$it/build.gradle.kts" }),
        )

        val [noFilterExitCode, noFilterSummary] = runJson(stub, "check")
        assertEquals(0 to emptyList<Any>(), noFilterExitCode to noFilterSummary.runErrors)
    }

    @Test
    fun `gradle failure without a failing report is an error`() {
        val [exitCode, summary] = runJson(Stub(ALL_MODULES.associateWith { StubModule() }, failed = true), "check")
        assertEquals(2, exitCode)
        assertEquals(listOf("Gradle failed without a failing report, see the Gradle log"), summary.runErrors)
    }

    @Test
    fun `gradle output goes to the log`() {
        val [_, summary] = runJson(stubOf(":mod-b"), "check", "--module", ":mod-b", "--class-pattern", ".*(Foo|Bar).*")
        val log = Path(summary.gradleLog!!).readText()
        val gradlew = shellQuote(root.resolve("gradlew").toString())
        assertTrue(log.startsWith("$ $gradlew -q $MANAGED_MODULE_TASK\n$MANAGED_MODULE_PREFIX:mod-a\t"), log)
        assertTrue("\n$ $gradlew -q --continue :mod-b:checkTestData " in log, log)
        // Quoted for a shell, as the command can be repeated by hand
        assertTrue(" '${PREFIX}testClassPattern=.*(Foo|Bar).*' ${PREFIX}runId=${summary.runId}\n" in log, log)
        assertTrue("Test data manager report: " in log, log)
    }

    @Test
    fun `usage errors and help`() {
        for ([args, error] in listOf(
            listOf<String>() to "the mode is required: check or update",
            listOf("verify") to "invalid mode 'verify': choose from 'check', 'update'",
            listOf("check", "--nope") to "unrecognized argument: --nope",
            listOf("check", "--path") to "argument --path: expected one argument",
            listOf("check", "--json=yes") to "argument --json: takes no value, got 'yes'",
        )) {
            assertEquals(2, run(Stub(), *args.toTypedArray()).first, args.toString())
            assertTrue(stderr.startsWith("usage: test-data-manager ") && stderr.endsWith("test-data-manager: error: $error\n"), stderr)
        }

        val [exitCode, output] = run(Stub(), "--help")
        assertEquals(0 to USAGE, exitCode to output)
    }
}
