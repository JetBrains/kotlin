package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import java.io.File

abstract class AbstractIncrementalK1JsKlibCompilerRunnerTest : AbstractIncrementalK1JsLegacyCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            libraries = "build/js-ir-runtime/full-runtime.klib"
            outputDir = destinationDir.path
            moduleName = testDir.name
            outputFile = null
            sourceMap = false
            irProduceKlibDir = false
            irProduceKlibFile = true
            irOnly = true
            languageVersion = "1.9"
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isKlibEnabled = true)
}

abstract class AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalK1JsKlibCompilerRunnerTest() {
    override val scopeExpansionMode = CompileScopeExpansionMode.ALWAYS
}

abstract class AbstractIncrementalK2JsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments {
        return super.createCompilerArguments(destinationDir, testDir).apply {
            useK2 = true
            languageVersion = "2.0"
        }
    }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isFirEnabled = true)
}