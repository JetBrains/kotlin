/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.metadata.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.MetadataArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

internal class KotlinMetadataKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(KotlinMetadataKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: MetadataArgumentsImpl = MetadataArgumentsImpl(),
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<MetadataArgumentsImpl, K2MetadataCompilerArguments>(compilerArguments),
    KotlinMetadataKlibCompilationOperation, KotlinMetadataKlibCompilationOperation.Builder,
    DeepCopyable<KotlinMetadataKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: MetadataArgumentsImpl = MetadataArgumentsImpl(),
        compilerVersion: String,
    ) : this(
        options = Options(KotlinMetadataKlibCompilationOperation::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        compilerVersion = compilerVersion,
    ) {
        initializeOptions(this::class, options)
    }

    override fun toBuilder(): KotlinMetadataKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): KotlinMetadataKlibCompilationOperationImpl {
        return KotlinMetadataKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            compilerVersion
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: KotlinMetadataKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: KotlinMetadataKlibCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): KotlinMetadataKlibCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun getRootProjectDir(): Path? {
        return null
    }

    override fun createAndPrepareCompilerArguments(): K2MetadataCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.metadataKlib = true
            compilerArguments.destination = destination.absolutePathStringOrThrow()
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.METADATA

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2MetadataCompilerArguments,
    ): IncrementalCompilationOptions? {
        return null
    }

    override fun shouldCompileIncrementally(): Boolean {
        return false
    }

    override fun createCompiler(): CLICompiler<K2MetadataCompilerArguments> {
        return KotlinMetadataCompiler()
    }

    override fun K2MetadataCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2MetadataCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        error("Metadata compiler doesn't support incremental compilation")
    }

    companion object
}
