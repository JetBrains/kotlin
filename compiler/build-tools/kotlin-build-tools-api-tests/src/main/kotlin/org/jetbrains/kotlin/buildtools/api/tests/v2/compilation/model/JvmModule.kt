/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Dependency
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.api.tests.v2.BaseTestV2
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class JvmModule(
    project: Project,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultExecutionPolicy: ExecutionPolicy,
    private val snapshotConfig: SnapshotConfig,
) : AbstractModule(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultExecutionPolicy,
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

    override suspend fun compileImpl(
        executionPolicy: ExecutionPolicy,
        compilationConfigAction: suspend (JvmCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val toolchain = BaseTestV2.kotlinToolchain
        val allowedExtensions = setOf("kt", "kts")

        val compilationOperation = toolchain.jvm.createJvmCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputDirectory
        )
        compilationConfigAction(compilationOperation)
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.NO_REFLECT] = true
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.NO_STDLIB] = true
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.CLASSPATH] = compileClasspath
        compilationOperation.compilerArguments[JvmCompilerArguments.Companion.MODULE_NAME] = moduleName

        compilationOperation[BuildOperation.PROJECT_ID] = project.projectId

//        val defaultCompilationArguments = listOf(
//            "-no-reflect",
//            "-no-stdlib",
//            "-d", outputDirectory.absolutePathString(),
//            "-cp", compileClasspath,
//            "-module-name", moduleName,
//        )
        return toolchain.executeOperation(compilationOperation, executionPolicy, kotlinLogger)
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

    private suspend fun generateClasspathSnapshot(dependency: Dependency): Path {
        val snapshotOperation = BaseTestV2.kotlinToolchain.jvm.createClasspathSnapshottingOperation(
            dependency.location
        )
        snapshotOperation[JvmClasspathSnapshottingOperation.GRANULARITY] = snapshotConfig.granularity
        snapshotOperation[PARSE_INLINED_LOCAL_CLASSES] = snapshotConfig.useInlineLambdaSnapshotting //todo is it the same setting?
        val snapshotResult: JvmClasspathEntrySnapshot = BaseTestV2.kotlinToolchain.executeOperation(snapshotOperation)
        val hash = snapshotResult.classSnapshots.values
            .filterIsInstance<AccessibleClassSnapshot>()
            .withIndex()
            .sumOf { (index, snapshot) -> index * 31 + snapshot.classAbiHash }
        // see details in docs for `CachedClasspathSnapshotSerializer` for details why we can't use a fixed name
        val snapshotFile = icWorkingDir.resolve("dep-$hash.snapshot")
        snapshotFile.createParentDirectories()
        snapshotResult.saveSnapshot(snapshotFile)
        return snapshotFile
    }

    override suspend fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        executionPolicy: ExecutionPolicy,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: suspend (JvmCompilationOperation) -> Unit,
        incrementalCompilationConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration) -> Unit,
        assertions: CompilationOutcome.(module: Module) -> Unit,
    ): CompilationResult {
        return compile(executionPolicy, forceOutput, { compilationOperation ->
            val snapshots = dependencies.map {
                generateClasspathSnapshot(it).toFile()
            }

            val snapshotIcOptions = compilationOperation.createSnapshotBasedIcOptions()
            snapshotIcOptions[MODULE_BUILD_DIR] = buildDirectory
            snapshotIcOptions[ROOT_PROJECT_DIR] = project.projectDirectory
            snapshotIcOptions[PRECISE_JAVA_TRACKING] = false
            snapshotIcOptions[JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES] = false
            snapshotIcOptions[JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY] = true
            snapshotIcOptions[JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER] = false
            snapshotIcOptions[JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS] = setOf(icWorkingDir, outputDirectory)
            snapshotIcOptions[FORCE_RECOMPILATION] = forceNonIncrementalCompilation
            snapshotIcOptions[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = false

            val incrementalConfiguration = JvmSnapshotBasedIncrementalCompilationConfiguration(
                icWorkingDir,
                sourcesChanges,
                snapshots.map { it.toPath() },
                icWorkingDir.resolve("shrunk-classpath-snapshot.bin"),
                snapshotIcOptions
            )

            incrementalCompilationConfigAction(incrementalConfiguration)
            compilationOperation[JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION] = incrementalConfiguration
//            compilationOperation.useIncrementalCompilation(
//                icCachesDir.toFile(), // todo what about the cachesdir? where to put it?
//                sourcesChanges,
//                params,
//                options,
//            )
            compilationConfigAction(compilationOperation)
        }, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
//        if (additionalCompilationArguments.contains("-cp")) {
//            throw UnsupportedOperationException(
//                "additional classpath support isn't implemented for JvmModule.executeCompiledClass"
//            )
//        }

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
