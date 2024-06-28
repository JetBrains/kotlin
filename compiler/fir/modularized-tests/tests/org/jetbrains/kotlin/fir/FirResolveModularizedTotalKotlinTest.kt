/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.sun.management.HotSpotDiagnosticMXBean
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.components.DiagnosticComponentsFactory
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dump.MultiModuleHtmlFirDump
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory


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
private val USE_LIGHT_TREE = System.getProperty("fir.bench.use.light.tree", "true").toBooleanLenient()!!
private val DUMP_MEMORY = System.getProperty("fir.bench.dump.memory", "false").toBooleanLenient()!!



class FirResolveModularizedTotalKotlinTest : AbstractFrontendModularizedTest() {

    private lateinit var dump: MultiModuleHtmlFirDump
    private lateinit var bench: FirResolveBench
    private var bestStatistics: FirResolveBench.TotalStatistics? = null
    private var bestPass: Int = 0

    private val asyncProfilerControl = AsyncProfilerControl()

    @OptIn(ObsoleteTestInfrastructure::class)
    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {

        val projectEnvironment = environment.toAbstractProjectEnvironment() as VfsBasedProjectEnvironment
        val project = environment.project

        val (sourceFiles: Collection<KtSourceFile>, scope) =
            if (USE_LIGHT_TREE) {
                val (platformSources, _) = collectSources(environment.configuration, projectEnvironment, environment.messageCollector)
                platformSources to projectEnvironment.getSearchScopeForProjectJavaSources()
            } else {
                val ktFiles = environment.getSourceFiles()
                ktFiles.map { KtPsiSourceFile(it) } to
                        GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })
                            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
                            .toAbstractProjectFileSearchScope()
            }
        val librariesScope = ProjectScope.getLibrariesScope(project)

        val session =
            FirTestSessionFactoryHelper.createSessionForTests(
                projectEnvironment,
                scope,
                librariesScope.toAbstractProjectFileSearchScope(),
                moduleData.qualifiedName,
                moduleData.friendDirs.map { it.toPath() },
                environment.configuration.languageVersionSettings
            )

        val scopeSession = ScopeSession()
        val processors = createAllCompilerResolveProcessors(session, scopeSession).let {
            if (RUN_CHECKERS) {
                it + FirCheckersResolveProcessor(session, scopeSession, MppCheckerKind.Common)
            } else {
                it
            }
        }

        val firProvider = session.firProvider as FirProviderImpl

        val firFiles = if (USE_LIGHT_TREE) {
            val lightTree2Fir = LightTree2Fir(session, firProvider.kotlinScopeProvider, diagnosticsReporter = null)
            bench.buildFiles(lightTree2Fir, sourceFiles)
        } else {
            val builder = PsiRawFirBuilder(session, firProvider.kotlinScopeProvider)
            bench.buildFiles(builder, sourceFiles.map { it as KtPsiSourceFile })
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
        val disposable = Disposer.newDisposable("Disposable for ${FirResolveModularizedTotalKotlinTest::class.simpleName}.processModule")

        try {
            val configuration = createDefaultConfiguration(moduleData)
            configureLanguageVersionSettings(configuration, moduleData, LanguageVersion.fromVersionString(LANGUAGE_VERSION_K2)!!)
            val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            PsiElementFinder.EP.getPoint(environment.project)
                .unregisterExtension(JavaElementFinder::class.java)

            runAnalysis(moduleData, environment)
        } finally {
            Disposer.dispose(disposable)
        }

        if (bench.hasFiles && FAIL_FAST) return ProcessorAction.STOP
        return ProcessorAction.NEXT
    }

    override fun beforePass(pass: Int) {
        if (DUMP_FIR) dump = MultiModuleHtmlFirDump(File(FIR_HTML_DUMP_PATH))
        System.gc()
        asyncProfilerControl.beforePass(pass, reportDateStr)
    }

    override fun afterPass(pass: Int) {

        asyncProfilerControl.afterPass(pass, reportDateStr)

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
        pinCurrentThreadToIsolatedCpu()
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

class FirCheckersResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession,
    mppCheckerKind: MppCheckerKind
) : FirTransformerBasedResolveProcessor(session, scopeSession, phase = null) {
    val diagnosticCollector: AbstractDiagnosticCollector = DiagnosticComponentsFactory.create(session, scopeSession, mppCheckerKind)

    override val transformer: FirTransformer<Nothing?> = FirCheckersRunnerTransformer(diagnosticCollector)
}

class FirCheckersRunnerTransformer(private val diagnosticCollector: AbstractDiagnosticCollector) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile = file.also {
        withFileAnalysisExceptionWrapping(file) {
            val reporter = DiagnosticReporterFactory.createPendingReporter()
            diagnosticCollector.collectDiagnostics(file, reporter)
        }
    }
}
