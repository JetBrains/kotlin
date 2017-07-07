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

import kotlinx.coroutines.experimental.channels.ConflatedChannel


interface CompletionBenchmarkSink {
    fun onCompletionStarted(completionSession: CompletionSession)
    fun onCompletionEnded(completionSession: CompletionSession)
    fun onFirstFlush(completionSession: CompletionSession)

    companion object {

        fun enableAndGet(): Impl = Impl().also { _instance = it }

        fun disable() {
            _instance.let { (it as? Impl)?.channel?.close() }
            _instance = Empty
        }

        val instance get() = _instance
        private var _instance: CompletionBenchmarkSink = Empty
    }

    private object Empty : CompletionBenchmarkSink {
        override fun onCompletionStarted(completionSession: CompletionSession) {}

        override fun onCompletionEnded(completionSession: CompletionSession) {}

        override fun onFirstFlush(completionSession: CompletionSession) {}
    }

    class Impl : CompletionBenchmarkSink {
        private val pendingSessions = mutableListOf<CompletionSession>()
        private lateinit var results: CompletionBenchmarkResults
        val channel = ConflatedChannel<CompletionBenchmarkResults>()

        override fun onCompletionStarted(completionSession: CompletionSession) = synchronized(this) {
            if (pendingSessions.isEmpty())
                results = CompletionBenchmarkResults()
            pendingSessions += completionSession
        }

        override fun onCompletionEnded(completionSession: CompletionSession) = synchronized(this) {
            pendingSessions -= completionSession
            if (pendingSessions.isEmpty()) {
                results.onEnd()
                channel.offer(results)
            }
        }

        override fun onFirstFlush(completionSession: CompletionSession) = synchronized(this) {
            results.onFirstFlush()
        }

        fun reset() = synchronized(this) {
            pendingSessions.clear()
        }

        class CompletionBenchmarkResults {
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
}
