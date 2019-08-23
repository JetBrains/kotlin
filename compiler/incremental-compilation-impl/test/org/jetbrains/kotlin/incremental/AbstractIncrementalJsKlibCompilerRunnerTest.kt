package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import java.io.File

abstract class AbstractIncrementalJsKlibCompilerRunnerTest : AbstractIncrementalJsCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            libraries = "build/js-ir-runtime/full-runtime.klib"
            outputFile = File(destinationDir, "${testDir.name}.klib").path
            sourceMap = true
            // Don't zip klib content since date on files affect the md5 checksum we compute to check whether output files identical
            irProduceKlibDir = true
            irOnly = true
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsIrEnabled = true)
}

abstract class AbstractIncrementalJsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalJsKlibCompilerRunnerTest() {
    override val scopeExpansionMode = CompileScopeExpansionMode.ALWAYS
}