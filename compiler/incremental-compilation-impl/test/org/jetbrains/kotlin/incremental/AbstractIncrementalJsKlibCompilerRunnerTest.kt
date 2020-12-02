package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import java.io.File

abstract class AbstractIncrementalJsKlibCompilerRunnerTest : AbstractIncrementalJsCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            libraries = "build/js-ir-runtime/full-runtime.klib"
            outputFile = File(destinationDir, "${testDir.name}.$KLIB_FILE_EXTENSION").path
            sourceMap = false
            irProduceKlibDir = false
            irProduceKlibFile = true
            irOnly = true
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsIrEnabled = true, isKlibEnabled = true)
}

abstract class AbstractIncrementalJsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalJsKlibCompilerRunnerTest() {
    override val scopeExpansionMode = CompileScopeExpansionMode.ALWAYS
}