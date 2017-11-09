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
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * This counter is thread-safe for initialization and usage.
 * But it may calculate time and number of runs not precisely.
 */
abstract class PerformanceCounter protected constructor(val name: String) {
    companion object {
        private val allCounters = arrayListOf<PerformanceCounter>()

        private var enabled = false

        fun currentTime(): Long = System.nanoTime()

        fun report(consumer: (String) -> Unit) {
            val countersCopy = synchronized(allCounters) {
                allCounters.toTypedArray()
            }
            countersCopy.forEach { it.report(consumer) }
        }

        fun setTimeCounterEnabled(enable: Boolean) {
            enabled = enable
        }

        fun resetAllCounters() {
            synchronized(allCounters) {
                allCounters.forEach(PerformanceCounter::reset)
            }
        }

        @JvmOverloads fun create(name: String, reenterable: Boolean = false): PerformanceCounter {
            return if (reenterable)
                ReenterableCounter(name)
            else
                SimpleCounter(name)
        }

        fun create(name: String, vararg excluded: PerformanceCounter): PerformanceCounter = CounterWithExclude(name, *excluded)

        internal inline fun <T> getOrPut(threadLocal: ThreadLocal<T>, default: () -> T) : T {
            var value = threadLocal.get()
            if (value == null) {
                value = default()
                threadLocal.set(value)
            }
            return value
        }
    }

    internal val excludedFrom: MutableList<CounterWithExclude> = ArrayList()

    private var count: Int = 0
    private var totalTimeNanos: Long = 0

    init {
        synchronized(allCounters) {
            allCounters.add(this)
        }
    }

    fun increment() {
        count++
    }

    fun <T> time(block: () -> T): T {
        count++
        if (!enabled) return block()

        excludedFrom.forEach { it.enterExcludedMethod() }
        try {
            return countTime(block)
        }
        finally {
            excludedFrom.forEach { it.exitExcludedMethod() }
        }
    }

    fun reset() {
        count = 0
        totalTimeNanos = 0
    }

    protected fun incrementTime(delta: Long) {
        totalTimeNanos += delta
    }

    protected abstract fun <T> countTime(block: () -> T): T

    fun report(consumer: (String) -> Unit) {
        if (totalTimeNanos == 0L) {
            consumer("$name performed $count times")
        }
        else {
            val millis = TimeUnit.NANOSECONDS.toMillis(totalTimeNanos)
            consumer("$name performed $count times, total time $millis ms")
        }
    }
}

private class SimpleCounter(name: String): PerformanceCounter(name) {
    override fun <T> countTime(block: () -> T): T {
        val startTime = PerformanceCounter.currentTime()
        try {
            return block()
        }
        finally {
            incrementTime(PerformanceCounter.currentTime() - startTime)
        }
    }
}

private class ReenterableCounter(name: String): PerformanceCounter(name) {
    companion object {
        private val enteredCounters = ThreadLocal<MutableSet<ReenterableCounter>>()

        private fun enterCounter(counter: ReenterableCounter) = PerformanceCounter.getOrPut(enteredCounters) { HashSet() }.add(counter)

        private fun leaveCounter(counter: ReenterableCounter) {
            enteredCounters.get()?.remove(counter)
        }
    }

    override fun <T> countTime(block: () -> T): T {
        val startTime = PerformanceCounter.currentTime()
        val needTime = enterCounter(this)
        try {
            return block()
        }
        finally {
            if (needTime) {
                incrementTime(PerformanceCounter.currentTime() - startTime)
                leaveCounter(this)
            }
        }
    }
}

/**
 *  This class allows to calculate pure time for some method excluding some other methods.
 *  For example, we can calculate total time for CallResolver excluding time for getTypeInfo().
 *
 *  Main and excluded methods may be reenterable.
 */
internal class CounterWithExclude(name: String, vararg excludedCounters: PerformanceCounter): PerformanceCounter(name) {
    companion object {
        private val counterToCallStackMapThreadLocal = ThreadLocal<MutableMap<CounterWithExclude, CallStackWithTime>>()

        private fun getCallStack(counter: CounterWithExclude)
                = PerformanceCounter.getOrPut(counterToCallStackMapThreadLocal) { HashMap() }.getOrPut(counter) { CallStackWithTime() }
    }

    init {
        excludedCounters.forEach { it.excludedFrom.add(this) }
    }

    private val callStack: CallStackWithTime
        get() = getCallStack(this)

    override fun <T> countTime(block: () -> T): T {
        incrementTime(callStack.push(true))
        try {
            return block()
        }
        finally {
            incrementTime(callStack.pop(true))
        }
    }

    fun enterExcludedMethod() {
        incrementTime(callStack.push(false))
    }

    fun exitExcludedMethod() {
        incrementTime(callStack.pop(false))
    }

    private class CallStackWithTime {
        private val callStack = Stack<Boolean>()
        private var intervalStartTime: Long = 0

        fun Stack<Boolean>.peekOrFalse() = if (isEmpty()) false else peek()

        private fun intervalUsefulTime(callStackUpdate: Stack<Boolean>.() -> Unit): Long {
            val delta = if (callStack.peekOrFalse()) PerformanceCounter.currentTime() - intervalStartTime else 0
            callStack.callStackUpdate()

            intervalStartTime = PerformanceCounter.currentTime()
            return delta
        }

        fun push(usefulCall: Boolean): Long {
            if (!isEnteredCounter() && !usefulCall) return 0

            return intervalUsefulTime { push(usefulCall) }
        }

        fun pop(usefulCall: Boolean): Long {
            if (!isEnteredCounter()) return 0

            assert(callStack.peek() == usefulCall)
            return intervalUsefulTime { pop() }
        }

        fun isEnteredCounter(): Boolean = !callStack.isEmpty()
    }
}
