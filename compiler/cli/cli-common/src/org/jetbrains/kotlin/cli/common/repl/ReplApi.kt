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

package org.jetbrains.kotlin.cli.common.repl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.reflect.KClass

data class ReplCodeLine(val no: Int, val code: String) : Serializable {
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

data class CompiledReplCodeLine(val className: String, val source: ReplCodeLine) : Serializable {
    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

data class CompiledClassData(val path: String, val bytes: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean = (other as? CompiledClassData)?.let { path == it.path && Arrays.equals(bytes, it.bytes) } ?: false
    override fun hashCode(): Int = path.hashCode() + Arrays.hashCode(bytes)

    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}


interface ReplCheckAction {
    fun check(codeLine: ReplCodeLine): ReplCheckResult
}

sealed class ReplCheckResult : Serializable {
    class Ok : ReplCheckResult()

    class Incomplete : ReplCheckResult()

    class Error(val message: String,
                val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION) : ReplCheckResult() {
        override fun toString(): String = "Error(message = \"$message\""
    }

    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

interface ReplResettableCodeLine {
    fun resetToLine(lineNumber: Int): List<ReplCodeLine>

    fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> = resetToLine(line.no)
}

interface ReplCodeLineHistory {
    val history: List<ReplCodeLine>
}

interface ReplCombinedHistory {
    val compiledHistory: List<ReplCodeLine>
    val evaluatedHistory: List<ReplCodeLine>
}

interface ReplCompileAction {
    fun compile(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>? = null): ReplCompileResult
}

sealed class ReplCompileResult(val compiledHistory: List<ReplCodeLine>) : Serializable {
    class CompiledClasses(compiledHistory: List<ReplCodeLine>,
                          val compiledCodeLine: CompiledReplCodeLine,
                          val generatedClassname: String,
                          val classes: List<CompiledClassData>,
                          val hasResult: Boolean,
                          val classpathAddendum: List<File>) : ReplCompileResult(compiledHistory)

    class Incomplete(compiledHistory: List<ReplCodeLine>) : ReplCompileResult(compiledHistory)

    class HistoryMismatch(compiledHistory: List<ReplCodeLine>, val lineNo: Int) : ReplCompileResult(compiledHistory)

    class Error(compiledHistory: List<ReplCodeLine>,
                val message: String,
                val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION) : ReplCompileResult(compiledHistory) {
        override fun toString(): String = "Error(message = \"$message\""
    }

    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

interface ReplCompiler : ReplResettableCodeLine, ReplCodeLineHistory, ReplCompileAction, ReplCheckAction

typealias EvalHistoryType = Pair<CompiledReplCodeLine, EvalClassWithInstanceAndLoader>

interface ReplEvaluatorExposedInternalHistory {
    val lastEvaluatedScripts: List<EvalHistoryType>
}

interface ReplClasspath {
    val currentClasspath: List<File>
}

data class EvalClassWithInstanceAndLoader(val klass: KClass<*>, val instance: Any?, val classLoader: ClassLoader, val invokeWrapper: InvokeWrapper?)

interface ReplEvalAction {
    fun eval(compileResult: ReplCompileResult.CompiledClasses,
             scriptArgs: ScriptArgsWithTypes? = null,
             invokeWrapper: InvokeWrapper? = null): ReplEvalResult
}

sealed class ReplEvalResult(val completedEvalHistory: List<ReplCodeLine>) : Serializable {
    class ValueResult(completedEvalHistory: List<ReplCodeLine>, val value: Any?) : ReplEvalResult(completedEvalHistory) {
        override fun toString(): String = "Result: $value"
    }

    class UnitResult(completedEvalHistory: List<ReplCodeLine>) : ReplEvalResult(completedEvalHistory)

    class Incomplete(completedEvalHistory: List<ReplCodeLine>) : ReplEvalResult(completedEvalHistory)

    class HistoryMismatch(completedEvalHistory: List<ReplCodeLine>, val lineNo: Int) : ReplEvalResult(completedEvalHistory)

    sealed class Error(completedEvalHistory: List<ReplCodeLine>, val message: String) : ReplEvalResult(completedEvalHistory) {
        class Runtime(completedEvalHistory: List<ReplCodeLine>, message: String, val cause: Exception? = null) : Error(completedEvalHistory, message)

        class CompileTime(completedEvalHistory: List<ReplCodeLine>,
                          message: String,
                          val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION) : Error(completedEvalHistory, message)

        override fun toString(): String = "${this::class.simpleName}Error(message = \"$message\""
    }

    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

interface ReplEvaluator : ReplResettableCodeLine, ReplCodeLineHistory, ReplEvaluatorExposedInternalHistory, ReplEvalAction, ReplClasspath

interface ReplAtomicEvalAction {
    fun compileAndEval(codeLine: ReplCodeLine,
                       scriptArgs: ScriptArgsWithTypes? = null,
                       verifyHistory: List<ReplCodeLine>? = null,
                       invokeWrapper: InvokeWrapper? = null): ReplEvalResult
}

interface ReplAtomicEvaluator : ReplResettableCodeLine, ReplCombinedHistory, ReplEvaluatorExposedInternalHistory, ReplAtomicEvalAction, ReplCheckAction, ReplClasspath

interface ReplDelayedEvalAction {
    fun compileToEvaluable(codeLine: ReplCodeLine, defaultScriptArgs: ScriptArgsWithTypes? = null, verifyHistory: List<ReplCodeLine>?): Pair<ReplCompileResult, Evaluable?>
}

interface Evaluable {
    val compiledCode: ReplCompileResult.CompiledClasses
    fun eval(scriptArgs: ScriptArgsWithTypes? = null, invokeWrapper: InvokeWrapper? = null): ReplEvalResult
}

interface ReplFullEvaluator : ReplEvaluator, ReplAtomicEvaluator, ReplDelayedEvalAction, ReplCombinedHistory

/**
 * Keep args and arg types together, so as a whole they are present or absent
 */
class ScriptArgsWithTypes(val scriptArgs: Array<out Any?>, val scriptArgsTypes: Array<out KClass<out Any>>) : Serializable {
    companion object {
        private val serialVersionUID: Long = 8529357500L
    }
}

interface ScriptTemplateEmptyArgsProvider {
    val defaultEmptyArgs: ScriptArgsWithTypes?
}

class SimpleScriptTemplateEmptyArgsProvider(override val defaultEmptyArgs: ScriptArgsWithTypes? = null) : ScriptTemplateEmptyArgsProvider

enum class ReplRepeatingMode {
    NONE,
    REPEAT_ONLY_MOST_RECENT,
    REPEAT_ANY_PREVIOUS
}


interface InvokeWrapper {
    operator fun <T> invoke(body: () -> T): T // e.g. for capturing io
}