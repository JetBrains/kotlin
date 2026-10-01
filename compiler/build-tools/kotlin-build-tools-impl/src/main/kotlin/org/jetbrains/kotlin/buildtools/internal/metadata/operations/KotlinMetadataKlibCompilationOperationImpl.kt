/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.metadata.operations

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.MetadataArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

@Serializable
internal class KotlinMetadataKlibCompilationOperationImpl(
    override val sources: List<Path>,
    override val destination: Path,
    override val compilerArguments: MetadataArgumentsImpl = MetadataArgumentsImpl(),
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<MetadataArgumentsImpl, @Contextual K2MetadataCompilerArguments>(),
    KotlinMetadataKlibCompilationOperation, KotlinMetadataKlibCompilationOperation.Builder,
    DeepCopyable<KotlinMetadataKlibCompilationOperationImpl> {

    override fun toBuilder(): KotlinMetadataKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): KotlinMetadataKlibCompilationOperationImpl {
        return KotlinMetadataKlibCompilationOperationImpl(
            sources,
            destination,
            compilerArguments.deepCopy(),
            compilerVersion
        ).also { it.copyFrom(this) }
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: KotlinMetadataKlibCompilationOperation.Option<V>): V =
        KotlinMetadataKlibCompilationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: KotlinMetadataKlibCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        KotlinMetadataKlibCompilationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun build(): KotlinMetadataKlibCompilationOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V =
        KotlinMetadataKlibCompilationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    private operator fun <V> set(key: Option<V>, value: V) {
        KotlinMetadataKlibCompilationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

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
        executionContext: ExecutionContext
    ): CompilationResult {
        error("Metadata compiler doesn't support incremental compilation")
    }

    companion object
}
