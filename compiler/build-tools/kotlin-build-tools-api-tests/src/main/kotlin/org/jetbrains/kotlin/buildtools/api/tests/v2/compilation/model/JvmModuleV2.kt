/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.*
import org.jetbrains.kotlin.buildtools.api.v2.*
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.io.path.walk

class JvmModuleV2(
    private val toolchain: KotlinToolchain,
    project: Project,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    private val defaultExecutionPolicy: ExecutionPolicy,
    private val snapshotConfig: SnapshotConfig,
    overrides: Module.Overrides,
) : AbstractModule<(JvmCompilationOperation) -> Unit>(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    overrides
) {
    private val stdlibLocation: Path =
        KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath() // compile against the provided stdlib

    /**
     * It won't be a problem to cache [dependencyFiles] and [compileClasspath] currently,
     * but we might add tests where dependencies change between compilations
     */
    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plusElement(stdlibLocation)
    private val compileClasspath: String
        get() = dependencyFiles.joinToString(File.pathSeparator)

    override fun compileImpl(
        compilationConfigAction: ((JvmCompilationOperation) -> Unit)?,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val allowedExtensions = setOf("kt", "kts")

        val compilationOperation = toolchain.jvm.createJvmCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputDirectory
        )
        compilationConfigAction?.let { it(compilationOperation) }
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.NO_REFLECT] = true
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.NO_STDLIB] = true
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.CLASSPATH] = compileClasspath
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.MODULE_NAME] = moduleName

        overrides.useFirIc?.let { compilationOperation.compilerArguments[CommonCompilerArguments.Companion.X_USE_FIR_IC] = it }
        overrides.languageVersion?.let { compilationOperation.compilerArguments[CommonCompilerArguments.Companion.LANGUAGE_VERSION] = it }

        compilationOperation[BuildOperation.PROJECT_ID] = project.projectId

        return runBlocking { toolchain.executeOperation(compilationOperation, defaultExecutionPolicy, kotlinLogger) }
//            project.projectId,
//            executionPolicy,
//            compilationConfig,
//            sourcesDirectory.walk()
//                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
//                .map { it.toFile() }
//                .toList(),
//            defaultCompilationArguments + additionalCompilationArguments,
//        )
    }

    private fun generateClasspathSnapshot(dependency: Dependency): Path {
        val snapshotOperation = toolchain.jvm.createClasspathSnapshottingOperation(
            dependency.location
        )
        snapshotOperation[JvmClasspathSnapshottingOperation.GRANULARITY] = snapshotConfig.granularity
        snapshotOperation[PARSE_INLINED_LOCAL_CLASSES] = snapshotConfig.useInlineLambdaSnapshotting //todo is it the same setting?
        val snapshotResult = runBlocking { toolchain.executeOperation(snapshotOperation) }
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
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        assertions: CompilationOutcome.(Module) -> Unit,
    ): CompilationResult {
        return compile(forceOutput, { compilationOperation ->
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

            overrides.keepIncrementalCompilationCachesInMemory?.let { snapshotIcOptions[KEEP_IC_CACHES_IN_MEMORY] = it }
            overrides.useFirRunner?.let { snapshotIcOptions[USE_FIR_RUNNER] = it }

            compilationOperation[JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION] = incrementalConfiguration

//            compilationConfigAction(compilationOperation)
        }, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        val executionClasspath = "${compileClasspath}${File.pathSeparator}${outputDirectory}"

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
