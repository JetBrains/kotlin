/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path


internal class JsLinkingOperationImpl : BaseCompilationOperationImpl<JsArgumentsImpl, K2JSCompilerArguments>,
    JsLinkingOperation, JsLinkingOperation.Builder,
    DeepCopyable<JsLinkingOperationImpl> {
    override val options: Options
    override val klib: Path
    override val destination: Path
    override val compilerArguments: JsArgumentsImpl

    private constructor(other: JsLinkingOperationImpl) : super(other) {
        options = other.options.deepCopy()
        klib = other.klib
        destination = other.destination
        compilerArguments = other.compilerArguments.deepCopy()
    }

    constructor(
        klib: Path,
        destination: Path,
        compilerArguments: JsArgumentsImpl = JsArgumentsImpl(),
    ) : super() {
        this.options = Options(JsLinkingOperation::class)
        this.klib = klib
        this.destination = destination
        this.compilerArguments = compilerArguments
        initializeOptions(this::class, options)
    }

    override fun toBuilder(): JsLinkingOperation.Builder = deepCopy()

    override fun deepCopy(): JsLinkingOperationImpl {
        return JsLinkingOperationImpl(this)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsLinkingOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsLinkingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): JsLinkingOperation = deepCopy()

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

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
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext
    ): CompilationResult {
        error("Linking doesn't support incremental compilation")
    }
}
