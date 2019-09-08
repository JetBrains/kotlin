/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.WholeProjectPerformanceTest.Companion.nsToMs
import java.io.*
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.measureNanoTime
import java.lang.ref.WeakReference
import kotlin.math.exp
import kotlin.math.ln
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class Stats(val name: String = "", val header: Array<String> = arrayOf("Name", "ValueMS", "StdDev")) : Closeable {
    private val perfTestRawDataMs = mutableListOf<Long>()

    private val statsFile: File = File("build/stats${statFilePrefix()}.csv").absoluteFile

    private val statsOutput: BufferedWriter

    init {
        statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln(header.joinToString())
    }

    private fun statFilePrefix() = if (name.isNotEmpty()) "-${name.toLowerCase().replace(' ', '-').replace('/', '_')}" else ""

    private fun append(id: String, timingsNs: LongArray) {
        val meanNs = timingsNs.average()
        val meanMs = meanNs.toLong().nsToMs

        val stdDevMs = if (timingsNs.size > 1) (sqrt(
            timingsNs.fold(0.0,
                           { accumulator, next -> accumulator + (1.0 * (next - meanNs)).pow(2) })
        ) / (timingsNs.size - 1)).toLong().nsToMs
        else 0

        val geomMeanMs = geomMean(timingsNs.toList()).nsToMs

        for (v in listOf(
            Triple("mean", "", meanMs),
            Triple("stdDev", " stdDev", stdDevMs),
            Triple("geomMean", "geomMean", geomMeanMs)
        )) {
            println("##teamcity[testStarted name='$id : ${v.first}' captureStandardOutput='true']")
            println("##teamcity[buildStatisticValue key='$id${v.second}' value='${v.third}']")
            println("##teamcity[testFinished name='$id : ${v.first}' duration='${v.third}']")
        }

        perfTestRawDataMs.addAll(timingsNs.map { it.nsToMs }.toList())
        append(arrayOf(id, meanMs, stdDevMs))
    }

    private fun append(values: Array<Any>) {
        if (values.size != header.size) {
            throw IllegalArgumentException("Expected ${header.size} values, actual ${values.size} values")
        }
        with(statsOutput) {
            appendln(values.joinToString { it.toString() })
            flush()
        }
    }

    fun append(file: String, id: String, nanoTime: Long) {
        val ms = nanoTime.nsToMs
        append(arrayOf(file, id, ms))
    }

    fun <K, T> perfTest(
        testName: String,
        warmUpIterations: Int = 3,
        iterations: Int = 10,
        setUp: (TestData<K, T>) -> Unit = { null },
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit = { null }
    ) {
        val namePrefix = "$testName"
        val timingsNs = LongArray(iterations)
        val errors = Array<Throwable?>(iterations, init = { null })

        tcSuite(namePrefix) {
            warmUpPhase(warmUpIterations, namePrefix, setUp, test, tearDown)

            mainPhase(iterations, setUp, test, tearDown, timingsNs, namePrefix, errors)

            assertEquals(iterations, timingsNs.size)
            appendTimings(namePrefix, errors, timingsNs)
        }
    }

    fun printWarmUpTimings(
        prefix: String,
        errors: Array<Throwable?>,
        warmUpTimingsNs: LongArray
    ) {
        assertEquals(warmUpTimingsNs.size, errors.size)
        for (timing in warmUpTimingsNs.withIndex()) {
            val attempt = timing.index
            val n = "$name: $prefix warm-up #$attempt"
            printTestStarted(n)
            val t = errors[attempt]
            if (t != null) printTestFinished(n, t!!) else printTestFinished(n, timing.value.nsToMs)
        }
    }

    fun appendTimings(
        prefix: String,
        errors: Array<Throwable?>,
        timingsNs: LongArray
    ) {
        assertEquals(timingsNs.size, errors.size)
        val namePrefix = "$name: $prefix"
        for (attempt in 0 until timingsNs.size) {
            val n = "$namePrefix #$attempt"
            printTestStarted(n)
            if (errors[attempt] != null) {
                tcPrintErrors(n, listOf(errors[attempt]!!))
            }
            val spentMs = timingsNs[attempt].nsToMs
            printTestFinished(n, spentMs)
        }

        append(namePrefix, timingsNs)
    }

    private fun <K, T> mainPhase(
        iterations: Int,
        setUp: (TestData<K, T>) -> Unit,
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit,
        timingsNs: LongArray,
        namePrefix: String,
        errors: Array<Throwable?>
    ) {
        val testData = TestData<K, T>(null, null)
        try {
            for (attempt in 0 until iterations) {
                testData.reset()
                triggerGC(attempt)

                val setUpMillis = measureTimeMillis {
                    setUp(testData)
                }
                println("-- setup took $setUpMillis ms")
                try {
                    val spentNs = measureNanoTime {
                        test(testData)
                    }
                    timingsNs[attempt] = spentNs
                } catch (t: Throwable) {
                    println("# error at $namePrefix #$attempt:")
                    t.printStackTrace()
                    errors[attempt] = t
                } finally {
                    val tearDownMillis = measureTimeMillis {
                        tearDown(testData)
                    }
                    println("-- tearDown took $tearDownMillis ms")
                }
            }
        } catch (t: Throwable) {
            println("error at $namePrefix:")
            tcPrintErrors(namePrefix, listOf(t))
        }
    }

    private fun <K, T> warmUpPhase(
        warmUpIterations: Int,
        namePrefix: String,
        setUp: (TestData<K, T>) -> Unit,
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit
    ) {
        val testData = TestData<K, T>(null, null)
        val warmUpTimingsNs = LongArray(warmUpIterations)
        val errors: Array<Throwable?> = Array(warmUpIterations, init = { null })
        for (attempt in 0 until warmUpIterations) {
            testData.reset()

            triggerGC(attempt)

            try {
                val setUpMillis = measureTimeMillis {
                    setUp(testData)
                }
                println("-- setup took $setUpMillis ms")
                var spentNs: Long = 0
                try {
                    spentNs = measureNanoTime {
                        test(testData)
                    }
                } finally {
                    val tearDownMillis = measureTimeMillis {
                        tearDown(testData)
                    }
                    println("-- tearDown took $tearDownMillis ms")
                }
                warmUpTimingsNs[attempt] = spentNs
            } catch (t: Throwable) {
                errors[attempt] = t
            }
        }

        printWarmUpTimings(namePrefix, errors, warmUpTimingsNs)

        errors.find { it != null }?.let { throw it }
    }

    fun printTestStarted(testName: String) {
        println("##teamcity[testStarted name='$testName' captureStandardOutput='true']")
    }

    fun printTestFinished(testName: String, spentMs: Long) {
        println("##teamcity[buildStatisticValue key='$testName' value='$spentMs']")
        println("##teamcity[testFinished name='$testName' duration='$spentMs']")
    }

    fun printTestFinished(testName: String, error: Throwable) {
        println("error at $testName:")
        tcPrintErrors(testName, listOf(error))

        println("##teamcity[buildStatisticValue key='$testName' value='-1']")
        println("##teamcity[testFinished name='$testName']")
    }

    private fun triggerGC(attempt: Int) {
        if (attempt > 0) {
            val ref = WeakReference(IntArray(32 * 1024))
            while (ref.get() != null) {
                System.gc()
                Thread.sleep(1)
            }
        }
    }

    private fun geomMean(data: List<Long>) = exp(data.fold(0.0, { mul, next -> mul + ln(1.0 * next) }) / data.size).toLong()

    override fun close() {
        if (perfTestRawDataMs.isNotEmpty()) {
            val geomMeanMs = geomMean(perfTestRawDataMs.toList())
            println("##teamcity[buildStatisticValue key='$name geomMean' value='$geomMeanMs']")
            append(arrayOf("$name geomMean", geomMeanMs, 0))
        }
        statsOutput.flush()
        statsOutput.close()
    }
}

data class TestData<SV, V>(var setUpValue: SV?, var value: V?) {
    fun reset() {
        setUpValue = null
        value = null
    }
}

inline fun runAndMeasure(note: String, block: () -> Unit) {
    val openProjectMillis = measureTimeMillis {
        block()
    }
    println("-- $note took $openProjectMillis ms")
}

inline fun tcSuite(name: String, block: () -> Unit) {
    println("##teamcity[testSuiteStarted name='$name']")
    block()
    println("##teamcity[testSuiteFinished name='$name']")
}

inline fun tcTest(name: String, block: () -> Pair<Long, List<Throwable>>) {
    println("##teamcity[testStarted name='$name' captureStandardOutput='true']")
    val (time, errors) = block()
    tcPrintErrors(name, errors)
    println("##teamcity[testFinished name='$name' duration='$time']")
}

fun tcPrintErrors(name: String, errors: List<Throwable>) {
    if (errors.isNotEmpty()) {
        val detailsWriter = StringWriter()
        val errorDetailsPrintWriter = PrintWriter(detailsWriter)
        errors.forEach {
            it.printStackTrace(errorDetailsPrintWriter)
            errorDetailsPrintWriter.println()
        }
        errorDetailsPrintWriter.close()
        val details = detailsWriter.toString()
        println("##teamcity[testFailed name='$name' message='Exceptions reported' details='${details.tcEscape()}']")
    }
}

fun String.tcEscape(): String {
    return this
        .replace("|", "||")
        .replace("[", "|[")
        .replace("]", "|]")
        .replace("\r", "|r")
        .replace("\n", "|n")
        .replace("'", "|'")
}
