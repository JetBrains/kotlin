/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import java.nio.file.Path

@OptIn(ExperimentalCompilerArgument::class)
class JsModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    override val project: JsProject,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    moduleCompilationConfigAction: (BaseCompilationOperation.Builder) -> Unit = {},
    private val stdlibKlibLocation: List<Path>,
    private val registeredModules: Set<JsModule> = mutableSetOf(),
) : AbstractModule<BaseCompilationOperation, BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    moduleCompilationConfigAction,
), LinkableModule<BaseCompilationOperation, BaseCompilationOperation.Builder> {

    var lastCompileProducedPackedKlib = false

    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibKlibLocation)
    override val expectedOutputFileName: String
        get() = "$moduleName.js"

    override fun link(
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        compilationConfigAction: (BaseCompilationOperation.Builder) -> Unit,
        compilationAction: (BaseCompilationOperation) -> Unit,
        assertions: context(ModuleContext) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        error("Not implemented")
    }

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
        error("Not implemented")
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        // JS modules produce KLib files and cannot be executed directly like JVM classes.
        throw UnsupportedOperationException("Execution of compiled JS modules is not supported directly")
    }
}
