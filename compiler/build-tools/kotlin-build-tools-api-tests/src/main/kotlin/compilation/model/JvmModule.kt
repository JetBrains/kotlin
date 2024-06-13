/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.tests.BaseTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class JvmModule(
    project: Project,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: CompilerExecutionStrategyConfiguration,
    additionalCompilationArguments: List<String> = emptyList(),
) : AbstractModule(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    additionalCompilationArguments,
) {
    override fun compileImpl(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val stdlibLocation =
            KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath() // compile against the provided stdlib
        val dependencyFiles = dependencies.map { it.location }.plusElement(stdlibLocation)
        val compilationConfig = BaseTest.compilationService.makeJvmCompilationConfiguration()
        compilationConfigAction(compilationConfig)
        compilationConfig.useLogger(kotlinLogger)
        val defaultCompilationArguments = listOf(
            "-no-reflect",
            "-no-stdlib",
            "-d", outputDirectory.absolutePathString(),
            "-cp", dependencyFiles.joinToString(File.pathSeparator),
            "-module-name", moduleName,
        )
        val allowedExtensions = compilationConfig.kotlinScriptFilenameExtensions + setOf("kt", "kts")
        return BaseTest.compilationService.compileJvm(
            project.projectId,
            strategyConfig,
            compilationConfig,
            sourcesDirectory.listDirectoryEntries()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .map { it.toFile() },
            defaultCompilationArguments + additionalCompilationArguments,
        )
    }

    private fun generateClasspathSnapshot(dependency: Dependency): Path {
        val snapshot = BaseTest.compilationService.calculateClasspathSnapshot(
            dependency.location.toFile(),
            ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
        )
        val hash = snapshot.classSnapshots.values
            .filterIsInstance<AccessibleClassSnapshot>()
            .withIndex()
            .sumOf { (index, snapshot) -> index * 31 + snapshot.classAbiHash }
        // see details in docs for `CachedClasspathSnapshotSerializer` for details why we can't use a fixed name
        val snapshotFile = icWorkingDir.resolve("dep-$hash.snapshot")
        snapshotFile.createParentDirectories()
        snapshot.saveSnapshot(snapshotFile.toFile())
        return snapshotFile
    }

    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit,
        incrementalCompilationConfigAction: (IncrementalJvmCompilationConfiguration<*>) -> Unit,
        assertions: context(Module) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        return compile(strategyConfig, forceOutput, { compilationConfig ->
            val snapshots = dependencies.map {
                generateClasspathSnapshot(it).toFile()
            }
            val shrunkClasspathSnapshotFile = icWorkingDir.resolve("shrunk-classpath-snapshot.bin")
            val params = ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                snapshots,
                shrunkClasspathSnapshotFile.toFile()
            )

            val options = compilationConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
            options.setBuildDir(buildDirectory.toFile())
            options.setRootProjectDir(project.projectDirectory.toFile())

            if (BaseTest.compilerVersion < KotlinToolingVersion(2, 0, 0, "Beta2")) {
                // workaround for the incorrect default value
                options.useOutputDirs(setOf(icCachesDir.toFile(), outputDirectory.toFile()))
            }

            if (forceNonIncrementalCompilation) {
                options.forceNonIncrementalMode()
            }

            incrementalCompilationConfigAction(options)

            compilationConfig.useIncrementalCompilation(
                icCachesDir.toFile(),
                sourcesChanges,
                params,
                options,
            )
            compilationConfigAction(compilationConfig)
        }, assertions)
    }
}