/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.util.PerformanceCounter
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors

abstract class AbstractFullPipelineParallelTest : AbstractFullPipelineModularizedTest() {

    private val executorService = Executors.newFixedThreadPool(2)

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        println("(${Thread.currentThread().name}) took ${moduleData.qualifiedName}")
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
        println("(${Thread.currentThread().name}) finished ${moduleData.qualifiedName}")
        synchronized(this) {
            if (result == ExitCode.OK) {
                totalPassResult += resultTime
            }

            return handleResult(result, moduleData, collector, manager.getTargetInfo())
        }
    }

    override fun processModules(modules: List<ModuleData>) {
        val resultSeq = modules
            .map { it to executorService.submit(Callable { processModule(it) }) }
            .progress(step = 0.0) { (module, _) -> "Analyzing ${module.qualifiedName}" }
            .asSequence()
            .map { (_, future) -> future.get() }
        for (result in resultSeq) {
            if (result.stop()) {
                break
            }
        }
    }

    override fun afterAllPasses() {
        super.afterAllPasses()
        executorService.shutdown()
    }
}