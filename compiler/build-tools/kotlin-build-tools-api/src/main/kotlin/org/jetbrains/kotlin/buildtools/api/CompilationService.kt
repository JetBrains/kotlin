/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import java.io.File

/**
 * A facade for invoking compilation and related stuff (such as [calculateClasspathSnapshot]) in Kotlin compiler.
 *
 * An example of the basic usage is:
 * ```
 *  val service = CompilationService.loadImplementation(ClassLoader.getSystemClassLoader())
 *  val executionConfig = service.makeCompilerExecutionStrategyConfiguration()
 *  val compilationConfig = service.makeJvmCompilationConfiguration()
 *  service.compileJvm(executionConfig, compilationConfig, listOf(File("src/a.kt")), listOf("-Xexplicit-api=strict"))
 * ```
 *
 * This interface is not intended to be implemented by the API consumers. An instance of [CompilationService] is expected to be obtained from [loadImplementation].
 */
@ExperimentalBuildToolsApi
public interface CompilationService {
    /**
     * Calculates JVM classpath snapshot for [classpathEntry] used for detecting changes in incremental compilation with specified [granularity].
     *
     * The [ClassSnapshotGranularity.CLASS_LEVEL] granularity should be preferred for rarely changing dependencies as more lightweight in terms of the resulting snapshot size.
     *
     * @param classpathEntry path to existent classpath entry
     * @param granularity determines granularity of tracking.
     */
    public fun calculateClasspathSnapshot(classpathEntry: File, granularity: ClassSnapshotGranularity): ClasspathEntrySnapshot

    /**
     * Provides a default [CompilerExecutionStrategyConfiguration] allowing to use it as is or customizing for specific requirements.
     * Could be used as an overview to default values of the options (as they are implementation-specific).
     */
    public fun makeCompilerExecutionStrategyConfiguration(): CompilerExecutionStrategyConfiguration

    /**
     * Provides a default [CompilerExecutionStrategyConfiguration] allowing to use it as is or customizing for specific requirements.
     * Could be used as an overview to default values of the options (as they are implementation-specific).
     */
    public fun makeJvmCompilationConfiguration(): JvmCompilationConfiguration

    /**
     * Compiles Kotlin code targeting JVM platform and using specified options.
     *
     * The [finishProjectCompilation] must be called with the same [projectId] after the entire project is compiled.
     * @param projectId The unique identifier of the project to be compiled. It may be the same for different modules of the project.
     * @param strategyConfig an instance of [CompilerExecutionStrategyConfiguration] initially obtained from [makeCompilerExecutionStrategyConfiguration]
     * @param compilationConfig an instance of [JvmCompilationConfiguration] initially obtained from [makeJvmCompilationConfiguration]
     * @param sources a list of all sources of the compilation unit
     * @param arguments a list of Kotlin JVM compiler arguments
     */
    public fun compileJvm(
        projectId: ProjectId,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfig: JvmCompilationConfiguration,
        sources: List<File>,
        arguments: List<String>,
    ): CompilationResult

    /**
     * A finalization function that must be called when all the modules of the project are compiled.
     *
     * May perform cache clean-ups and resource freeing.
     *
     * @param projectId The unique identifier of the compiled project.
     */
    public fun finishProjectCompilation(projectId: ProjectId)

    /**
     * Retrieves the custom Kotlin script filename extensions based on script definitions from the specified classpath.
     *
     * @param classpath The list of files representing the classpath.
     * @return A collection of strings representing the custom Kotlin script filename extensions.
     */
    public fun getCustomKotlinScriptFilenameExtensions(classpath: List<File>): Collection<String>

    /**
     * Returns the version of the Kotlin compiler used to run compilation.
     *
     * @return A string representing the version of the Kotlin compiler, for example `2.0.0-Beta4`.
     */
    public fun getCompilerVersion(): String

    @ExperimentalBuildToolsApi
    public companion object {
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): CompilationService =
            loadImplementation(CompilationService::class, classLoader)
    }
}