/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain.Companion.native
import org.jetbrains.kotlin.buildtools.api.konan.nativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.walk

@OptIn(DelicateBuildToolsApi::class)
class NativeModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: NativeProject,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    moduleCompilationConfigAction: (NativeKlibCompilationOperation.Builder) -> Unit = {},
    private val stdlibLocation: List<Path>,
) : AbstractModule<NativeKlibCompilationOperation, NativeKlibCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    project,
    moduleName,
    moduleDirectory,
    dependencies,
    defaultStrategyConfig,
    moduleCompilationConfigAction,
) {
    private val dependencyFiles: List<Path>
        get() = dependencies.map { it.location }.plus(stdlibLocation)

    /**
     * The klib the compilation produces. It is passed as the operation destination and used by dependent modules.
     */
    val outputKlib: Path
        get() = outputDirectory.resolve("$moduleName.klib")

    override val location: Path
        get() = outputKlib

    override fun compileImpl(
        strategyConfig: ExecutionPolicy,
        compilationConfigAction: (NativeKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (NativeKlibCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val allowedExtensions = setOf("kt")

        val compilationOperation = kotlinToolchain.native.nativeKlibCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputKlib
        ) {
            moduleCompilationConfigAction(this) // apply module-wide configuration
            compilationConfigAction(this) // apply any overrides for this compilation only
            // TODO: K/N compiler requires KOTLIN_HOME to be passed even if it's actually unused. Pass some directory here yet.
            compilerArguments[KOTLIN_HOME] = moduleDirectory
            compilerArguments.applyCommandLineArguments(
                buildList {
                    add("-nostdlib")
                    add("-no-default-libs")
                    // When K/N is disabled in this build, the stdlib klib comes from the (older) bootstrap distribution,
                    // and the compiler would refuse it otherwise: "The Kotlin/Native standard library has an older version".
                    add("-Xskip-library-special-compatibility-checks")
                    add("-module-name")
                    add(moduleName)
                    for (library in dependencyFiles) {
                        add("-library")
                        add(library.absolutePathString())
                    }
                }
            )
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
        compilationConfigAction: (NativeKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (NativeKlibCompilationOperation) -> Unit,
        icOptionsConfigAction: (BaseIncrementalCompilationConfiguration.Builder) -> Unit,
        assertions: context(ModuleContext) CompilationOutcome.() -> Unit,
    ): CompilationResult {
        error("Kotlin/Native modules don't support incremental compilation")
    }

    override fun prepareExecutionProcessBuilder(
        mainClassFqn: String,
    ): ProcessBuilder {
        throw UnsupportedOperationException("Execution of compiled Kotlin/Native modules is not supported directly")
    }
}
