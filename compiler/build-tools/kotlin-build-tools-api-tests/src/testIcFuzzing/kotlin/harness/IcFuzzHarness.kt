/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.harness

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

/**
 * The result of building the same sources with one of the modules of [IcFuzzHarness].
 *
 * @property errors the error messages with the module-specific paths replaced by [SOURCES_PLACEHOLDER]
 * @property icFailures the warnings reporting that the incremental compilation failed and fell back to a non-incremental one
 * @property compiledSources the files compiled by each IC iteration, as reported in the debug log
 */
class BuildSnapshot(
    val result: CompilationResult?,
    val crash: Throwable?,
    val errors: Set<String>,
    val icFailures: List<String>,
    val compiledSources: List<String>,
    val outputs: Map<String, ByteArray>,
    val outputDirectory: Path,
) {
    val isSuccessful: Boolean get() = result == CompilationResult.COMPILATION_SUCCESS

    val outcome: String get() = crash?.let { "crashed with $it" } ?: result.toString()

    companion object {
        const val SOURCES_PLACEHOLDER = "<src>"
    }
}

class StepBuilds(val incremental: BuildSnapshot, val rebuild: BuildSnapshot)

/** How the incremental build learns about the source changes. */
enum class SourcesChangesMode {
    /** Reported as [SourcesChanges.Known], the first build is a rebuild with [SourcesChanges.Unknown]. */
    KNOWN,

    /** Calculated by the incremental compilation, [SourcesChanges.ToBeCalculated] from the first build on, like the scenario DSL does. */
    CALCULATED,

    /** Calculated by the incremental compilation after a first rebuild with [SourcesChanges.Unknown]. */
    CALCULATED_AFTER_REBUILD,
}

/**
 * Builds the same sequence of source states twice: incrementally in one module, and from scratch in another one.
 *
 * Source changes are reported to the incremental build as [SourcesChanges.Known], like the Kotlin Gradle plugin does,
 * or calculated by the incremental compilation itself, see [sourcesChangesMode]. Its default is set with the
 * `kotlin.ic.fuzz.sourcesChanges` system property: `known`, `calculated` or `calculatedAfterRebuild`.
 * As with the scenario DSL, the changes are accumulated until an incremental build succeeds.
 *
 * Both builds run in process or, with `kotlin.ic.fuzz.strategy=daemon`, in the Kotlin daemon, with `-jvm-target 17`
 * and the compiler arguments of the space-separated `kotlin.ic.fuzz.compilerArgs` system property.
 */
class IcFuzzHarness(
    workingDirectory: Path,
    private val sourcesChangesMode: SourcesChangesMode = defaultSourcesChangesMode,
) : AutoCloseable {
    private val incrementalProject = JvmProject(toolchains, executionPolicy, workingDirectory.resolve("incremental"))
    private val rebuildProject = JvmProject(toolchains, executionPolicy, workingDirectory.resolve("rebuild"))
    private val incrementalModule: JvmModule = incrementalProject.module(TEMPLATE_MODULE)
    private val rebuildModule: JvmModule = rebuildProject.module(TEMPLATE_MODULE)

    private var currentSources: Map<String, String> = emptyMap()
    private var isFirstBuild = true
    private val pendingModified = linkedSetOf<String>()
    private val pendingRemoved = linkedSetOf<String>()

    val sources: Map<String, String> get() = currentSources

    /**
     * Brings the sources to [sources] and builds them both incrementally and from scratch.
     *
     * @param touchedFiles files reported as modified even when their content is unchanged
     * @param sourcesChangesOverride the changes to report to the incremental build instead of the actual ones, used to check the oracle
     */
    fun build(
        sources: Map<String, String>,
        touchedFiles: Set<String> = emptySet(),
        sourcesChangesOverride: SourcesChanges? = null,
    ): StepBuilds {
        val changed = sources.filter { [name, text] -> currentSources[name] != text || name in touchedFiles }.keys
        val removed = currentSources.keys - sources.keys
        writeSources(incrementalModule, changed, removed, sources)
        pendingModified += changed
        pendingModified -= removed
        pendingRemoved += removed
        pendingRemoved -= changed
        currentSources = sources

        val sourcesChanges = when {
            sourcesChangesOverride != null -> sourcesChangesOverride
            sourcesChangesMode == SourcesChangesMode.CALCULATED -> SourcesChanges.ToBeCalculated
            isFirstBuild -> SourcesChanges.Unknown
            sourcesChangesMode == SourcesChangesMode.CALCULATED_AFTER_REBUILD -> SourcesChanges.ToBeCalculated
            else -> SourcesChanges.Known(
                modifiedFiles = pendingModified.map { incrementalModule.sourceFile(it) },
                removedFiles = pendingRemoved.map { incrementalModule.sourceFile(it) },
            )
        }
        isFirstBuild = false
        val incremental = compile(incrementalModule, sourcesChanges)
        if (incremental.isSuccessful) {
            pendingModified.clear()
            pendingRemoved.clear()
        }

        @OptIn(ExperimentalPathApi::class)
        rebuildModule.moduleDirectory.deleteRecursively()
        rebuildModule.sourcesDirectory.createDirectories()
        writeSources(rebuildModule, sources.keys, emptySet(), sources)
        val rebuild = compile(rebuildModule, SourcesChanges.Unknown)

        return StepBuilds(incremental, rebuild)
    }

    private fun JvmModule.sourceFile(name: String): File = sourcesDirectory.resolve(name).toFile()

    private fun writeSources(module: JvmModule, changed: Set<String>, removed: Set<String>, sources: Map<String, String>) {
        for (name in removed) {
            module.sourcesDirectory.resolve(name).deleteIfExists()
        }
        for (name in changed) {
            module.sourcesDirectory.resolve(name).writeText(sources.getValue(name))
        }
    }

    private fun compile(module: JvmModule, sourcesChanges: SourcesChanges): BuildSnapshot {
        var outcome: CompilationOutcome? = null
        var crash: Throwable? = null
        val result = try {
            module.compileIncrementally(
                sourcesChanges,
                compilationConfigAction = { it.compilerArguments.applyCommandLineArguments(compilerArgs) },
            ) {
                expectCompilationResult(actualResult)
                outcome = this
            }
        } catch (e: Throwable) {
            crash = e
            null
        }
        val logLines = outcome?.logLines.orEmpty()
        val sourcesPaths = listOf(module.sourcesDirectory.absolute(), module.sourcesDirectory.toRealPath())
            .flatMap { listOf(it.toUri().toString().removeSuffix("/"), it.pathString) }
            .distinct()
        fun String.normalizePaths() = sourcesPaths.fold(this) { acc, path -> acc.replace(path, BuildSnapshot.SOURCES_PLACEHOLDER) }
        return BuildSnapshot(
            result = result,
            crash = crash,
            errors = logLines[LogLevel.ERROR].orEmpty().map { it.normalizePaths() }.toSet(),
            icFailures = logLines[LogLevel.WARN].orEmpty().filter { IC_FAILURE_MARKER in it },
            compiledSources = logLines[LogLevel.DEBUG].orEmpty()
                .map { it.removePrefix("[KOTLIN] ") }
                .filter { it.startsWith(COMPILE_ITERATION_PREFIX) }
                .map { it.removePrefix(COMPILE_ITERATION_PREFIX).normalizePaths() },
            outputs = readOutputs(module.outputDirectory),
            outputDirectory = module.outputDirectory,
        )
    }

    private fun readOutputs(outputDirectory: Path): Map<String, ByteArray> {
        if (!outputDirectory.exists()) return emptyMap()
        @OptIn(ExperimentalPathApi::class)
        return outputDirectory.walk()
            .filter { it.isRegularFile() }
            .associate { it.relativeTo(outputDirectory).invariantSeparatorsPathString to it.readBytes() }
            .toSortedMap()
    }

    override fun close() {
        incrementalProject.close()
        rebuildProject.close()
    }

    companion object {
        private const val TEMPLATE_MODULE = "empty"
        private const val IC_FAILURE_MARKER = "Incremental compilation was attempted but failed"
        private const val COMPILE_ITERATION_PREFIX = "compile iteration: "

        private val defaultSourcesChangesMode = when (System.getProperty("kotlin.ic.fuzz.sourcesChanges")) {
            "calculated" -> SourcesChangesMode.CALCULATED
            "calculatedAfterRebuild" -> SourcesChangesMode.CALCULATED_AFTER_REBUILD
            else -> SourcesChangesMode.KNOWN
        }
        /** The programs may use `@JvmRecord`, which needs the JVM target 16 at least. */
        private val compilerArgs = listOf("-jvm-target", "17") +
                System.getProperty("kotlin.ic.fuzz.compilerArgs").orEmpty().split(' ').filter(String::isNotBlank)

        val toolchains: KotlinToolchains by lazy { KotlinToolchains.loadImplementation(btaClassloader) }
        private val executionPolicy: ExecutionPolicy by lazy {
            if (System.getProperty("kotlin.ic.fuzz.strategy") == "daemon") toolchains.daemonExecutionPolicyBuilder().build()
            else toolchains.createInProcessExecutionPolicy()
        }
    }
}
