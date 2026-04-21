/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.jsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.jsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.historyBasedIcConfiguration
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.walk

@OptIn(ExperimentalCompilerArgument::class)
class JsModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    override val project: JsProject,
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
), LinkableModule<JsLinkingOperation, JsLinkingOperation.Builder> {

    val otherModules = mutableListOf<JsModule>()
    var lastCompileProducedPackedKlib = false

    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibKlibLocation)

    override fun link(
        strategyConfig: ExecutionPolicy,
        forceOutput: LogLevel?,
        compilationConfigAction: (JsLinkingOperation.Builder) -> Unit,
        compilationAction: (JsLinkingOperation) -> Unit,
        assertions: context(Module<*, *, *>) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        val kotlinLogger = TestKotlinLogger()
        val compilationOperation = kotlinToolchain.js.jsLinkingOperation(
            // handle both cases of NOPACK set to true and false
            if (lastCompileProducedPackedKlib) {
                outputDirectory.resolve("$moduleName.klib")
            } else {
                outputDirectory
            },
            outputDirectory,
        ) {
            compilationConfigAction(this)
            compilerArguments[LIBRARIES] = dependencyFiles
            compilerArguments[IR_OUTPUT_NAME] = moduleName
        }
        val result = compilationOperation.let {
            compilationAction(it)
            buildSession.executeOperation(it, strategyConfig, kotlinLogger)
        }

        processOutcome(kotlinLogger, result, assertions, forceOutput)
        return result
    }

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
            compilerArguments[NOPACK] = true
            moduleCompilationConfigAction(this)
            compilationConfigAction(this)
            compilerArguments[LIBRARIES] = dependencyFiles
            compilerArguments[IR_OUTPUT_NAME] = moduleName

            lastCompileProducedPackedKlib = compilerArguments[NOPACK]
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
        assertions: context(Module<*, *, *>) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        return compile(strategyConfig, forceOutput, { compilationOperation ->
            val modulesInfo = (otherModules + this).map {
                IncrementalModule(
                    it.moduleName,
                    it.outputDirectory,
                    it.buildDirectory,
                    it.icCachesDir,
                )
            }

            modulesInfo.forEach {
                println(it)
            }

            val icConfig = compilationOperation.historyBasedIcConfiguration(
                project.projectDirectory,
                icCachesDir,
                sourcesChanges,
                modulesInfo
            ) {
                this[MODULE_BUILD_DIR] = buildDirectory
                this[FORCE_RECOMPILATION] = forceNonIncrementalCompilation

                icOptionsConfigAction(this)
            }

            compilationOperation[INCREMENTAL_COMPILATION] = icConfig
            compilationConfigAction(compilationOperation)
        }, compilationAction, assertions)
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        // JS modules produce KLib files and cannot be executed directly like JVM classes.
        throw UnsupportedOperationException("Execution of compiled JS modules is not supported directly")
    }
}
