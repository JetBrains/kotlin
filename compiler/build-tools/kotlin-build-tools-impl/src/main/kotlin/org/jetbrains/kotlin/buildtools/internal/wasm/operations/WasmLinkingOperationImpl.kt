/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.wasm.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.WasmArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

internal class WasmLinkingOperationImpl(
    override val klib: Path,
    override val destination: Path,
    override val compilerArguments: WasmArgumentsImpl = WasmArgumentsImpl(),
) : BaseCompilationOperationImpl<WasmArgumentsImpl, KotlinWasmCompilerArguments>(),
    WasmLinkingOperation, WasmLinkingOperation.Builder,
    DeepCopyable<WasmLinkingOperationImpl> {

    override fun toBuilder(): WasmLinkingOperation.Builder = deepCopy()

    override fun deepCopy(): WasmLinkingOperationImpl {
        return WasmLinkingOperationImpl(
            klib,
            destination,
            compilerArguments.deepCopy(),
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: WasmLinkingOperation.Option<V>): V =
        WasmLinkingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: WasmLinkingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        WasmLinkingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun build(): WasmLinkingOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V =
        WasmLinkingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    private operator fun <V> set(key: Option<V>, value: V) {
        WasmLinkingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

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
        executionContext: ExecutionContext
    ): CompilationResult {
        error("Linking doesn't support incremental compilation")
    }
}
