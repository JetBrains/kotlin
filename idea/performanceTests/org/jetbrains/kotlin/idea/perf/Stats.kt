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

class Stats(val name: String = "", val header: Array<String> = arrayOf("Name", "ValueMS", "StdDev")) : Closeable {
    private val statsFile: File =
        File("build/stats${if (name.isNotEmpty()) "-${name.toLowerCase().replace(' ', '-')}" else ""}.csv")
            .absoluteFile
    private val statsOutput: BufferedWriter

    init {
        statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln(header.joinToString())
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
        append(arrayOf(file, id, nanoTime.nsToMs))
    }

    fun <T, K> perfTest(
        testName: String,
        warmUpIterations: Int = 3,
        iterations: Int = 10,
        setUp: () -> T? = { null },
        test: (t: T?) -> K,
        tearDown: (t: K?) -> Unit = {}
    ) {
        val namePrefix = "$name: $testName"
        val timingsNs = LongArray(iterations)
        val errors = Array<Throwable?>(iterations, init = { null })

        tcSuite(namePrefix) {
            for (attempt in 0 until warmUpIterations) {
                val n = "$namePrefix warm-up #$attempt"
                println("##teamcity[testStarted name='$n' captureStandardOutput='true']")

                try {
                    val setupValue: T? = setUp()
                    var value: K? = null
                    var spentNs: Long = 0
                    try {
                        spentNs = measureNanoTime {
                            value = test(setupValue)
                        }
                    } catch (t: Throwable) {
                        println("error at $n:")
                        tcPrintErrors(n, listOf(t))
                    } finally {
                        tearDown(value)
                    }
                    val spentMs = spentNs.nsToMs
                    println("##teamcity[buildStatisticValue key='$n' value='$spentMs']")
                    println("##teamcity[testFinished name='$n' duration='$spentMs']")
                } catch (t: Throwable) {
                    println("error at $n:")
                    tcPrintErrors(n, listOf(t))
                    throw t
                }
            }

            try {
                for (attempt in 0 until iterations) {
                    val setupValue: T? = setUp()
                    var value: K? = null
                    try {
                        val spentNs = measureNanoTime {
                            value = test(setupValue)
                        }
                        timingsNs[attempt] = spentNs
                    } catch (t: Throwable) {
                        println("error at $namePrefix #$attempt:")
                        errors[attempt] = t
                    } finally {
                        tearDown(value)
                    }
                }
            } catch (t: Throwable) {
                println("error at $namePrefix:")
                tcPrintErrors(namePrefix, listOf(t))
            }


            val meanNs = timingsNs.average()
            val meanMs = meanNs.toLong().nsToMs
            val stdDivMs = (sqrt(
                timingsNs.fold(0.0,
                               { accumulator, next -> accumulator + (1.0 * (next - meanMs)).pow(2.0) })
            ) / timingsNs.size).toLong().nsToMs

            for (attempt in 0 until iterations) {
                val n = "$namePrefix #$attempt"
                println("##teamcity[testStarted name='$n' captureStandardOutput='true']")
                if (errors[attempt] != null) {
                    tcPrintErrors(n, listOf(errors[attempt]!!))
                }
                val spentMs = timingsNs[attempt].nsToMs
                println("##teamcity[buildStatisticValue key='$n' value='$spentMs']")
                println("##teamcity[testFinished name='$n' duration='$spentMs']")
            }

            println("##teamcity[buildStatisticValue key='$namePrefix' value='$meanMs']")
            println("##teamcity[buildStatisticValue key='$namePrefix stdDev' value='${stdDivMs}']")

            append(arrayOf(namePrefix, meanMs, stdDivMs))
        }
    }

    override fun close() {
        statsOutput.flush()
        statsOutput.close()
    }
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
