/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class JvmModule(
    private val compilationService: CompilationService,
    project: Project,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    private val defaultStrategyConfig: CompilerExecutionStrategyConfiguration,
    private val snapshotConfig: SnapshotConfig,
    overrides: Module.Overrides,
) : AbstractModule<(JvmCompilationConfiguration) -> Unit>(
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
        compilationConfigAction: ((JvmCompilationConfiguration) -> Unit)?,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val compilationConfig = compilationService.makeJvmCompilationConfiguration()
        compilationConfigAction?.let { it(compilationConfig) }
        compilationConfig.useLogger(kotlinLogger)
        val defaultCompilationArguments = listOf(
            "-no-reflect",
            "-no-stdlib",
            "-d", outputDirectory.absolutePathString(),
            "-cp", compileClasspath,
            "-module-name", moduleName,
        ) + buildList {
            overrides.useFirIc?.let { add("-Xuse-fir-ic") }
            overrides.languageVersion?.let { add("-language-version=${it.stringValue}") }

        }
        val allowedExtensions = compilationConfig.kotlinScriptFilenameExtensions + setOf("kt", "kts")
        return compilationService.compileJvm(
            project.projectId,
            defaultStrategyConfig,
            compilationConfig,
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .map { it.toFile() }
                .toList(),
            defaultCompilationArguments,
        )
    }

    private fun generateClasspathSnapshot(dependency: Dependency): Path {
        val snapshot = compilationService.calculateClasspathSnapshot(
            dependency.location.toFile(),
            snapshotConfig.granularity,
            snapshotConfig.useInlineLambdaSnapshotting,
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

    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        assertions: CompilationOutcome.(Module) -> Unit,
    ): CompilationResult {
        return compile(forceOutput, { compilationConfig ->
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

            if (forceNonIncrementalCompilation) {
                options.forceNonIncrementalMode()
            }

            overrides.keepIncrementalCompilationCachesInMemory?.let { options.keepIncrementalCompilationCachesInMemory(it) }
            overrides.useFirRunner?.let { options.useFirRunner(it) }

            compilationConfig.useIncrementalCompilation(
                icCachesDir.toFile(),
                sourcesChanges,
                params,
                options,
            )
//            compilationConfigAction(compilationConfig)
        }, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
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
