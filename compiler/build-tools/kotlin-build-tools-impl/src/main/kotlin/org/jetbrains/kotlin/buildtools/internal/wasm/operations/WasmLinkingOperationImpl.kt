/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.wasm.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.WasmArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

@OptIn(ExperimentalCompilerArgument::class)
internal class WasmLinkingOperationImpl private constructor(
    override val options: Options = Options(WasmLinkingOperation::class),
    override val klib: Path,
    override val destination: Path,
    compilerArguments: WasmArgumentsImpl = WasmArgumentsImpl(),
) : BaseCompilationOperationImpl<WasmArgumentsImpl, KotlinWasmCompilerArguments>(compilerArguments),
    WasmLinkingOperation, WasmLinkingOperation.Builder,
    DeepCopyable<WasmLinkingOperationImpl> {
    constructor(
        klib: Path,
        destination: Path,
        compilerArguments: WasmArgumentsImpl = WasmArgumentsImpl(),
    ) : this(
        options = Options(WasmLinkingOperation::class),
        klib = klib,
        destination = destination,
        compilerArguments = compilerArguments,
    ) {
        initializeOptions(this::class, options)
    }

    override fun toBuilder(): WasmLinkingOperation.Builder = deepCopy()

    override fun deepCopy(): WasmLinkingOperationImpl {
        return WasmLinkingOperationImpl(
            options.deepCopy(),
            klib,
            destination,
            compilerArguments.deepCopy(),
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: WasmLinkingOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: WasmLinkingOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): WasmLinkingOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun getRootProjectDir(): Path? {
        return null
    }

    override fun createAndPrepareCompilerArguments(): KotlinWasmCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
            compilerArguments.includes = klib.absolutePathStringOrThrow()
            compilerArguments.irProduceJs = true
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.WASM

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: KotlinWasmCompilerArguments,
    ): IncrementalCompilationOptions? {
        return null
    }

    override fun shouldCompileIncrementally(): Boolean {
        return false
    }

    override fun createCompiler(): CLICompiler<KotlinWasmCompilerArguments> {
        return KotlinWasmCompiler()
    }

    override fun KotlinWasmCompilerArguments.addSources() {}

    override fun compileIncrementallyInProcess(
        arguments: KotlinWasmCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        error("Linking doesn't support incremental compilation")
    }
}
