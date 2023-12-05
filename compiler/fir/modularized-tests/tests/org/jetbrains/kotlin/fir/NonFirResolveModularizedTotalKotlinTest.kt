/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.measureNanoTime

private val USE_NI = System.getProperty("fir.bench.oldfe.ni", "true") == "true"

class NonFirResolveModularizedTotalKotlinTest : AbstractFrontendModularizedTest() {
    private var totalTime = 0L
    private var files = 0
    private var lines = 0
    private var measure = FirResolveBench.Measure()

    private val times = mutableListOf<Long>()

    private fun runAnalysis(environment: KotlinCoreEnvironment) {
        val vmBefore = vmStateSnapshot()
        val time = measureNanoTime {
            try {
                KotlinToJVMBytecodeCompiler.analyze(environment)
            } catch (e: Throwable) {
                var exception: Throwable? = e
                while (exception != null && exception != exception.cause) {
                    exception.printStackTrace()
                    exception = exception.cause
                }
                throw e
            }
        }
        val vmAfter = vmStateSnapshot()

        val psiFiles = environment.getSourceFiles()
        files += psiFiles.size
        lines += psiFiles.sumOf { StringUtil.countNewLines(it.text) }
        totalTime += time
        measure.time += time
        measure.vmCounters += vmAfter - vmBefore
        measure.files += psiFiles.size

        println("Time is ${time * 1e-6} ms")
    }

    private fun writeMessageToLog(message: String) {
        PrintStream(FileOutputStream(reportDir().resolve("report-$reportDateStr.log"), true)).use { stream ->
            stream.println(message)
        }
    }

    private fun dumpTime(message: String, time: Long) {
        writeMessageToLog("$message: ${time * 1e-6} ms")
    }

    private fun configureAndSetupEnvironment(moduleData: ModuleData, disposable: Disposable): KotlinCoreEnvironment {

        val configuration = createDefaultConfiguration(moduleData)

        configureLanguageVersionSettings(
            configuration, moduleData,
            LanguageVersion.fromVersionString(LANGUAGE_VERSION_K1)!!,
            configureFeatures = {
                put(LanguageFeature.NewInference, if (USE_NI) LanguageFeature.State.ENABLED else LanguageFeature.State.DISABLED)
            },
            configureFlags = {
                // TODO: Remove when old tests are no longer supported
                if (moduleData.arguments == null) {
                    put(AnalysisFlags.skipPrereleaseCheck, true)
                }
            }
        )
        // TODO: Remove when old tests are no longer supported
        if (moduleData.arguments == null) {
            System.getProperty("fir.bench.oldfe.jvm_target")?.let {
                configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.fromString(it) ?: error("Unknown JvmTarget"))
            }
        }
        configuration.put(MESSAGE_COLLECTOR_KEY, object : MessageCollector {
            override fun clear() {

            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                if (location != null)
                    print(location.toString())
                print(":")
                print(severity)
                print(":")
                println(message)
            }

            override fun hasErrors(): Boolean {
                return false
            }

        })
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val visibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)
        for (friendDir in configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)) {
            visibilityManager.addFriendPath(friendDir)
        }

        return environment
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val disposable = Disposer.newDisposable("Disposable for ${NonFirResolveModularizedTotalKotlinTest::class.simpleName}.processModule")
        try {
            val environment = configureAndSetupEnvironment(moduleData, disposable)
            runAnalysis(environment)
        } finally {
            Disposer.dispose(disposable)
        }

        return ProcessorAction.NEXT
    }

    override fun afterPass(pass: Int) {}

    override fun beforePass(pass: Int) {
        measure = FirResolveBench.Measure()
        files = 0
        lines = 0
        totalTime = 0
    }

    fun testTotalKotlin() {

        pinCurrentThreadToIsolatedCpu()

        writeMessageToLog("use_ni: $USE_NI")

        for (i in 0 until PASSES) {
            runTestOnce(i)
            times += totalTime
            dumpTime("Pass $i", totalTime)
            totalTime = 0L
        }

        val bestTime = times.minOrNull()!!
        val bestPass = times.indexOf(bestTime)
        dumpTime("Best pass: $bestPass", bestTime)
    }
}
