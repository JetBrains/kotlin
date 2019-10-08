/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.CONTENT_ROOTS
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import kotlin.system.measureNanoTime


class NonFirResolveModularizedTotalKotlinTest : AbstractModularizedTest() {


    private var totalTime = 0L
    private var files = 0

    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project

        val time = measureNanoTime {
            KotlinToJVMBytecodeCompiler.analyze(environment, null)
        }

        files += environment.getSourceFiles().size
        totalTime += time
        println("Time is ${time * 1e-6} ms")

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

        if (System.getProperty("fir.bench.oldfe.ni", "true") == "true") {
            configuration.languageVersionSettings =
                LanguageVersionSettingsImpl(
                    LanguageVersion.KOTLIN_1_4, ApiVersion.KOTLIN_1_3, specificFeatures = mapOf(
                        LanguageFeature.NewInference to LanguageFeature.State.ENABLED
                    )
                )
        }

        configuration.put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, true)

        System.getProperty("fir.bench.oldfe.jvm_target")?.let {
            configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.fromString(it) ?: error("Unknown JvmTarget"))
        }
        configuration.put(MESSAGE_COLLECTOR_KEY, object : MessageCollector {
            override fun clear() {

            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
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
        for (i in 0 until PASSES) {
            runTestOnce(i)
            println("Total time is ${totalTime * 1e-6} ms")
            totalTime = 0L
        }
    }
}