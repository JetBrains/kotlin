/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

open class GenericRepl protected constructor(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        baseClassloader: ClassLoader?,
        protected val fallbackScriptArgs: ScriptArgsWithTypes?,
        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
        protected val stateLock: ReentrantReadWriteLock
) : ReplCompiler, ReplEvaluator, ReplAtomicEvaluator {
    constructor (
            disposable: Disposable,
            scriptDefinition: KotlinScriptDefinition,
            compilerConfiguration: CompilerConfiguration,
            messageCollector: MessageCollector,
            baseClassloader: ClassLoader?,
            fallbackScriptArgs: ScriptArgsWithTypes? = null
    ) : this(disposable, scriptDefinition, compilerConfiguration, messageCollector, baseClassloader, fallbackScriptArgs, stateLock = ReentrantReadWriteLock())

    protected val compiler: ReplCompiler by lazy { GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector, stateLock) }
    protected val evaluator: ReplFullEvaluator by lazy { GenericReplCompilingEvaluator(compiler, compilerConfiguration.jvmClasspathRoots, baseClassloader, fallbackScriptArgs, repeatingMode, stateLock) }
    protected var codeLineNumber = AtomicInteger(0)

    override fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
        return evaluator.resetToLine(lineNumber)
    }

    override fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> {
        return evaluator.resetToLine(line)
    }

    override val history: List<ReplCodeLine> get() = evaluator.history
    override val compiledHistory: List<ReplCodeLine> get() = compiler.history
    override val evaluatedHistory: List<ReplCodeLine> get() = evaluator.history

    override val currentClasspath: List<File>
        get() = evaluator.currentClasspath

    override val lastEvaluatedScripts: List<EvalHistoryType>
        get() = evaluator.lastEvaluatedScripts

    override fun check(codeLine: ReplCodeLine): ReplCheckResponse {
        return compiler.check(codeLine)
    }

    override fun compile(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>?): ReplCompileResponse {
        return compiler.compile(codeLine, verifyHistory)
    }

    override fun eval(compileResult: ReplCompileResponse.CompiledClasses, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        return evaluator.eval(compileResult, scriptArgs, invokeWrapper)
    }

    override fun compileAndEval(codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, verifyHistory: List<ReplCodeLine>?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        return evaluator.compileAndEval(codeLine, scriptArgs, verifyHistory, invokeWrapper)
    }

    fun nextCodeLine(code: String) = ReplCodeLine(codeLineNumber.incrementAndGet(), code)
}