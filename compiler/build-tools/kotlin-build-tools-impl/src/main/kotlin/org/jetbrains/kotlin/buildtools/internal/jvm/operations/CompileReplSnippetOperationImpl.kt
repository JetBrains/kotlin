/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplStatelessCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import java.nio.file.Path
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.updateClasspath

/**
 * BTA op implementation that bridges the public [CompileReplSnippetOperation] API to
 * [K2ReplStatelessCompiler], the prototype-level stateless compile entry point.
 *
 *   1. Decode each prior-snippet `ByteArray` with [SnippetArtifactCodec] back into a typed
 *      [SnippetArtifact].
 *   2. Build a per-call [ScriptCompilationConfiguration] (adds [ADDITIONAL_CLASSPATH] entries).
 *   3. Invoke `K2ReplStatelessCompiler.compile(...)` via [internalScriptingRunSuspend] (same
 *      bridge that every existing scripting host uses — `KotlinJsr223ScriptEngineImpl`,
 *      `BasicScriptingHost`, etc. — to avoid depending on `kotlinx-coroutines` from this module).
 *   4. On success, encode the resulting [SnippetArtifact] back to bytes via [SnippetArtifactCodec]
 *      and return it.
 *   5. On failure, throw a `RuntimeException` whose message contains every emitted diagnostic.
 *      The op contract is a `BuildOperation<ByteArray>` (no diagnostic channel today) — diagnostic
 *      streaming is a follow-up iteration; surface it through the exception until the API gets a
 *      structured failure type.
 */
internal class CompileReplSnippetOperationImpl private constructor(
    override val options: Options = Options(CompileReplSnippetOperation::class),
    override val priorSnippets: List<ByteArray>,
    override val snippetSource: String,
    override val snippetName: String,
) : BuildOperationImpl<ByteArray>(),
    CompileReplSnippetOperation,
    CompileReplSnippetOperation.Builder,
    DeepCopyable<CompileReplSnippetOperation> {

    constructor(
        priorSnippets: List<ByteArray>,
        snippetSource: String,
        snippetName: String,
    ) : this(Options(CompileReplSnippetOperation::class), priorSnippets, snippetSource, snippetName) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): ByteArray {
        // KT-84096-equivalent for REPL: daemon execution will be added in a later iteration.
        check(executionPolicy is ExecutionPolicy.InProcess) {
            "Only in-process execution policy is supported for this operation."
        }

        val priors: List<SnippetArtifact> = priorSnippets.map { SnippetArtifactCodec.decode(it) }
        val extraClasspath: List<Path> = this[ADDITIONAL_CLASSPATH]
        val compilationConfig = ScriptCompilationConfiguration {
            if (extraClasspath.isNotEmpty()) {
                updateClasspath(extraClasspath.map { it.toFile() })
            }
        }

        val compiler = K2ReplStatelessCompiler()
        @Suppress("DEPRECATION_ERROR")
        val result: ResultWithDiagnostics<SnippetArtifact> = internalScriptingRunSuspend {
            compiler.compile(
                priorSnippets = priors,
                snippet = snippetSource.toScriptSource(snippetName),
                scriptCompilationConfiguration = compilationConfig,
            )
        }

        return when (result) {
            is ResultWithDiagnostics.Success ->
                SnippetArtifactCodec.encode(result.value)

            is ResultWithDiagnostics.Failure -> {
                val errors = result.reports
                    .filter { it.severity >= ScriptDiagnostic.Severity.ERROR }
                    .joinToString(separator = "\n") { "  ${it.severity}: ${it.message}" }
                throw RuntimeException(
                    "CompileReplSnippetOperation: snippet '$snippetName' failed to compile.\nDiagnostics:\n$errors"
                )
            }
        }
    }

    override fun toBuilder(): CompileReplSnippetOperation.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: CompileReplSnippetOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: CompileReplSnippetOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): CompileReplSnippetOperation = deepCopy()

    override fun deepCopy(): CompileReplSnippetOperationImpl =
        CompileReplSnippetOperationImpl(options.deepCopy(), priorSnippets, snippetSource, snippetName)

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        val ADDITIONAL_CLASSPATH: Option<List<Path>> =
            Option("ADDITIONAL_CLASSPATH", default = emptyList())

        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER", default = DefaultCompilerMessageRenderer)
    }
}
