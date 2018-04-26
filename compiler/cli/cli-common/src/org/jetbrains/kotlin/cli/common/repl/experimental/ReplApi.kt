/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.repl.experimental

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.repl.*
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass

data class CompiledClassData(val path: String, val bytes: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean =
        (other as? CompiledClassData)?.let { path == it.path && Arrays.equals(bytes, it.bytes) } ?: false

    override fun hashCode(): Int = path.hashCode() + Arrays.hashCode(bytes)

    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

interface CreateReplStageStateAction {
    suspend fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): IReplStageState<*>
}

// --- check

interface ReplCheckAction {
    suspend fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult
}

// --- compile

interface ReplCompileAction {
    suspend fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult
}

interface ReplCompiler : ReplCompileAction, ReplCheckAction, CreateReplStageStateAction

// --- eval

data class EvalClassWithInstanceAndLoader(
    val klass: KClass<*>,
    val instance: Any?,
    val classLoader: ClassLoader,
    val invokeWrapper: InvokeWrapper?
)

interface ReplEvalAction {
    suspend fun eval(
        state: IReplStageState<*>,
        compileResult: ReplCompileResult.CompiledClasses,
        scriptArgs: ScriptArgsWithTypes? = null,
        invokeWrapper: InvokeWrapper? = null
    ): ReplEvalResult
}

interface ReplEvaluator : ReplEvalAction, CreateReplStageStateAction

// --- compileAdnEval

interface ReplAtomicEvalAction {
    suspend fun compileAndEval(
        state: IReplStageState<*>,
        codeLine: ReplCodeLine,
        scriptArgs: ScriptArgsWithTypes? = null,
        invokeWrapper: InvokeWrapper? = null
    ): ReplEvalResult
}

interface ReplAtomicEvaluator : ReplAtomicEvalAction, ReplCheckAction

interface ReplDelayedEvalAction {
    suspend fun compileToEvaluable(
        state: IReplStageState<*>,
        codeLine: ReplCodeLine,
        defaultScriptArgs: ScriptArgsWithTypes? = null
    ): Pair<ReplCompileResult, Evaluable?>
}

// other

interface Evaluable {
    val compiledCode: ReplCompileResult.CompiledClasses
    suspend fun eval(scriptArgs: ScriptArgsWithTypes? = null, invokeWrapper: InvokeWrapper? = null): ReplEvalResult
}

interface ReplFullEvaluator : ReplEvaluator, ReplAtomicEvaluator, ReplDelayedEvalAction

/**
 * Keep args and arg types together, so as a whole they are present or absent
 */