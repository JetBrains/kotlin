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
            irBackend = true
            irProduceOnly = "klib"
        }

    override fun rebuildAndCompareOutput(sourceRoots: List<File>, testDir: File, buildLogSteps: List<BuildStep>, outDir: File) {
        // todo fix
    }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsIrEnabled = true)
}