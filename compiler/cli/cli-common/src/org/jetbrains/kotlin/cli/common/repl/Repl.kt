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

// TODO: consider storing code hash where source is not needed

data class CompiledClassData(val path: String, val bytes: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean = (other as? CompiledClassData)?.let { path == it.path && Arrays.equals(bytes, it.bytes) } ?: false
    override fun hashCode(): Int = path.hashCode() + Arrays.hashCode(bytes)
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

sealed class ReplCheckResult(val updatedHistory: List<ReplCodeLine>) : Serializable {
    class Ok(updatedHistory: List<ReplCodeLine>) : ReplCheckResult(updatedHistory)
    class Incomplete(updatedHistory: List<ReplCodeLine>) : ReplCheckResult(updatedHistory)
    class Error(updatedHistory: List<ReplCodeLine>,
                val message: String,
                val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION
    ) : ReplCheckResult(updatedHistory)
    {
        override fun toString(): String = "Error(message = \"$message\""
    }
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

sealed class ReplCompileResult(val updatedHistory: List<ReplCodeLine>) : Serializable {
    class CompiledClasses(updatedHistory: List<ReplCodeLine>,
                          val classes: List<CompiledClassData>,
                          val hasResult: Boolean,
                          val classpathAddendum: List<File>
    ) : ReplCompileResult(updatedHistory)
    class Incomplete(updatedHistory: List<ReplCodeLine>) : ReplCompileResult(updatedHistory)
    class HistoryMismatch(updatedHistory: List<ReplCodeLine>, val lineNo: Int): ReplCompileResult(updatedHistory)
    class Error(updatedHistory: List<ReplCodeLine>,
                val message: String,
                val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION
    ) : ReplCompileResult(updatedHistory)
    {
        override fun toString(): String = "Error(message = \"$message\""
    }
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

sealed class ReplInvokeResult : Serializable {
    class ValueResult(val value: Any?) : ReplInvokeResult() {
        override fun toString(): String = "Result: $value"
    }
    object UnitResult : ReplInvokeResult()
    sealed class Error(val message: String) : ReplInvokeResult() {
        class Runtime(message: String, val cause: Exception? = null) : Error(message)
        class NoSuchEntity(message: String) : Error(message)
        class CompileTime(message: String, val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION) : Error(message)
        override fun toString(): String = "${this::class.simpleName}Error(message = \"$message\""
    }
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

sealed class ReplEvalResult(val updatedHistory: List<ReplCodeLine>) : Serializable {
    class ValueResult(updatedHistory: List<ReplCodeLine>, val value: Any?) : ReplEvalResult(updatedHistory) {
        override fun toString(): String = "Result: $value"
    }
    class UnitResult(updatedHistory: List<ReplCodeLine>) : ReplEvalResult(updatedHistory)
    class Incomplete(updatedHistory: List<ReplCodeLine>) : ReplEvalResult(updatedHistory)
    class HistoryMismatch(updatedHistory: List<ReplCodeLine>, val lineNo: Int): ReplEvalResult(updatedHistory)
    sealed class Error(updatedHistory: List<ReplCodeLine>, val message: String) : ReplEvalResult(updatedHistory) {
        class Runtime(updatedHistory: List<ReplCodeLine>, message: String, val cause: Exception? = null) : Error(updatedHistory, message)
        class CompileTime(updatedHistory: List<ReplCodeLine>,
                          message: String,
                          val location: CompilerMessageLocation = CompilerMessageLocation.NO_LOCATION
        ) : Error(updatedHistory, message)
        override fun toString(): String = "${this::class.simpleName}Error(message = \"$message\""
    }
    companion object {
        private val serialVersionUID: Long = 8228357578L
    }
}

interface ReplChecker {
    fun check(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplCheckResult
}

interface ReplCompiler : ReplChecker {
    fun compile(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplCompileResult
}

interface ReplInvoker {
    fun <T: Any> getInterface(clasz: KClass<T>): ReplInvokeResult
    fun <T: Any> getInterface(receiver: Any, clasz: KClass<T>): ReplInvokeResult
    fun invokeMethod(receiver: Any, name: String, vararg args: Any?): ReplInvokeResult
    fun invokeFunction(name: String, vararg args: Any?): ReplInvokeResult
}

interface ReplCompiledEvaluator {

    fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>, compiledClasses: List<CompiledClassData>, hasResult: Boolean, classpathAddendum: List<File>): ReplEvalResult

    // override to capture output
    fun<T> evalWithIO(body: () -> T): T = body()
}


interface ReplEvaluator : ReplChecker {

    fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplEvalResult

    // override to capture output
    fun<T> evalWithIO(body: () -> T): T = body()
}