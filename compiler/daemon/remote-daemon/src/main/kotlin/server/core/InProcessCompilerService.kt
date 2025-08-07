/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.SERVER_COMPILATION_WORKSPACE_DIR
import org.jetbrains.kotlin.build.report.RemoteBuildReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.EventManager
import org.jetbrains.kotlin.daemon.EventManagerImpl
import org.jetbrains.kotlin.daemon.common.BuildMetricsValue
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationPerformanceMetrics
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.WallAndThreadAndMemoryTotalProfiler
import org.jetbrains.kotlin.daemon.common.usedMemory
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.incremental.withIncrementalCompilation
import org.jetbrains.kotlin.incremental.withJsIC
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
import org.jetbrains.kotlin.util.getLinesPerSecond
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class InProcessCompilerService(
    var reportPerf: Boolean = false
) {

    init {
        Files.createDirectories(File(SERVER_COMPILATION_WORKSPACE_DIR).toPath())
    }

    companion object {

//        fun buildCompilerArgs(sourceFiles: List<File>, outputDirectory: Path, additionalArguments: List<String>): List<String> {
//            return buildCompilerArgsWithoutSourceFiles(outputDirectory, additionalArguments).toMutableList().apply {
//                addAll(0, sourceFiles.map { it.path })
//            }
//        }

        fun buildCompilerArgsWithoutSourceFiles(outputDirectory: Path, additionalArguments: List<String>): List<String> {
            return mutableListOf<String>().apply {
                add("-d")
                add(outputDirectory.toString())
                add("-cp")
                add("/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar") // TODO fix
                addAll(additionalArguments)
            }
        }

    }

    private val log by lazy { Logger.getLogger("compiler") }

    private fun getPerformanceMetrics(compiler: CLICompiler<CommonCompilerArguments>): List<BuildMetricsValue> {
        val performanceMetrics = ArrayList<BuildMetricsValue>()
        val performanceManager = compiler.defaultPerformanceManager
        val moduleStats = performanceManager.unitStats
        if (moduleStats.linesCount > 0) {
            performanceMetrics.add(BuildMetricsValue(CompilationPerformanceMetrics.SOURCE_LINES_NUMBER, moduleStats.linesCount.toLong()))
        }

        var codegenTime = Time.Companion.ZERO

        fun reportLps(lpsMetrics: CompilationPerformanceMetrics, time: Time) {
            if (time != Time.Companion.ZERO) {
                performanceMetrics.add(BuildMetricsValue(lpsMetrics, moduleStats.getLinesPerSecond(time).toLong()))
            }
        }

        moduleStats.forEachPhaseMeasurement { phaseType, time ->
            if (time == null) return@forEachPhaseMeasurement

            val metrics = when (phaseType) {
                PhaseType.Initialization -> CompilationPerformanceMetrics.COMPILER_INITIALIZATION
                PhaseType.Analysis -> CompilationPerformanceMetrics.CODE_ANALYSIS
                // TODO: Report `IrGeneration` (FIR2IR) time
                PhaseType.IrLowering -> {
                    codegenTime += time
                    null
                }
                PhaseType.Backend -> {
                    codegenTime += time
                    null
                }
                else -> null
            }
            if (metrics != null) {
                performanceMetrics.add(BuildMetricsValue(metrics, time.millis))
                if (phaseType == PhaseType.Analysis) {
                    reportLps(CompilationPerformanceMetrics.ANALYSIS_LPS, time)
                }
            }
        }

        if (codegenTime != Time.Companion.ZERO) {
            performanceMetrics.add(BuildMetricsValue(CompilationPerformanceMetrics.CODE_GENERATION, codegenTime.millis))
            reportLps(CompilationPerformanceMetrics.CODE_GENERATION_LPS, codegenTime)
        }

        return performanceMetrics
    }

    fun <ServicesFacadeT, JpsServicesFacadeT, CompilationResultsT> compileImpl(
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: ServicesFacadeT,
        compilationResults: CompilationResultsT,
        hasIncrementalCaches: JpsServicesFacadeT.() -> Boolean,
        createMessageCollector: (ServicesFacadeT, CompilationOptions) -> MessageCollector,
        createReporter: (ServicesFacadeT, CompilationOptions) -> DaemonMessageReporter,
        createServices: (JpsServicesFacadeT, EventManager) -> Services,
        getICReporter: (ServicesFacadeT, CompilationResultsT?, IncrementalCompilationOptions) -> RemoteBuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    ): Int = run {
        val messageCollector = createMessageCollector(servicesFacade, compilationOptions)
        val daemonReporter = createReporter(servicesFacade, compilationOptions)
        val targetPlatform = compilationOptions.targetPlatform
        println("Starting compilation with args: " + compilerArguments.joinToString(" "))

        @Suppress("UNCHECKED_CAST")
        val compiler = when (targetPlatform) {
            CompileService.TargetPlatform.JVM -> K2JVMCompiler()
            CompileService.TargetPlatform.JS -> K2JSCompiler()
            CompileService.TargetPlatform.METADATA -> KotlinMetadataCompiler()
        } as CLICompiler<CommonCompilerArguments>

        val k2PlatformArgs = compiler.createArguments()
        parseCommandLineArguments(compilerArguments.asList(), k2PlatformArgs)
        val argumentParseError = validateArguments(k2PlatformArgs.errors)

        if (argumentParseError != null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, argumentParseError)
            ExitCode.COMPILATION_ERROR.code
        } else when (compilationOptions.compilerMode) {
            CompilerMode.JPS_COMPILER -> {
                @Suppress("UNCHECKED_CAST")
                servicesFacade as JpsServicesFacadeT
                withIncrementalCompilation(k2PlatformArgs, enabled = servicesFacade.hasIncrementalCaches()) {
                    doCompile(daemonReporter) { eventManger ->
                        val services = createServices(servicesFacade, eventManger)
                        val exitCode = compiler.exec(messageCollector, services, k2PlatformArgs)

                        compilationResults.also {
                            val compilationResult = it as CompilationResults
                            getPerformanceMetrics(compiler).forEach {
                                compilationResult.add(CompilationResultCategory.BUILD_METRICS.code, it)
                            }
                        }

                        exitCode
                    }
                }.get()
            }
            CompilerMode.NON_INCREMENTAL_COMPILER -> {
                doCompile(daemonReporter) { _ ->
                    val exitCode = compiler.exec(messageCollector, Services.Companion.EMPTY, k2PlatformArgs)

                    val perfString = compiler.defaultPerformanceManager.createPerformanceReport(dumpFormat = PerformanceManager.DumpFormat.PlainText)
                    compilationResults?.also {
                        (it as CompilationResults).add(
                            CompilationResultCategory.BUILD_REPORT_LINES.code,
                            arrayListOf(perfString)
                        )
                    }

                    exitCode
                }.get()
            }
            CompilerMode.INCREMENTAL_COMPILER -> {
                val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
                val gradleIncrementalServicesFacade = servicesFacade

                when (targetPlatform) {
                    CompileService.TargetPlatform.JVM -> withIncrementalCompilation(k2PlatformArgs) {
                        TODO("we do not support incremental compilation at this moment")
                        // TODO we do not support incremental compilation at this moment

//                    doCompile(daemonReporter) { _ ->
//                        execIncrementalCompiler(
//                            k2PlatformArgs as K2JVMCompilerArguments,
//                            gradleIncrementalArgs,
//                            messageCollector,
//                            getICReporter(
//                                gradleIncrementalServicesFacade,
//                                compilationResults!!,
//                                gradleIncrementalArgs
//                            )
//                        )
//                    }
                    }
                    CompileService.TargetPlatform.JS -> withJsIC(k2PlatformArgs) {
                        TODO("we do not support JS right now")
                        // TODO we do not support JS right now
//                    doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
//                        execJsIncrementalCompiler(
//                            k2PlatformArgs as K2JSCompilerArguments,
//                            gradleIncrementalArgs,
//                            messageCollector,
//                            getICReporter(
//                                gradleIncrementalServicesFacade,
//                                compilationResults!!,
//                                gradleIncrementalArgs
//                            )
//                        )
//                    }
                    }
                    else -> throw IllegalStateException("Incremental compilation is not supported for target platform: $targetPlatform")

                }
            }
        }
    }

    private inline fun doCompile(
        daemonMessageReporter: DaemonMessageReporter,
        body: (EventManager) -> ExitCode,
    ): CompileService.CallResult<Int> = run {
        log.fine("alive!")
        val eventManager = EventManagerImpl()
        try {
            log.fine("trying get exitCode")
            val exitCode = checkedCompile(daemonMessageReporter) {
                body(eventManager).code
            }
            CompileService.CallResult.Good(exitCode)
        } finally {
            eventManager.fireCompilationFinished()
        }
    }

    fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)

    fun Long.kb() = this / 1024

    private inline fun <R> checkedCompile(
        daemonMessageReporter: DaemonMessageReporter,
        body: () -> R,
    ): R {
        try {
            val profiler = if (reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            val res = profiler.withMeasure(null, body)

            val endMem = if (reportPerf) usedMemory(withGC = false) else 0L

            log.info("Done with result $res")

            if (reportPerf) {
                val pc = profiler.getTotalCounters()

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; memory: ${endMem.kb()} kb (${
                    "%+d".format(
                        pc.memory.kb()
                    )
                } kb)".let {
                    daemonMessageReporter.report(ReportSeverity.INFO, it)
                    log.info(it)
                }

            }
            return res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Throwable) {
            log.log(
                Level.SEVERE,
                "Exception: $e\n  ${e.stackTrace.joinToString("\n  ")}${
                    if (e.cause != null && e.cause != e) {
                        "\nCaused by: ${e.cause}\n  ${e.cause!!.stackTrace.joinToString("\n  ")}"
                    } else ""
                }"
            )
            throw e
        }
    }
}