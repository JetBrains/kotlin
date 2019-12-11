/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.CONTENT_ROOTS
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dump.MultiModuleHtmlFirDump
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream


private const val FAIL_FAST = true

private const val FIR_DUMP_PATH = "tmp/firDump"
private const val FIR_HTML_DUMP_PATH = "tmp/firDump-html"
const val FIR_LOGS_PATH = "tmp/fir-logs"

private val DUMP_FIR = System.getProperty("fir.bench.dump", "true") == "true"
internal val PASSES = System.getProperty("fir.bench.passes")?.toInt() ?: 3
internal val SEPARATE_PASS_DUMP = System.getProperty("fir.bench.dump.separate_pass", "false") == "true"

class FirResolveModularizedTotalKotlinTest : AbstractModularizedTest() {

    private lateinit var dump: MultiModuleHtmlFirDump
    private lateinit var bench: FirResolveBench
    private var bestStatistics: FirResolveBench.TotalStatistics? = null
    private var bestPass: Int = 0

    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project
        val ktFiles = environment.getSourceFiles()

        val scope = ProjectScope.getContentScope(project)
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = createSession(environment, scope, librariesScope, moduleData.qualifiedName)
        val builder = RawFirBuilder(session, stubMode = false)

        val totalTransformer = FirTotalResolveTransformer()
        val firFiles = bench.buildFiles(builder, ktFiles)

        println("Raw FIR up, files: ${firFiles.size}")

        bench.processFiles(firFiles, totalTransformer.transformers)

        val disambiguatedName = moduleData.disambiguatedName()
        dumpFir(disambiguatedName, moduleData, firFiles)
        dumpFirHtml(disambiguatedName, moduleData, firFiles)
    }

    private fun dumpFir(disambiguatedName: String, moduleData: ModuleData, firFiles: List<FirFile>) {
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
        while(!dumpedModules.add(disambiguatedName)) {
            disambiguatedName = "$baseName.${counter++}"
        }
        return disambiguatedName
    }

    private fun dumpFirHtml(disambiguatedName: String, moduleData: ModuleData, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        dump.module(disambiguatedName) {
            firFiles.forEach(dump::indexFile)
            firFiles.forEach(dump::generateFile)
        }
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val configurationKind = ConfigurationKind.ALL
        val testJdkKind = TestJdkKind.FULL_JDK


        val disposable = Disposer.newDisposable()

        val configuration =
            KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, moduleData.classpath, moduleData.javaSourceRoots)

        configuration.addAll(
            CONTENT_ROOTS,
            moduleData.sources.filter { it.extension == "kt" }.map { KotlinSourceRoot(it.absolutePath, false) })
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        Extensions.getArea(environment.project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)

        runAnalysis(moduleData, environment)

        Disposer.dispose(disposable)
        if (bench.hasFiles && FAIL_FAST) return ProcessorAction.STOP
        return ProcessorAction.NEXT
    }

    override fun beforePass() {
        if (DUMP_FIR) dump = MultiModuleHtmlFirDump(File(FIR_HTML_DUMP_PATH))
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
        PrintStream(FileOutputStream(reportDir().resolve("errors-$reportDateStr.log"), true)).use(statistics::reportErrors)
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

    fun testTotalKotlin() {
        for (i in 0 until PASSES) {
            println("Pass $i")

            bench = FirResolveBench(withProgress = false)
            runTestOnce(i)
        }
        afterAllPasses()
    }
}