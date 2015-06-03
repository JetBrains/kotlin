/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

public class PerformanceCounter jvmOverloads constructor (val name: String, val reenterable: Boolean = false) {
    public var count: Int = 0
    public var totalTimeNanos: Long = 0

    init {
        allCounters.add(this)
    }

    public fun increment() {
        count++
    }

    public fun time<T>(block: () -> T): T {
        count++
        val needTime = !reenterable || enterCounter(this)
        val startTime = currentThreadCpuTime()
        try {
            return block()
        }
        finally {
            if (needTime) {
                totalTimeNanos += currentThreadCpuTime() - startTime
                if (reenterable) leaveCounter(this)
            }
        }
    }

    public fun report(consumer: (String) -> Unit) {
        if (totalTimeNanos == 0L) {
            consumer("$name performed $count times")
        }
        else {
            val millis = TimeUnit.NANOSECONDS.toMillis(totalTimeNanos)
            consumer("$name performed $count times, total time $millis ms")
        }
    }

    companion object {
        val threadMxBean = ManagementFactory.getThreadMXBean()
        val allCounters = arrayListOf<PerformanceCounter>()

        val enteredCounters = ThreadLocal<MutableSet<PerformanceCounter>>()

        fun enterCounter(counter: PerformanceCounter): Boolean {
            var enteredCountersInThread = enteredCounters.get()
            if (enteredCountersInThread == null) {
                enteredCountersInThread = hashSetOf(counter)
                enteredCounters.set(enteredCountersInThread)
                return true
            } else {
                return enteredCountersInThread.add(counter)
            }
        }

        fun leaveCounter(counter: PerformanceCounter) {
            enteredCounters.get()?.remove(counter)
        }

        init {
            threadMxBean.setThreadCpuTimeEnabled(true)
        }

        public fun currentThreadCpuTime(): Long = threadMxBean.getCurrentThreadUserTime()

        fun report(consumer: (String) -> Unit) {
            allCounters.forEach { it.report(consumer) }
        }
    }
}