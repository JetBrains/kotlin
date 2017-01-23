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
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition

private val logger = Logger.getInstance(GenericRepl::class.java)

open class GenericRepl(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        baseClassloader: ClassLoader?,
        scriptArgs: Array<Any?>? = null,
        scriptArgsTypes: Array<Class<*>>? = null
) : ReplEvaluator, GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector) {

    private val compiledEvaluator = GenericReplCompiledEvaluator(compilerConfiguration.jvmClasspathRoots, baseClassloader, scriptArgs, scriptArgsTypes)

    override val lastEvaluatedScript: ClassWithInstance? get() = compiledEvaluator.lastEvaluatedScript

    @Synchronized
    override fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>, invokeWrapper: InvokeWrapper?): ReplEvalResult =
        compileAndEval(this, compiledEvaluator, codeLine, history, invokeWrapper)
}


fun compileAndEval(replCompiler: ReplCompiler, replCompiledEvaluator: ReplCompiledEvaluator, codeLine: ReplCodeLine, history: List<ReplCodeLine>, invokeWrapper: InvokeWrapper?): ReplEvalResult =
        replCompiler.compile(codeLine, history).let {
            when (it) {
                is ReplCompileResult.Incomplete -> ReplEvalResult.Incomplete(it.updatedHistory)
                is ReplCompileResult.HistoryMismatch -> ReplEvalResult.HistoryMismatch(it.updatedHistory, it.lineNo)
                is ReplCompileResult.Error -> ReplEvalResult.Error.CompileTime(it.updatedHistory, it.message, it.location)
                is ReplCompileResult.CompiledClasses -> replCompiledEvaluator.eval(codeLine, history, it.classes, it.hasResult, it.classpathAddendum, invokeWrapper)
            }
        }


