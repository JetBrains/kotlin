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

package org.jetbrains.kotlin.idea.completion

object CompletionBenchmarkSink {

    val pendingSessions = mutableListOf<CompletionSession>()
    lateinit var results: CompletionResults
    var listener: ((CompletionResults) -> Unit)? = null

    fun onCompletionStarted(completionSession: CompletionSession) {
        if (pendingSessions.isEmpty())
            results = CompletionResults()
        pendingSessions += completionSession
    }

    fun onCompletionEnded(completionSession: CompletionSession) {
        pendingSessions -= completionSession
        if (pendingSessions.isEmpty()) {
            results.onEnd()
            listener?.invoke(results)
        }
    }

    fun onFirstFlush(completionSession: CompletionSession) {
        results.onFirstFlush()
    }

    fun reset() {
        pendingSessions.clear()
    }


    class CompletionResults {
        var start: Long = System.currentTimeMillis()
        var firstFlush: Long = 0
        var full: Long = 0
        fun onFirstFlush() {
            if (firstFlush == 0L)
                firstFlush = System.currentTimeMillis() - start
        }

        fun onEnd() {
            full = System.currentTimeMillis() - start
        }
    }
}
