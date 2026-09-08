package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import java.io.File

abstract class AbstractIncrementalJsKlibCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JSCompilerArguments>() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            libraries = ForTestCompileRuntime.stdlibJs().absolutePath
            outputDir = destinationDir.path
            moduleName = testDir.name
            sourceMap = false
            languageVersion = LanguageVersion.LATEST_STABLE.versionString
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(
            isFirEnabled = true,
            isKlibEnabled = true,
            isJsEnabled = true,
            isScopeExpansionEnabled = scopeExpansionMode != CompileScopeExpansionMode.NEVER,
        )

    override fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: K2JSCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = MessageCollectorImpl()
        makeJsIncrementally(cacheDir, sourceRoots, args, buildHistoryFile(cacheDir), messageCollector, reporter, scopeExpansionMode)
        return TestCompilationResult(reporter, messageCollector)
    }

    protected open val scopeExpansionMode = CompileScopeExpansionMode.NEVER

    override fun failFile(testDir: File): File = testDir.resolve("fail_js_k2.txt")
}

abstract class AbstractIncrementalJsKlibCompilerWithScopeExpansionRunnerTest : AbstractIncrementalJsKlibCompilerRunnerTest() {
    override val scopeExpansionMode = CompileScopeExpansionMode.ALWAYS
}
