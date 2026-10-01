/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

@Serializable
internal class JsLinkingOperationImpl(
    override val klib: Path,
    override val destination: Path,
    override val compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
) : BaseCompilationOperationImpl<JsArgumentsImpl, @Contextual K2JSCompilerArguments>(),
    JsLinkingOperation, JsLinkingOperation.Builder,
    DeepCopyable<JsLinkingOperationImpl> {

    override fun toBuilder(): JsLinkingOperation.Builder = deepCopy()

    override fun deepCopy(): JsLinkingOperationImpl {
        return JsLinkingOperationImpl(
            klib,
            destination,
            compilerArguments.deepCopy(),
        ).also { it.copyFrom(this) }
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsLinkingOperation.Option<V>): V =
        JsLinkingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsLinkingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        JsLinkingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun build(): JsLinkingOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V =
        JsLinkingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    private operator fun <V> set(key: Option<V>, value: V) {
        JsLinkingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    override fun getRootProjectDir(): Path? {
        return null
    }

    override fun createAndPrepareCompilerArguments(): K2JSCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.outputDir = destination.absolutePathStringOrThrow()
            compilerArguments.includes = klib.absolutePathStringOrThrow()
            compilerArguments.irProduceJs = true
        }

    override val targetPlatform: CompileService.TargetPlatform = CompileService.TargetPlatform.JS

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2JSCompilerArguments,
    ): IncrementalCompilationOptions? {
        return null
    }

    override fun shouldCompileIncrementally(): Boolean {
        return false
    }

    override fun createCompiler(): CLICompiler<K2JSCompilerArguments> {
        return K2JSCompiler()
    }

    override fun K2JSCompilerArguments.addSources() {}

    override fun compileIncrementallyInProcess(
        arguments: K2JSCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        executionContext: ExecutionContext
    ): CompilationResult {
        error("Linking doesn't support incremental compilation")
    }
}
