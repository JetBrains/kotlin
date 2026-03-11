/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.CompilerArgumentValueAdapter
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.BaseCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.toOptions
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.io.File
import java.nio.file.Path

internal class JsKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(JsKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: JsArgumentsImpl = JsArgumentsImpl(
        CompilerArgumentValueAdapter.getOrNull(JsArguments.JsArgument::class)
    ),
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : BaseCompilationOperationImpl<JsArgumentsImpl, K2JSCompilerArguments>(compilerArguments, buildIdToSessionFlagFile),
    JsKlibCompilationOperation, JsKlibCompilationOperation.Builder,
    DeepCopyable<JsKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(
            CompilerArgumentValueAdapter.getOrNull(JsArguments.JsArgument::class)
        ),
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : this(
        options = Options(JsKlibCompilationOperation ::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        buildIdToSessionFlagFile = buildIdToSessionFlagFile
    )

    override fun toBuilder(): JsKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): JsKlibCompilationOperationImpl {
        return JsKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            buildIdToSessionFlagFile
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsKlibCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): JsKlibCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    override fun getRootProjectDir(): Path? {
        return (get(INCREMENTAL_COMPILATION) as? JvmSnapshotBasedIncrementalCompilationConfiguration)?.toOptions()?.get(ROOT_PROJECT_DIR)
    }

    override fun createAndPrepareCompilerArguments(): K2JSCompilerArguments = compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JS

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
    ): IncrementalCompilationOptions? {
        return null
    }

    override fun shouldCompileIncrementally(): Boolean {
        return false
    }

    override fun createCompiler(): CLICompiler<K2JSCompilerArguments> {
        return K2JSCompiler()
    }

    override fun K2JSCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2JSCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        TODO("Not yet implemented")
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)

        val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS", null)
    }
}