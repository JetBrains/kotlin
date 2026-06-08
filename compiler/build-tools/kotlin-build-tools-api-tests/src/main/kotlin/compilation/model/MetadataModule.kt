/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.buildtools.api.metadata.metadataKlibCompilationOperation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.walk

class MetadataModule(
    private val kotlinToolchain: KotlinToolchains,
    val buildSession: KotlinToolchains.BuildSession,
    project: MetadataProject,
    moduleName: String,
    moduleDirectory: Path,
    dependencies: List<Dependency>,
    defaultStrategyConfig: ExecutionPolicy,
    moduleCompilationConfigAction: (KotlinMetadataKlibCompilationOperation.Builder) -> Unit = {},
    private val stdlibLocation: List<Path>,
) : AbstractModule<KotlinMetadataKlibCompilationOperation, KotlinMetadataKlibCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
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
        compilationConfigAction: (KotlinMetadataKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (KotlinMetadataKlibCompilationOperation) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult {
        val allowedExtensions = setOf("kt", "kts", "java", "greet")

        val compilationOperation = kotlinToolchain.metadata.metadataKlibCompilationOperation(
            sourcesDirectory.walk()
                .filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                .toList(),
            outputDirectory
        ) {
            moduleCompilationConfigAction(this) // apply module-wide configuration
            compilationConfigAction(this) // apply any overrides for this compilation only
            this.compilerArguments[MetadataArguments.CLASSPATH] = compileClasspath
            when (compilerArguments[MetadataArguments.MODULE_NAME]) {
                null -> compilerArguments[MetadataArguments.MODULE_NAME] = moduleName
                EXPLICIT_NULL_MODULE_NAME_MARKER -> compilerArguments[MetadataArguments.MODULE_NAME] = null
                else -> {}
            }
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
        compilationConfigAction: (KotlinMetadataKlibCompilationOperation.Builder) -> Unit,
        compilationAction: (KotlinMetadataKlibCompilationOperation) -> Unit,
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
