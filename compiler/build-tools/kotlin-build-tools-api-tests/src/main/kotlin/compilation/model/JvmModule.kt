/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.io.path.walk

class JvmModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: Project,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    private val snapshotConfig: SnapshotConfig,
    moduleCompilationConfigAction: (JvmCompilationOperation) -> Unit = {},
    private val stdlibLocation: List<Path>,
) : AbstractModule(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    moduleCompilationConfigAction,
) {


    /**
     * It won't be a problem to cache [dependencyFiles] and [compileClasspath] currently,
     * but we might add tests where dependencies change between compilations
     */
    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibLocation)
    private val compileClasspath: String
        get() = dependencyFiles.joinToString(File.pathSeparator)

    override fun compileImpl(
        strategyConfig: ExecutionPolicy,
        compilationConfigAction: (JvmCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger
    ): CompilationResult {
        val allowedExtensions = setOf("kt", "kts", "java")

        val compilationOperation = kotlinToolchain.jvm.createJvmCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputDirectory
        )
        moduleCompilationConfigAction(compilationOperation) // apply module-wide configuration
        compilationConfigAction(compilationOperation) // apply any overrides for this compilation only
        compilationOperation.compilerArguments[NO_REFLECT] = true
        compilationOperation.compilerArguments[NO_STDLIB] = true
        compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
        compilationOperation.compilerArguments[MODULE_NAME] = moduleName

        return buildSession.executeOperation(compilationOperation, strategyConfig, kotlinLogger)
    }

    private fun generateClasspathSnapshot(dependency: Dependency): Path {
        val snapshotOperation = kotlinToolchain.jvm.createClasspathSnapshottingOperation(
            dependency.location
        )
        snapshotOperation[JvmClasspathSnapshottingOperation.GRANULARITY] = snapshotConfig.granularity
        snapshotOperation[PARSE_INLINED_LOCAL_CLASSES] = snapshotConfig.useInlineLambdaSnapshotting
        val snapshotResult = buildSession.executeOperation(snapshotOperation)
        val hash = snapshotResult.classSnapshots.values
            .filterIsInstance<AccessibleClassSnapshot>()
            .withIndex()
            .sumOf { (index, snapshot) -> index * 31 + snapshot.classAbiHash }
        // see details in docs for `CachedClasspathSnapshotSerializer` for details why we can't use a fixed name
        val snapshotFile = icWorkingDir.resolve("dep-$hash.snapshot")
        snapshotFile.createParentDirectories()
        snapshotResult.saveSnapshot(snapshotFile.toFile())
        return snapshotFile
    }

    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: (JvmCompilationOperation) -> Unit,
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationOptions) -> Unit,
        assertions: CompilationOutcome.(Module) -> Unit,
    ): CompilationResult {
        return compile(strategyConfig, forceOutput, { compilationOperation ->
            val snapshots = dependencies.map {
                generateClasspathSnapshot(it).toFile()
            }

            val snapshotIcOptions = compilationOperation.createSnapshotBasedIcOptions()
            snapshotIcOptions[MODULE_BUILD_DIR] = buildDirectory
            snapshotIcOptions[ROOT_PROJECT_DIR] = project.projectDirectory
            snapshotIcOptions[FORCE_RECOMPILATION] = forceNonIncrementalCompilation

            val incrementalConfiguration = JvmSnapshotBasedIncrementalCompilationConfiguration(
                icCachesDir,
                sourcesChanges,
                snapshots.map { it.toPath() },
                icWorkingDir.resolve("shrunk-classpath-snapshot.bin"),
                snapshotIcOptions
            )

            icOptionsConfigAction(incrementalConfiguration.options)

            compilationOperation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = incrementalConfiguration
            compilationConfigAction(compilationOperation)
        }, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String
    ): ProcessBuilder {
        val executionClasspath = "$compileClasspath${File.pathSeparator}${outputDirectory}"

        val builder = ProcessBuilder(
            javaExe.absolutePath, // it is possible to support jdk selection, but we don't need it yet
            "-cp", executionClasspath,
            mainClassFqn
        )
        builder.directory(outputDirectory.toFile())

        return builder
    }

    private companion object {
        val javaExe: File
            get() {
                val javaHome = System.getProperty("java.home")
                return File(javaHome, "bin/java.exe").takeIf(File::exists)
                    ?: File(javaHome, "bin/java").takeIf(File::exists)
                    ?: error("Can't find 'java' executable in $javaHome")
            }
    }
}
