/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import common.OUTPUT_FILES_DIR
import common.buildAbsPath
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
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.EventManager
import org.jetbrains.kotlin.daemon.EventManagerImpl
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import org.jetbrains.kotlin.incremental.withIncrementalCompilation
import org.jetbrains.kotlin.incremental.withJsIC
import org.jetbrains.kotlin.util.PerformanceManager.DumpFormat
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
import org.jetbrains.kotlin.util.getLinesPerSecond
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil.kotlinPathsForDistDirectory
import java.io.File


fun getPerformanceMetrics(compiler: CLICompiler<CommonCompilerArguments>): List<BuildMetricsValue> {
    val performanceMetrics = ArrayList<BuildMetricsValue>()
    val performanceManager = compiler.defaultPerformanceManager
    val moduleStats = performanceManager.unitStats
    if (moduleStats.linesCount > 0) {
        performanceMetrics.add(BuildMetricsValue(CompilationPerformanceMetrics.SOURCE_LINES_NUMBER, moduleStats.linesCount.toLong()))
    }

    var codegenTime = Time.ZERO

    fun reportLps(lpsMetrics: CompilationPerformanceMetrics, time: Time) {
        if (time != Time.ZERO) {
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

    if (codegenTime != Time.ZERO) {
        performanceMetrics.add(BuildMetricsValue(CompilationPerformanceMetrics.CODE_GENERATION, codegenTime.millis))
        reportLps(CompilationPerformanceMetrics.CODE_GENERATION_LPS, codegenTime)
    }

    return performanceMetrics
}

fun <ServicesFacadeT, JpsServicesFacadeT, CompilationResultsT> compileImpl(
    sessionId: Int,
    compilerArguments: Array<out String>,
    compilationOptions: CompilationOptions,
    servicesFacade: ServicesFacadeT,
    compilationResults: CompilationResultsT,
    hasIncrementalCaches: JpsServicesFacadeT.() -> Boolean,
    createMessageCollector: (ServicesFacadeT, CompilationOptions) -> MessageCollector,
    createReporter: (ServicesFacadeT, CompilationOptions) -> DaemonMessageReporter,
    createServices: (JpsServicesFacadeT, EventManager, Profiler) -> Services,
    getICReporter: (ServicesFacadeT, CompilationResultsT?, IncrementalCompilationOptions) -> RemoteBuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
) = run {
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
        CompileService.CallResult.Good(ExitCode.COMPILATION_ERROR.code)
    } else when (compilationOptions.compilerMode) {
        CompilerMode.JPS_COMPILER -> {
            @Suppress("UNCHECKED_CAST")
            servicesFacade as JpsServicesFacadeT
            withIncrementalCompilation(k2PlatformArgs, enabled = servicesFacade.hasIncrementalCaches()) {
                doCompile(sessionId, daemonReporter, tracer = null) { eventManger, profiler ->
                    val services = createServices(servicesFacade, eventManger, profiler)
                    val exitCode = compiler.exec(messageCollector, services, k2PlatformArgs)

                    compilationResults.also {
                        val compilationResult = it as CompilationResults
                        getPerformanceMetrics(compiler).forEach {
                            compilationResult.add(CompilationResultCategory.BUILD_METRICS.code, it)
                        }
                    }

                    exitCode
                }
            }
        }
        CompilerMode.NON_INCREMENTAL_COMPILER -> {
            doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                val exitCode = compiler.exec(messageCollector, Services.EMPTY, k2PlatformArgs)

                val perfString = compiler.defaultPerformanceManager.createPerformanceReport(dumpFormat = DumpFormat.PlainText)
                compilationResults?.also {
                    (it as CompilationResults).add(
                        CompilationResultCategory.BUILD_REPORT_LINES.code,
                        arrayListOf(perfString)
                    )
                }

                exitCode
            }
        }
        CompilerMode.INCREMENTAL_COMPILER -> {
            val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
            val gradleIncrementalServicesFacade = servicesFacade

            when (targetPlatform) {
                CompileService.TargetPlatform.JVM -> withIncrementalCompilation(k2PlatformArgs) {
                    // TODO we do not support incremental compilation at this moment

//                    doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
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

inline fun doCompile(
    sessionId: Int,
    daemonMessageReporter: DaemonMessageReporter,
    tracer: RemoteOperationsTracer?,
    body: (EventManager, Profiler) -> ExitCode,
): CompileService.CallResult<Int> = run {
    println("alive!")
//    withValidClientOrSessionProxy(sessionId) {
        tracer?.before("compile")
//        val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
        val rpcProfiler = DummyProfiler()
        val eventManager = EventManagerImpl()
        try {
            println("trying get exitCode")
            val exitCode = body(eventManager, rpcProfiler).code

            CompileService.CallResult.Good(exitCode)
        } finally {
            eventManager.fireCompilationFinished()
            tracer?.after("compile")
        }
//    }
}


fun getKotlinPaths(): KotlinPaths {
    val paths = kotlinPathsForDistDirectory
    return paths
}


fun getCompilerLib(): File {
    return getKotlinPaths().libPath.getAbsoluteFile()
}
fun main() {

    // Print the current JVM classpath
    val classpath = System.getProperty("java.class.path")
    println("Current classpath:")
    classpath.split(File.pathSeparator).forEach { println("  $it") }

    val sourceFiles = listOf(
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input.kt")
    )
    val compilerArguments =
        sourceFiles.map { it-> it.absolutePath} + "-d" + buildAbsPath(OUTPUT_FILES_DIR) + "-cp" + "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar"
//    println("DEBUG SERVER: compilerArguments=${compilerArguments.contentToString()}")

    val remoteMessageCollector = RemoteMessageCollector(object : MessageSender {
        override fun send(msg: MessageCollectorImpl.Message) {
        }
    })



    val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
    val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

    val compilationOptions = CompilationOptions(
        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        targetPlatform = CompileService.TargetPlatform.JVM,
        reportSeverity = ReportSeverity.DEBUG.code,
        reportCategories = arrayOf(),
        requestedCompilationResults = arrayOf(),
    )

    compileImpl(
        sessionId = 1,
        compilerArguments = compilerArguments.toTypedArray(),
        compilationOptions = compilationOptions,
        servicesFacade = servicesFacade,
        compilationResults = null,
        hasIncrementalCaches = JpsCompilerServicesFacade::hasIncrementalCaches,
        createMessageCollector = { facade, options -> RemoteMessageCollector(object : MessageSender {
            override fun send(msg: MessageCollectorImpl.Message) {
                println("this is our compilation message $msg")
            }
        }) },
        createReporter = ::DaemonMessageReporter,
        createServices = { facade, eventManager, profiler ->
            // You'll need to implement this function or use an existing one
            // Based on the search results, there should be a createCompileServices function
            Services.EMPTY // Placeholder - replace with actual implementation
        },
        getICReporter = { a, b, c -> getBuildReporter(a, b!!, c) }
    )

}

