package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import java.io.File

abstract class AbstractIncrementalK1JsKlibCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JSCompilerArguments>() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            libraries = "build/js-ir-runtime/full-runtime.klib"
            outputDir = destinationDir.path
            moduleName = testDir.name
            sourceMap = false
            irProduceKlibDir = false
            irProduceKlibFile = true
            irOnly = true
            languageVersion = "1.9"
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(
            isKlibEnabled = true,
            isJsEnabled = true,
            isScopeExpansionEnabled = scopeExpansionMode != CompileScopeExpansionMode.NEVER,
        )

    override fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: K2JSCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        makeJsIncrementally(cacheDir, sourceRoots, args, buildHistoryFile(cacheDir), messageCollector, reporter, scopeExpansionMode)
        return TestCompilationResult(reporter, messageCollector)
    }

    protected open val scopeExpansionMode = CompileScopeExpansionMode.NEVER

    override fun failFile(testDir: File): File = testDir.resolve("fail_js_legacy.txt")
}

abstract class AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalK1JsKlibCompilerRunnerTest() {
    override val scopeExpansionMode = CompileScopeExpansionMode.ALWAYS
}

abstract class AbstractIncrementalK2JsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments {
        return super.createCompilerArguments(destinationDir, testDir).apply {
            languageVersion = "2.0"
        }
    }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isFirEnabled = true)
}