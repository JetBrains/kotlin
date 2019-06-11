/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.WholeProjectPerformanceTest.Companion.nsToMs
import java.io.*
import kotlin.system.measureNanoTime

class Stats(name: String = "") : Closeable {
    private val statsFile: File = File("build/stats$name.csv").absoluteFile
    private val statsOutput: BufferedWriter

    init {
        statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln("File, ProcessID, Time")
    }

    fun append(file: String, id: String, nanoTime: Long) {
        statsOutput.appendln(buildString {
            append(file)
            append(", ")
            append(id)
            append(", ")
            append(nanoTime.nsToMs)
        })
        statsOutput.flush()
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
    println("##teamcity[testFinished name='$name' duration='$time']")
}

inline fun tcSimplePerfTest(file: String, name: String, stats: Stats, block: () -> Unit) {
    var spentMs = 0L
    tcTest(name) {
        val errors = mutableListOf<Throwable>()
        val spentNs = measureNanoTime {
            try {
                block()
            } catch (t: Throwable) {
                errors += t
            }
        }
        stats.append(file, name, spentNs)
        spentMs = spentNs.nsToMs
        spentMs to errors
    }
    println("##teamcity[buildStatisticValue key='$name' value='$spentMs']")
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
