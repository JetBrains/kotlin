/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import java.io.File
import java.util.*

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
     * TODO KT-57565
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
        arguments: List<String>
    ): CompilationResult

    /**
     * A finalization function that must be called when all the modules of the project are compiled.
     *
     * May perform cache clean-ups and resource freeing.
     *
     * @param projectId The unique identifier of the compiled project.
     */
    public fun finishProjectCompilation(projectId: ProjectId)

    @ExperimentalBuildToolsApi
    public companion object {
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): CompilationService =
            loadImplementation(CompilationService::class, classLoader)
    }
}