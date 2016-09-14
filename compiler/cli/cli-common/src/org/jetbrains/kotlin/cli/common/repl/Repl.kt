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

import java.io.File
import java.io.Serializable
import java.util.*

data class ReplCodeLine(val no: Int, val code: String)

data class CompiledClassData(val path: String, val bytes: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean = (other as? CompiledClassData)?.let { path == it.path && Arrays.equals(bytes, it.bytes) } ?: false
    override fun hashCode(): Int = path.hashCode() + Arrays.hashCode(bytes)
}

sealed class ReplCheckResult : Serializable {
    object Ok : ReplCheckResult()
    object Incomplete : ReplCheckResult()
    class Error(val message: String) : ReplCheckResult()
}

sealed class ReplCompileResult : Serializable {
    class CompiledClasses(val classes: List<CompiledClassData>, val hasResult: Boolean) : ReplCompileResult()
    object Incomplete : ReplCompileResult()
    class HistoryMismatch(val lineNo: Int): ReplCompileResult()
    class Error(val message: String) : ReplCompileResult()
}

sealed class ReplEvalResult : Serializable {
    class ValueResult(val value: Any?) : ReplEvalResult()
    object UnitResult : ReplEvalResult()
    object Incomplete : ReplEvalResult()
    class HistoryMismatch(val lineNo: Int): ReplEvalResult()
    sealed class Error(val message: String) : ReplEvalResult() {
        class Runtime(message: String) : Error(message)
        class CompileTime(message: String) : Error(message)
    }
}

interface ReplChecker {
    fun check(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplCheckResult
}

interface ReplCompiler : ReplChecker {
    fun compile(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplCompileResult

    // a callback
    fun dependenciesAdded(classpath: List<File>)
}

interface ReplCompiledEvaluator {

    fun eval(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>, compiledClasses: List<CompiledClassData>, hasResult: Boolean): ReplEvalResult

    // override to capture output
    fun<T> evalWithIO(body: () -> T): T = body()
}


interface ReplEvaluator : ReplChecker {

    fun eval(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplEvalResult

    fun dependenciesAdded(classpath: List<File>)

    // override to capture output
    fun<T> evalWithIO(body: () -> T): T = body()
}