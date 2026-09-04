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
import java.util.concurrent.locks.ReentrantReadWriteLock

const val REPL_CODE_LINE_FIRST_NO = 0
const val REPL_CODE_LINE_FIRST_GEN = 1

data class ReplCodeLine(val no: Int, val generation: Int, val code: String) : Serializable {
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

interface CreateReplStageStateAction {
    fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): IReplStageState<*>
}

// --- check

interface ReplCheckAction {
    fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult
}

sealed class ReplCheckResult : Serializable {
    class Ok : ReplCheckResult() {
        companion object { private val serialVersionUID: Long = 1L }
    }

    class Incomplete : ReplCheckResult() {
        companion object { private val serialVersionUID: Long = 1L }
    }

    class Error(val message: String, val location: CompilerMessageLocation? = null) : ReplCheckResult() {
        override fun toString(): String = "Error(message = \"$message\")"
        companion object { private val serialVersionUID: Long = 1L }
    }

    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

// --- compile

interface ReplCompileAction {
    fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult
}

sealed class ReplCompileResult : Serializable {
    class CompiledClasses(val lineId: LineId,
                          val previousLines: List<ILineId>,
                          val mainClassName: String,
                          val classes: List<CompiledClassData>,
                          val hasResult: Boolean,
                          val classpathAddendum: List<File>,
                          val type: String?,
                          val data: Any? // TODO: temporary; migration to new scripting infrastructure
    ) : ReplCompileResult() {
        companion object { private val serialVersionUID: Long = 2L }
    }

    class Incomplete(val message: String) : ReplCompileResult() {
        companion object { private val serialVersionUID: Long = 1L }
    }

    class Error(val message: String, val location: CompilerMessageLocation? = null) : ReplCompileResult() {
        override fun toString(): String = "Error(message = \"$message\""
        companion object { private val serialVersionUID: Long = 1L }
    }

    companion object {
        private val serialVersionUID: Long = 8228307678L
    }
}

interface ReplCompilerWithoutCheck : ReplCompileAction, CreateReplStageStateAction

interface ReplCompiler : ReplCompilerWithoutCheck, ReplCheckAction
