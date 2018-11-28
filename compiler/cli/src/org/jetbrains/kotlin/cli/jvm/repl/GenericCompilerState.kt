/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.dependencies.ScriptDependencies

class ReplCompilerStageHistory(private val state: GenericReplCompilerState) : BasicReplStageHistory<ScriptDescriptor>(state.lock) {

    override fun reset(): Iterable<ILineId> {
        val removedCompiledLines = super.reset()
        val removedAnalyzedLines = state.analyzerEngine.reset()

        checkConsistent(removedCompiledLines, removedAnalyzedLines)
        return removedCompiledLines
    }

    override fun resetTo(id: ILineId): Iterable<ILineId> {
        val removedCompiledLines = super.resetTo(id)
        val removedAnalyzedLines = state.analyzerEngine.resetToLine(id)

        checkConsistent(removedCompiledLines, removedAnalyzedLines)
        return removedCompiledLines
    }

    private fun checkConsistent(removedCompiledLines: Iterable<ILineId>, removedAnalyzedLines: List<ReplCodeLine>) {
        removedCompiledLines.zip(removedAnalyzedLines).forEach { (removedCompiledLine, removedAnalyzedLine) ->
            if (removedCompiledLine != LineId(removedAnalyzedLine)) {
                throw IllegalStateException("History mismatch when resetting lines: ${removedCompiledLine.no} != $removedAnalyzedLine")
            }
        }
    }
}

abstract class GenericReplCheckerState : IReplStageState<ScriptDescriptor> {

    // "line" - is the unit of evaluation here, could in fact consists of several character lines
    class LineState(
        val codeLine: ReplCodeLine,
        val psiFile: KtFile,
        val errorHolder: DiagnosticMessageHolder
    )

    var lastLineState: LineState? = null // for transferring state to the compiler in most typical case
}

class GenericReplCompilerState(environment: KotlinCoreEnvironment, override val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()) :
    IReplStageState<ScriptDescriptor>, GenericReplCheckerState() {

    override val history = ReplCompilerStageHistory(this)

    override val currentGeneration: Int get() = (history as BasicReplStageHistory<*>).currentGeneration.get()

    val analyzerEngine = ReplCodeAnalyzer(environment)

    var lastDependencies: ScriptDependencies? = null
}
