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
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dump.MultiModuleHtmlFirDump
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.measureNanoTime


private const val FAIL_FAST = true
private const val DUMP_FIR = true

private const val FIR_DUMP_PATH = "tmp/firDump"
private const val FIR_HTML_DUMP_PATH = "tmp/firDump-html"
private const val FIR_LOGS_PATH = "tmp/fir-logs"

internal val PASSES = System.getProperty("fir.bench.passes")?.toInt() ?: 3

class FirResolveModularizedTotalKotlinTest : AbstractModularizedTest() {

    private lateinit var bench: FirResolveBench
    private lateinit var dump: MultiModuleHtmlFirDump

    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project
        val ktFiles = environment.getSourceFiles()

        val scope = ProjectScope.getContentScope(project)
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = createSession(environment, scope, librariesScope)
        val builder = RawFirBuilder(session, stubMode = false)

        val totalTransformer = FirTotalResolveTransformer()
        val firFiles = ktFiles.toList().mapNotNull {
            var firFile: FirFile? = null
            val time = measureNanoTime {
                firFile = builder.buildFirFile(it)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile!!)
            }
            bench.countBuilder(builder, time)
            firFile
        }.toList()


        println("Raw FIR up, files: ${firFiles.size}")

        bench.processFiles(firFiles, totalTransformer.transformers)

        dumpFir(moduleData, firFiles)
        dumpFirHtml(moduleData, firFiles)
    }

    private fun dumpFir(moduleData: ModuleData, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        val dumpRoot = File(FIR_DUMP_PATH).resolve(moduleData.qualifiedName)
        firFiles.forEach {
            val directory = it.packageFqName.pathSegments().fold(dumpRoot) { file, name -> file.resolve(name.asString()) }
            directory.mkdirs()
            directory.resolve(it.name + ".fir").writeText(it.render())
        }
    }

    private fun dumpFirHtml(moduleData: ModuleData, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        dump.module(moduleData.qualifiedName) {
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

        configuration.put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, true)
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

    override fun afterPass() {
        bench.report(System.out, errorTypeReports = false)

        saveReport()
        if (FAIL_FAST) {
            bench.throwFailure()
        }
    }

    private fun saveReport() {
        if (DUMP_FIR) dump.finish()
        val format = SimpleDateFormat("yyyy-MM-dd__HH-mm")
        val logDir = File(FIR_LOGS_PATH)
        logDir.mkdirs()
        PrintStream(logDir.resolve("report-${format.format(Date())}.log").outputStream()).use { stream ->
            bench.report(stream)
        }
    }

    fun testTotalKotlin() {
        for (i in 0 until PASSES) {
            println("Pass $i")

            bench = FirResolveBench(withProgress = false)
            runTestOnce(i)
        }
    }
}