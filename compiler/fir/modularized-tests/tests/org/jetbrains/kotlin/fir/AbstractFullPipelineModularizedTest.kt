/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path

private val JVM_TARGET: String = System.getProperty("fir.bench.jvm.target", "1.8")

abstract class AbstractFullPipelineModularizedTest : AbstractModularizedTest() {

    private val asyncProfilerControl = AsyncProfilerControl()

    data class ModuleStatus(val data: ModuleData, val targetInfo: String) {
        var compilationError: String? = null
        var jvmInternalError: String? = null
        var exceptionMessage: String = "NO MESSAGE"
    }

    private val totalModules = mutableListOf<ModuleStatus>()
    private val okModules = mutableListOf<ModuleStatus>()
    private val errorModules = mutableListOf<ModuleStatus>()
    private val crashedModules = mutableListOf<ModuleStatus>()

    protected data class CumulativeTime(
        val gcInfo: Map<String, GCInfo>,
        val components: Map<String, Long>,
        val files: Int,
        val lines: Int
    ) {
        constructor() : this(emptyMap(), emptyMap(), 0, 0)

        operator fun plus(other: CumulativeTime): CumulativeTime {
            return CumulativeTime(
                (gcInfo.values + other.gcInfo.values).groupingBy { it.name }.reduce { key, accumulator, element ->
                    GCInfo(key, accumulator.gcTime + element.gcTime, accumulator.collections + element.collections)
                },
                (components.toList() + other.components.toList()).groupingBy { (name, _) -> name }.fold(0L) { a, b -> a + b.second },
                files + other.files,
                lines + other.lines
            )
        }

        fun totalTime() = components.values.sum()
    }

    protected lateinit var totalPassResult: CumulativeTime

    override fun beforePass(pass: Int) {
        totalPassResult = CumulativeTime()
        totalModules.clear()
        okModules.clear()
        errorModules.clear()
        crashedModules.clear()

        asyncProfilerControl.beforePass(pass, reportDateStr)
    }

    override fun afterPass(pass: Int) {
        asyncProfilerControl.afterPass(pass, reportDateStr)

        createReport(finalReport = pass == PASSES - 1)
        require(totalModules.isNotEmpty()) { "No modules were analyzed" }
        require(okModules.isNotEmpty()) { "All of $totalModules is failed" }
    }

    protected fun formatReportTable(stream: PrintStream) {
        val total = totalPassResult
        var totalGcTimeMs = 0L
        var totalGcCount = 0L
        printTable(stream) {
            row("Name", "Time", "Count")
            separator()
            fun gcRow(name: String, timeMs: Long, count: Long) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = TableTimeUnit.MS)
                    cell(count.toString())
                }
            }
            for (measurement in total.gcInfo.values) {
                totalGcTimeMs += measurement.gcTime
                totalGcCount += measurement.collections
                gcRow(measurement.name, measurement.gcTime, measurement.collections)
            }
            separator()
            gcRow("Total", totalGcTimeMs, totalGcCount)

        }

        printTable(stream) {
            row("Phase", "Time", "Files", "L/S")
            separator()

            fun phase(name: String, timeMs: Long, files: Int, lines: Int) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = TableTimeUnit.MS)
                    cell(files.toString())
                    linePerSecondCell(lines, timeMs, timeUnit = TableTimeUnit.MS)
                }
            }
            for (component in total.components) {
                phase(component.key, component.value, total.files, total.lines)
            }

            separator()
            phase("Total", total.totalTime(), total.files, total.lines)
        }

    }

    private fun configureBaseArguments(args: K2JVMCompilerArguments, moduleData: ModuleData, tmp: Path) {
        val originalArguments = moduleData.arguments as? K2JVMCompilerArguments
        if (originalArguments != null) {
            args.apiVersion = originalArguments.apiVersion
            args.noJdk = originalArguments.noJdk
            args.noStdlib = originalArguments.noStdlib
            args.noReflect = originalArguments.noReflect
            args.jvmTarget = originalArguments.jvmTargetIfSupported()?.description
            args.jsr305 = originalArguments.jsr305
            args.nullabilityAnnotations = originalArguments.nullabilityAnnotations
            args.jspecifyAnnotations = originalArguments.jspecifyAnnotations
            args.jvmDefault = originalArguments.jvmDefault
            args.jdkRelease = originalArguments.jdkRelease
            args.progressiveMode = originalArguments.progressiveMode
            args.optIn = (moduleData.optInAnnotations + (originalArguments.optIn ?: emptyArray())).toTypedArray()
            args.allowKotlinPackage = originalArguments.allowKotlinPackage

            args.pluginOptions = originalArguments.pluginOptions
            args.pluginClasspaths = originalArguments.pluginClasspaths?.mapNotNull {
                substituteCompilerPluginPathForKnownPlugins(it)?.absolutePath
            }?.toTypedArray()
            args.contextReceivers = originalArguments.contextReceivers
            args.skipPrereleaseCheck = originalArguments.skipPrereleaseCheck

        } else {
            args.jvmTarget = JVM_TARGET
            args.allowKotlinPackage = true
        }
        args.reportPerf = true
        args.jdkHome = moduleData.jdkHome?.absolutePath ?: originalArguments?.jdkHome?.fixPath()?.absolutePath
        args.renderInternalDiagnosticNames = true
        configureArgsUsingBuildFile(args, moduleData, tmp)
    }

    private fun configureArgsUsingBuildFile(args: K2JVMCompilerArguments, moduleData: ModuleData, tmp: Path) {
        val builder = KotlinModuleXmlBuilder()
        builder.addModule(
            moduleData.name,
            tmp.toAbsolutePath().toFile().toString(),
            sourceFiles = moduleData.sources,
            javaSourceRoots = moduleData.javaSourceRoots.map { JvmSourceRoot(it.path, it.packagePrefix) },
            classpathRoots = moduleData.classpath,
            commonSourceFiles = emptyList(),
            modularJdkRoot = moduleData.modularJdkRoot,
            "java-production",
            isTests = false,
            emptySet(),
            friendDirs = moduleData.friendDirs,
            isIncrementalCompilation = true
        )
        val modulesFile = tmp.toFile().resolve("modules.xml")
        modulesFile.writeText(builder.asText().toString())
        args.buildFile = modulesFile.absolutePath
    }

    abstract fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData)

    protected open fun handleResult(result: ExitCode, moduleData: ModuleData, collector: TestMessageCollector, targetInfo: String): ProcessorAction {
        val status = ModuleStatus(moduleData, targetInfo)
        totalModules += status

        return when (result) {
            ExitCode.OK -> {
                okModules += status
                ProcessorAction.NEXT
            }
            ExitCode.COMPILATION_ERROR -> {
                errorModules += status
                status.compilationError = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.ERROR
                }?.message
                status.jvmInternalError = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.EXCEPTION
                }?.message
                ProcessorAction.NEXT
            }
            ExitCode.INTERNAL_ERROR -> {
                crashedModules += status
                status.exceptionMessage = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.EXCEPTION
                }?.message?.split("\n")?.let { exceptionLines ->
                    exceptionLines.lastOrNull { it.startsWith("Caused by: ") } ?: exceptionLines.firstOrNull()
                } ?: "NO MESSAGE"
                ProcessorAction.NEXT
            }
            else -> ProcessorAction.NEXT
        }
    }


    private fun String.shorten(): String {
        val split = split("\n")
        return split.mapIndexedNotNull { index, s ->
            if (index < 4 || index >= split.size - 6) s else null
        }.joinToString("\n")
    }

    open fun formatReport(stream: PrintStream, finalReport: Boolean) {
        stream.println("TOTAL MODULES: ${totalModules.size}")
        stream.println("OK MODULES: ${okModules.size}")
        stream.println("FAILED MODULES: ${totalModules.size - okModules.size}")

        formatReportTable(stream)

        if (finalReport) {
            with(stream) {
                println()
                println("SUCCESSFUL MODULES")
                println("------------------")
                println()
                for (okModule in okModules) {
                    println("${okModule.data.qualifiedName}: ${okModule.targetInfo}")
                }
                println()
                println("COMPILATION ERRORS")
                println("------------------")
                println()
                for (errorModule in errorModules.filter { it.jvmInternalError == null }) {
                    println("${errorModule.data.qualifiedName}: ${errorModule.targetInfo}")
                    println("        1st error: ${errorModule.compilationError}")
                }
                println()
                println("JVM INTERNAL ERRORS")
                println("------------------")
                println()
                for (errorModule in errorModules.filter { it.jvmInternalError != null }) {
                    println("${errorModule.data.qualifiedName}: ${errorModule.targetInfo}")
                    println("        1st error: ${errorModule.jvmInternalError?.shorten()}")
                }
                val crashedModuleGroups = crashedModules.groupBy { it.exceptionMessage.take(60) }
                for (modules in crashedModuleGroups.values) {
                    println()
                    println(modules.first().exceptionMessage)
                    println("--------------------------------------------------------")
                    println()
                    for (module in modules) {
                        println("${module.data.qualifiedName}: ${module.targetInfo}")
                        println("        ${module.exceptionMessage}")
                    }
                }
            }
        }
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments()
        val tmp = Files.createTempDirectory("compile-output")
        configureBaseArguments(args, moduleData, tmp)
        configureArguments(args, moduleData)

        val manager = CompilerPerformanceManager()
        val services = Services.Builder().register(CommonCompilerPerformanceManager::class.java, manager).build()
        val collector = TestMessageCollector()
        val result = try {
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
            compiler.exec(collector, services, args)
        } catch (e: Exception) {
            e.printStackTrace()
            ExitCode.INTERNAL_ERROR
        }
        val resultTime = manager.reportCumulativeTime()
        PerformanceCounter.resetAllCounters()

        tmp.toFile().deleteRecursively()
        if (result == ExitCode.OK) {
            totalPassResult += resultTime
        }

        return handleResult(result, moduleData, collector, manager.getTargetInfo())
    }

    protected fun createReport(finalReport: Boolean) {
        formatReport(System.out, finalReport)

        PrintStream(
            FileOutputStream(
                reportDir().resolve("report-$reportDateStr.log"),
                true
            )
        ).use { stream ->
            formatReport(stream, finalReport)
            stream.println()
            stream.println()
        }
    }


    private inner class CompilerPerformanceManager : CommonCompilerPerformanceManager("Modularized test performance manager") {

        fun reportCumulativeTime(): CumulativeTime {
            val gcInfo = measurements.filterIsInstance<GarbageCollectionMeasurement>()
                .associate { it.garbageCollectionKind to GCInfo(it.garbageCollectionKind, it.milliseconds, it.count) }

            val analysisMeasurement = measurements.filterIsInstance<CodeAnalysisMeasurement>().firstOrNull()
            val initMeasurement = measurements.filterIsInstance<CompilerInitializationMeasurement>().firstOrNull()
            val irMeasurements = measurements.filterIsInstance<IRMeasurement>()

            val components = buildMap {
                put("Init", initMeasurement?.milliseconds ?: 0)
                put("Analysis", analysisMeasurement?.milliseconds ?: 0)

                irMeasurements.firstOrNull { it.kind == IRMeasurement.Kind.TRANSLATION }?.milliseconds?.let { put("Translation", it) }
                irMeasurements.firstOrNull { it.kind == IRMeasurement.Kind.LOWERING }?.milliseconds?.let { put("Lowering", it) }

                val generationTime =
                    irMeasurements.firstOrNull { it.kind == IRMeasurement.Kind.GENERATION }?.milliseconds ?:
                    measurements.filterIsInstance<CodeGenerationMeasurement>().firstOrNull()?.milliseconds

                if (generationTime != null) {
                    put("Generation", generationTime)
                }
            }

            return CumulativeTime(
                gcInfo,
                components,
                files ?: 0,
                lines ?: 0
            )
        }
    }

    protected class TestMessageCollector : MessageCollector {

        data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageSourceLocation?)

        val messages = arrayListOf<Message>()

        override fun clear() {
            messages.clear()
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages.add(Message(severity, message, location))
            if (severity in CompilerMessageSeverity.VERBOSE) return
            println(MessageRenderer.GRADLE_STYLE.render(severity, message, location))
        }

        override fun hasErrors(): Boolean = messages.any {
            it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR
        }
    }


}



fun substituteCompilerPluginPathForKnownPlugins(path: String): File? {
    val file = File(path)
    val paths = PathUtil.kotlinPathsForDistDirectoryForTests
    return when {
        file.name.startsWith("kotlinx-serialization") || file.name.startsWith("kotlin-serialization") ->
            paths.jar(KotlinPaths.Jar.SerializationPlugin)
        file.name.startsWith("kotlin-sam-with-receiver") -> paths.jar(KotlinPaths.Jar.SamWithReceiver)
        file.name.startsWith("kotlin-allopen") -> paths.jar(KotlinPaths.Jar.AllOpenPlugin)
        file.name.startsWith("kotlin-noarg") -> paths.jar(KotlinPaths.Jar.NoArgPlugin)
        file.name.startsWith("kotlin-lombok") -> paths.jar(KotlinPaths.Jar.LombokPlugin)
        else -> null
    }

}