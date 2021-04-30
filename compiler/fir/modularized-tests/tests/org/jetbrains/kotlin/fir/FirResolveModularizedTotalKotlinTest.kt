/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.management.HotSpotDiagnosticMXBean
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.profiling.AsyncProfilerHelper
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.fir.analysis.FirCheckersResolveProcessor
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dump.MultiModuleHtmlFirDump
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import sun.management.ManagementFactoryHelper
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.text.DecimalFormat


private const val FAIL_FAST = true

private const val FIR_DUMP_PATH = "tmp/firDump"
private const val FIR_HTML_DUMP_PATH = "tmp/firDump-html"
const val FIR_LOGS_PATH = "tmp/fir-logs"
private const val FIR_MEMORY_DUMPS_PATH = "tmp/memory-dumps"

private val DUMP_FIR = System.getProperty("fir.bench.dump", "true").toBooleanLenient()!!
internal val PASSES = System.getProperty("fir.bench.passes")?.toInt() ?: 3
internal val SEPARATE_PASS_DUMP = System.getProperty("fir.bench.dump.separate_pass", "false").toBooleanLenient()!!
private val APPEND_ERROR_REPORTS = System.getProperty("fir.bench.report.errors.append", "false").toBooleanLenient()!!
private val RUN_CHECKERS = System.getProperty("fir.bench.run.checkers", "false").toBooleanLenient()!!
private val USE_LIGHT_TREE = System.getProperty("fir.bench.use.light.tree", "false").toBooleanLenient()!!
private val DUMP_MEMORY = System.getProperty("fir.bench.dump.memory", "false").toBooleanLenient()!!

private val ASYNC_PROFILER_LIB = System.getProperty("fir.bench.use.async.profiler.lib")
private val ASYNC_PROFILER_START_CMD = System.getProperty("fir.bench.use.async.profiler.cmd.start")
private val ASYNC_PROFILER_STOP_CMD = System.getProperty("fir.bench.use.async.profiler.cmd.stop")
private val PROFILER_SNAPSHOT_DIR = System.getProperty("fir.bench.snapshot.dir") ?: "tmp/snapshots"

private val REPORT_PASS_EVENTS = System.getProperty("fir.bench.report.pass.events", "false").toBooleanLenient()!!

private interface CLibrary : Library {
    fun getpid(): Int
    fun gettid(): Int

    companion object {
        val INSTANCE = Native.load("c", CLibrary::class.java) as CLibrary
    }
}

internal fun isolate() {
    val isolatedList = System.getenv("DOCKER_ISOLATED_CPUSET")
    val othersList = System.getenv("DOCKER_CPUSET")
    println("Trying to set affinity, other: '$othersList', isolated: '$isolatedList'")
    if (othersList != null) {
        println("Will move others affinity to '$othersList'")
        ProcessBuilder().command("bash", "-c", "ps -ae -o pid= | xargs -n 1 taskset -cap $othersList ").inheritIO().start().waitFor()
    }
    if (isolatedList != null) {
        val selfPid = CLibrary.INSTANCE.getpid()
        val selfTid = CLibrary.INSTANCE.gettid()
        println("Will pin self affinity, my pid: $selfPid, my tid: $selfTid")
        ProcessBuilder().command("taskset", "-cp", isolatedList, "$selfTid").inheritIO().start().waitFor()
    }
    if (othersList == null && isolatedList == null) {
        println("No affinity specified")
    }
}

class PassEventReporter(private val stream: PrintStream) : AutoCloseable {

    private val decimalFormat = DecimalFormat().apply {
        this.maximumFractionDigits = 3
    }

    private fun formatStamp(): String {
        val uptime = ManagementFactoryHelper.getRuntimeMXBean().uptime
        return decimalFormat.format(uptime.toDouble() / 1000)
    }

    fun reportPassStart(num: Int) {
        stream.println("<pass_start num='$num' stamp='${formatStamp()}'/>")
    }

    fun reportPassEnd(num: Int) {
        stream.println("<pass_end num='$num' stamp='${formatStamp()}'/>")
        stream.flush()
    }

    override fun close() {
        stream.close()
    }
}

class FirResolveModularizedTotalKotlinTest : AbstractModularizedTest() {

    private lateinit var dump: MultiModuleHtmlFirDump
    private lateinit var bench: FirResolveBench
    private var bestStatistics: FirResolveBench.TotalStatistics? = null
    private var bestPass: Int = 0

    private var passEventReporter: PassEventReporter? = null

    private val asyncProfiler = if (ASYNC_PROFILER_LIB != null) {
        try {
            AsyncProfilerHelper.getInstance(ASYNC_PROFILER_LIB)
        } catch (e: ExceptionInInitializerError) {
            if (e.cause is ClassNotFoundException) {
                throw IllegalStateException("Async-profiler initialization error, make sure async-profiler.jar is on classpath", e.cause)
            }
            throw e
        }
    } else {
        null
    }

    private fun executeAsyncProfilerCommand(command: String?, pass: Int) {
        if (asyncProfiler != null) {
            require(command != null)
            fun String.replaceParams(): String =
                this.replace("\$REPORT_DATE", reportDateStr)
                    .replace("\$PASS", pass.toString())

            val snapshotDir = File(PROFILER_SNAPSHOT_DIR.replaceParams()).also { it.mkdirs() }
            val expandedCommand = command
                .replace("\$SNAPSHOT_DIR", snapshotDir.toString())
                .replaceParams()
            val result = asyncProfiler.execute(expandedCommand)
            println("PROFILER: $result")
        }
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project
        val ktFiles = environment.getSourceFiles()

        val scope = GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = createSessionForTests(
            environment,
            scope,
            librariesScope,
            moduleData.qualifiedName,
            moduleData.friendDirs.map { it.toPath() }
        )
        val scopeSession = ScopeSession()
        val processors = createAllCompilerResolveProcessors(session, scopeSession).let {
            if (RUN_CHECKERS) {
                it + FirCheckersResolveProcessor(session, scopeSession)
            } else {
                it
            }
        }

        val firProvider = session.firProvider as FirProviderImpl

        val firFiles = if (USE_LIGHT_TREE) {
            val lightTree2Fir = LightTree2Fir(session, firProvider.kotlinScopeProvider, stubMode = false)

            val allSourceFiles = moduleData.sources.flatMap {
                if (it.isDirectory) {
                    it.walkTopDown().toList()
                } else {
                    listOf(it)
                }
            }.filter {
                it.extension == "kt"
            }
            bench.buildFiles(lightTree2Fir, allSourceFiles)
        } else {
            val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider)
            bench.buildFiles(builder, ktFiles)
        }


        bench.processFiles(firFiles, processors)

        createMemoryDump(moduleData)

        val disambiguatedName = moduleData.disambiguatedName()
        dumpFir(disambiguatedName, firFiles)
        dumpFirHtml(disambiguatedName, firFiles)
    }

    private fun dumpFir(disambiguatedName: String, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        val dumpRoot = File(FIR_DUMP_PATH).resolve(disambiguatedName)
        firFiles.forEach {
            val directory = it.packageFqName.pathSegments().fold(dumpRoot) { file, name -> file.resolve(name.asString()) }
            directory.mkdirs()
            directory.resolve(it.name + ".fir").writeText(it.render())
        }
    }

    private val dumpedModules = mutableSetOf<String>()
    private fun ModuleData.disambiguatedName(): String {
        val baseName = qualifiedName
        var disambiguatedName = baseName
        var counter = 1
        while (!dumpedModules.add(disambiguatedName)) {
            disambiguatedName = "$baseName.${counter++}"
        }
        return disambiguatedName
    }

    private fun dumpFirHtml(disambiguatedName: String, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        dump.module(disambiguatedName) {
            firFiles.forEach(dump::indexFile)
            firFiles.forEach(dump::generateFile)
        }
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val disposable = Disposer.newDisposable()
        val configuration = createDefaultConfiguration(moduleData)
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        PsiElementFinder.EP.getPoint(environment.project)
            .unregisterExtension(JavaElementFinder::class.java)

        runAnalysis(moduleData, environment)

        Disposer.dispose(disposable)
        if (bench.hasFiles && FAIL_FAST) return ProcessorAction.STOP
        return ProcessorAction.NEXT
    }

    override fun beforePass(pass: Int) {
        if (DUMP_FIR) dump = MultiModuleHtmlFirDump(File(FIR_HTML_DUMP_PATH))
        System.gc()
        passEventReporter?.reportPassStart(pass)
        executeAsyncProfilerCommand(ASYNC_PROFILER_START_CMD, pass)
    }

    override fun afterPass(pass: Int) {
        val statistics = bench.getTotalStatistics()
        statistics.report(System.out, "Pass $pass")

        saveReport(pass, statistics)
        if (statistics.totalTime < (bestStatistics?.totalTime ?: Long.MAX_VALUE)) {
            bestStatistics = statistics
            bestPass = pass
        }
        if (!SEPARATE_PASS_DUMP) {
            dumpedModules.clear()
        }
        if (FAIL_FAST) {
            bench.throwFailure()
        }

        executeAsyncProfilerCommand(ASYNC_PROFILER_STOP_CMD, pass)

        passEventReporter?.reportPassEnd(pass)
    }

    override fun afterAllPasses() {
        val bestStatistics = bestStatistics ?: return
        printStatistics(bestStatistics, "Best pass: $bestPass")
        printErrors(bestStatistics)
    }

    private fun saveReport(pass: Int, statistics: FirResolveBench.TotalStatistics) {
        if (DUMP_FIR) dump.finish()
        printStatistics(statistics, "PASS $pass")
    }

    private fun printErrors(statistics: FirResolveBench.TotalStatistics) {
        PrintStream(
            FileOutputStream(
                reportDir().resolve("errors-$reportDateStr.log"),
                APPEND_ERROR_REPORTS
            )
        ).use(statistics::reportErrors)
    }

    private fun printStatistics(statistics: FirResolveBench.TotalStatistics, header: String) {
        PrintStream(
            FileOutputStream(
                reportDir().resolve("report-$reportDateStr.log"),
                true
            )
        ).use { stream ->
            statistics.report(stream, header)
            stream.println()
            stream.println()
        }
    }

    private fun beforeAllPasses() {
        isolate()

        if (REPORT_PASS_EVENTS) {
            passEventReporter =
                PassEventReporter(PrintStream(FileOutputStream(reportDir().resolve("pass-events-$reportDateStr.log"), true)))
        }
    }

    fun testTotalKotlin() {

        beforeAllPasses()

        for (i in 0 until PASSES) {
            println("Pass $i")
            bench = FirResolveBench(withProgress = false)
            runTestOnce(i)
        }
        afterAllPasses()
    }

    private fun createMemoryDump(moduleData: ModuleData) {
        if (!DUMP_MEMORY) return
        val name = "module_${moduleData.name}.hprof"
        val dir = File(FIR_MEMORY_DUMPS_PATH).also {
            it.mkdirs()
        }
        val filePath = dir.resolve(name).absolutePath
        createMemoryDump(filePath)
    }

    private fun createMemoryDump(filePath: String) {
        val server = ManagementFactory.getPlatformMBeanServer()
        val mxBean = ManagementFactory.newPlatformMXBeanProxy(
            server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean::class.java
        )
        mxBean.dumpHeap(filePath, true)
    }
}
