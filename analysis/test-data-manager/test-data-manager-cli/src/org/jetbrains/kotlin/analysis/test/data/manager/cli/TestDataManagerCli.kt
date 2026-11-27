/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.TEST_DATA_MANAGER_PREFIX
import org.jetbrains.kotlin.analysis.test.data.manager.report.RunStatus
import org.jetbrains.kotlin.analysis.test.data.manager.report.TEST_DATA_MANAGER_JSON
import java.io.PrintStream
import java.nio.CharBuffer
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.system.exitProcess
import kotlin.uuid.Uuid

internal const val EXIT_PASSED = 0
internal const val EXIT_FAILED = 1
internal const val EXIT_ERROR = 2
internal const val EXIT_INTERRUPTED = 130

/** The system property the launcher passes the repository root in. */
internal const val REPOSITORY_ROOT_PROPERTY = "$TEST_DATA_MANAGER_PREFIX.cli.repositoryRoot"

/**
 * A wrapper over `checkTestData` and `updateTestData` that aggregates the module reports of one run.
 *
 * It runs the task in the modules with managed test data in one Gradle build, keeps the Gradle output in a log, and reads
 * the report every module prints the path of. The options are passed to the runner as is: the runner validates them.
 * On top of the module reports, it reports what a single module can't see: a module that wrote no report (the task didn't
 * run, or its JVM crashed) and filters that match no test in any module.
 *
 * The summary goes to `build/test-data-manager/<runId>.summary.json`, whose path is the last line of the output: an index
 * of the run (status, counts, the module reports), while the problems and file changes themselves stay in the module
 * reports. The text output lists at most the first [LIST_LIMIT] entries of each kind.
 *
 * The modules come from Gradle, [MANAGED_MODULE_TASK], with the test data each of them declares. With `--path`, only the
 * modules whose test data is in or under the paths run, so the others aren't even built; see [planRun]. A path that
 * doesn't exist fails the run before the tests are built, and so does one outside the test data of every module.
 *
 * The paths of the Gradle log and of the summary are printed to [err] before Gradle starts, so a long run can be followed;
 * `--run-id` names them and the module reports in advance. The summary is written at once when the run ends, interrupted
 * runs included, so it is never read half-written. When Gradle fails for another reason than a failing module report,
 * the summary lists its failures and the compiler errors from the log.
 *
 * @param root the repository root, a real path
 * @param cwd the directory the wrapper is started in, which `--path` and `--module` may be relative to in suggestions
 */
internal class TestDataManagerCli(
    private val root: Path,
    private val cwd: Path,
    private val gradle: GradleLauncher,
    private val interrupts: Interrupts,
    private val out: PrintStream,
    private val err: PrintStream,
) {
    /** Runs the wrapper with [args]; returns the exit code. */
    fun run(args: List<String>): Int {
        val arguments = try {
            parseArguments(args)
        } catch (_: HelpRequestedException) {
            out.print(USAGE)
            return EXIT_PASSED
        } catch (e: UsageException) {
            err.print(SHORT_USAGE)
            err.println("test-data-manager: error: ${e.message}")
            return EXIT_ERROR
        }

        val runId = arguments[Option.RunId] ?: Uuid.generateV7().toString()
        val directory = RunDirectory(root.resolve("build/test-data-manager").createDirectories())
        val gradleLog = directory.gradleLog(runId)
        err.println("Gradle log: $gradleLog")
        err.println("Summary, written when the run ends: ${directory.summaryFile(runId)}")
        err.flush()

        val [execution, interrupted] = gradleLog.outputStream().use { log ->
            val builds = InterruptibleBuilds(gradle, root, log, err)
            val execution = interrupts.handling(onInterrupt = builds::interrupt) { execute(arguments, runId, directory, builds) }
            execution to builds.interrupted
        }

        val plan = execution.plan
        val gradleOutput = gradleLog.readText()
        val result = RunResult(
            mode = arguments.mode,
            runId = runId,
            plan = plan,
            hasFilters = plan.testDataPaths.isNotEmpty() || arguments[Option.ClassPattern] != null,
            gradle = execution.outcome,
            gradleOutput = gradleOutput,
            interrupted = interrupted,
            reports = execution.testsCommandLine?.let { reportsOfTests(gradleOutput, it, runId) }.orEmpty(),
            buildFiles = (plan.modules + plan.skipped).associateWith(execution.repository::buildFile),
            gradleLog = gradleLog,
            summaryFile = directory.summaryFile(runId),
        )

        val summary = summarize(result)
        val summaryText = TEST_DATA_MANAGER_JSON.encodeToString(Summary.serializer(), summary)
        directory.writeSummary(runId, summaryText + "\n")
        directory.deleteOldRuns(keptRun = arguments[Option.Rerun])

        if (Option.Json in arguments) out.println(summaryText) else out.printSummary(summary, result.merged)
        out.flush()

        if (interrupted) return EXIT_INTERRUPTED
        return when (summary.status) {
            RunStatus.PASSED -> EXIT_PASSED
            RunStatus.FAILED -> EXIT_FAILED
            RunStatus.ERROR -> EXIT_ERROR
        }
    }

    /**
     * What the builds of a run did.
     *
     * @property repository the repository with the modules Gradle listed, without modules if it didn't
     * @property outcome how the last build ended, if one was started and waited for: the build of the tests, or the build
     * listing the modules if it failed
     * @property testsCommandLine the command line of the build of the tests in the log, if it ran: its output follows
     */
    private class Execution(
        val repository: Repository,
        val plan: RunPlan = RunPlan(),
        val outcome: GradleOutcome? = null,
        val testsCommandLine: String? = null,
    )

    /** Lists the managed modules, plans the run with them, and runs the tests of its modules, if any; stops on a Ctrl+C. */
    private fun execute(arguments: Arguments, runId: String, directory: RunDirectory, builds: InterruptibleBuilds): Execution {
        val listing = builds.run(arguments.listingBuild())
        val unlisted = Repository(root, modules = emptyList())
        if (builds.interrupted) return Execution(unlisted)
        if (listing == null || listing.failed) {
            return Execution(
                repository = unlisted,
                plan = RunPlan(errors = listOf("Gradle failed to list the modules with managed test data, see the Gradle failures")),
                outcome = listing,
            )
        }

        val repository = Repository(root, parseManagedModules(directory.gradleLog(runId).readText(), root))
        val failed = arguments[Option.Rerun]?.let { rerun ->
            directory.failedTests(rerun).also {
                val count = it.testDataPaths.size
                err.println("Rerunning $count test data file${if (count == 1) "" else "s"} of the run $rerun")
            }
        }

        val plan = repository.planRun(
            requestedModules = arguments.values(Option.Module),
            paths = arguments.values(Option.Path),
            failed = failed,
            cwd = cwd,
        )

        if (plan.modules.isEmpty() || builds.interrupted) return Execution(repository, plan)
        err.println("Running ${arguments.mode.taskName} in ${plan.modules.joinToString(", ")}")
        err.flush()
        val testsBuild = arguments.testsBuild(plan.modules, plan.testDataPaths, runId)
        return Execution(repository, plan, outcome = builds.run(testsBuild), testsCommandLine = commandLine(root, testsBuild))
    }

    /**
     * The reports of the run [runId]. Only the build of the tests writes them: they are read from its [commandLine] in
     * [gradleOutput] on, which has the run id the build listing the modules doesn't get.
     */
    private fun reportsOfTests(gradleOutput: String, commandLine: String, runId: String): Map<String, LoadedReport> {
        val start = gradleOutput.indexOf(commandLine).takeIf { it >= 0 } ?: return emptyMap()
        return loadReports(CharBuffer.wrap(gradleOutput, start, gradleOutput.length), runId)
    }
}

fun main(args: Array<String>) {
    // The Tooling API logs through SLF4J, which would warn on each run that it has no logger to log to
    System.setProperty("slf4j.internal.verbosity", "ERROR")
    val exitCode = try {
        val root = Path(
            System.getProperty(REPOSITORY_ROOT_PROPERTY) ?: error("The launcher must set $REPOSITORY_ROOT_PROPERTY")
        ).toRealPath()

        SignalInterrupts.install()
        TestDataManagerCli(
            root = root,
            cwd = Path("").toAbsolutePath(),
            gradle = ToolingApiGradleLauncher(root),
            interrupts = SignalInterrupts,
            out = System.out,
            err = System.err,
        ).run(args.asList())
    } catch (e: Exception) {
        // An unexpected failure is a failed run, not test data problems
        e.printStackTrace()
        System.err.println("ERROR the test data manager failed: ${e.message}")
        EXIT_ERROR
    }

    System.out.flush()
    exitProcess(exitCode)
}
