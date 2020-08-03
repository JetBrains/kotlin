/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.measureNanoTime

private val USE_NI = System.getProperty("fir.bench.oldfe.ni", "true") == "true"

class NonFirResolveModularizedTotalKotlinTest : AbstractModularizedTest() {
    private var totalTime = 0L
    private var files = 0

    private val times = mutableListOf<Long>()

    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project

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

        files += environment.getSourceFiles().size
        totalTime += time
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

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val disposable = Disposer.newDisposable()


        val configuration = createDefaultConfiguration(moduleData)


        configuration.languageVersionSettings =
            LanguageVersionSettingsImpl(
                configuration.languageVersionSettings.languageVersion,
                configuration.languageVersionSettings.apiVersion,
                specificFeatures = mapOf(
                    LanguageFeature.NewInference to if (USE_NI) LanguageFeature.State.ENABLED else LanguageFeature.State.DISABLED
                )
            )

        System.getProperty("fir.bench.oldfe.jvm_target")?.let {
            configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.fromString(it) ?: error("Unknown JvmTarget"))
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

        runAnalysis(moduleData, environment)

        Disposer.dispose(disposable)
        return ProcessorAction.NEXT
    }


    override fun afterPass(pass: Int) {}
    override fun beforePass() {}

    fun testTotalKotlin() {
        writeMessageToLog("use_ni: $USE_NI")

        for (i in 0 until PASSES) {
            runTestOnce(i)
            times += totalTime
            dumpTime("Pass $i", totalTime)
            totalTime = 0L
        }

        val bestTime = times.min()!!
        val bestPass = times.indexOf(bestTime)
        dumpTime("Best pass: $bestPass", bestTime)
    }
}