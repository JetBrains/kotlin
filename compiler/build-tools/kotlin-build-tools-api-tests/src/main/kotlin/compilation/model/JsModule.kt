/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_IR_PRODUCE_KLIB_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_IR_PRODUCE_KLIB_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.jsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.jsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.walk

@OptIn(ExperimentalCompilerArgument::class)
class JsModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: AbstractProject<*, *, *>,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    moduleCompilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit = {},
    private val stdlibKlibLocation: List<Path>,
) : AbstractModule<JsKlibCompilationOperation, JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    moduleCompilationConfigAction,
) {

    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibKlibLocation)

    override fun compileImpl(
        strategyConfig: ExecutionPolicy,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (JsKlibCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val allowedExtensions = setOf("kt")

        val compilationOperation = kotlinToolchain.js.jsKlibCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputDirectory,
        ) {
            moduleCompilationConfigAction(this)
            compilationConfigAction(this)
            compilerArguments[LIBRARIES] = dependencyFiles
            compilerArguments[IR_OUTPUT_NAME] = moduleName
            compilerArguments[X_IR_PRODUCE_KLIB_DIR] = true
        }

        return compilationOperation.let {
            compilationAction(it)
            buildSession.executeOperation(it, strategyConfig, kotlinLogger)
        }
    }

    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (JsKlibCompilationOperation) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
        assertions: context(Module<JsKlibCompilationOperation, JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        // JS incremental compilation uses a different IC mechanism than JVM;
        // for now, delegate to non-incremental compilation
        return compile(strategyConfig, forceOutput, compilationConfigAction, compilationAction, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        // JS modules produce KLib files and cannot be executed directly like JVM classes.
        throw UnsupportedOperationException("Execution of compiled JS modules is not supported directly")
    }

    fun linkedJsModule(moduleLinkingConfigAction: (JsLinkingOperation.Builder) -> Unit = {},): LinkedJsModule {
        return LinkedJsModule(
            kotlinToolchain,
            buildSession,
            project,
            moduleName,
            moduleDirectory,
            this,
            dependencies,
            defaultStrategyConfig,
            stdlibKlibLocation,
            moduleLinkingConfigAction
        )
    }
}

@OptIn(ExperimentalCompilerArgument::class)
class LinkedJsModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: AbstractProject<*, *, *>,
    moduleName: String,
    moduleDirectory: Path,
    val klib: Dependency,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    private val stdlibKlibLocation: List<Path>,
    moduleCompilationConfigAction: (JsLinkingOperation.Builder) -> Unit = {},
) : AbstractModule<JsLinkingOperation, JsLinkingOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    moduleCompilationConfigAction,
) {

    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibKlibLocation)

    override fun compileImpl(
        strategyConfig: ExecutionPolicy,
        compilationConfigAction: (JsLinkingOperation.Builder) -> Unit,
        compilationAction: (JsLinkingOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {

        val compilationOperation = kotlinToolchain.js.jsLinkingOperation(
            klib.location,
            outputDirectory,
        ) {
            moduleCompilationConfigAction(this)
            compilationConfigAction(this)
            compilerArguments[LIBRARIES] = dependencyFiles
            compilerArguments[IR_OUTPUT_NAME] = moduleName
        }

        return compilationOperation.let {
            compilationAction(it)
            buildSession.executeOperation(it, strategyConfig, kotlinLogger)
        }
    }

    override fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        forceNonIncrementalCompilation: Boolean,
        compilationConfigAction: (JsLinkingOperation.Builder) -> Unit,
        compilationAction: (JsLinkingOperation) -> Unit,
        icOptionsConfigAction: (BaseIncrementalCompilationConfiguration.Builder) -> Unit,
        assertions: context(Module<JsLinkingOperation, JsLinkingOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        // linking doesn't have any special config for incremental compilation, just use the regular one
        return compile(strategyConfig, forceOutput, compilationConfigAction, compilationAction, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        // JS modules produce KLib files and cannot be executed directly like JVM classes.
        throw UnsupportedOperationException("Execution of compiled JS modules is not supported directly")
    }
}
