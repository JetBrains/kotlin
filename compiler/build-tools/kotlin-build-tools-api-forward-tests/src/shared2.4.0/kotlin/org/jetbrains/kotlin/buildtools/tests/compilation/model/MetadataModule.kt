/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import java.nio.file.Path

class MetadataModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: MetadataProject,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    moduleCompilationConfigAction: (BaseCompilationOperation.Builder) -> Unit = {},
    private val stdlibLocation: List<Path>,
) : AbstractModule<BaseCompilationOperation, BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
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
    val compileClasspath: List<Path>
        get() = dependencyFiles

    override fun compileImpl(
        strategyConfig: ExecutionPolicy,
        compilationConfigAction: (BaseCompilationOperation.Builder) -> Unit,
        compilationAction: (BaseCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        error("Not implemented")
    }

    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: (BaseCompilationOperation.Builder) -> Unit,
        compilationAction: (BaseCompilationOperation) -> Unit,
        icOptionsConfigAction: (BaseIncrementalCompilationConfiguration.Builder) -> Unit,
        assertions: context(ModuleContext) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        error("doesn't support incremental compilation")
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        throw UnsupportedOperationException("Execution of compiled Metadata modules is not supported directly")
    }
}
