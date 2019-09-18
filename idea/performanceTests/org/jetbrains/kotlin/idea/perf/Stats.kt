/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.WholeProjectPerformanceTest.Companion.nsToMs
import org.jetbrains.kotlin.idea.perf.profilers.async.ProfilerHandler
import org.jetbrains.kotlin.idea.testFramework.logMessage
import org.jetbrains.kotlin.util.PerformanceCounter
import java.io.*
import kotlin.system.measureNanoTime
import java.lang.ref.WeakReference
import kotlin.math.*
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

typealias StatInfos = Map<String, Any>?

class Stats(val name: String = "", val header: Array<String> = arrayOf("Name", "ValueMS", "StdDev")) : Closeable {

    private val perfTestRawDataMs = mutableListOf<Long>()

    private val statsFile: File = File(pathToResource("stats${statFilePrefix()}.csv")).absoluteFile

    private val statsOutput: BufferedWriter

    private val profilerHandler = ProfilerHandler.getInstance()

    init {
        statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln(header.joinToString())

        PerformanceCounter.setTimeCounterEnabled(true)
    }

    private fun statFilePrefix() = if (name.isNotEmpty()) "-${plainname()}" else ""

    private fun plainname() = name.toLowerCase().replace(' ', '-').replace('/', '_')

    private fun pathToResource(resource: String) = "build/$resource"

    private fun append(id: String, statInfosArray: Array<StatInfos>) {
        val timingsMs = toTimingsMs(statInfosArray)

        val calcMean = calcMean(timingsMs)

        for (v in listOf(
            Triple("mean", "", calcMean.mean.toLong()),
            Triple("stdDev", " stdDev", calcMean.stdDev.toLong()),
            Triple("geomMean", "geomMean", calcMean.geomMean.toLong())
        )) {
            val n = "$id : ${v.first}"

            printTestStarted(n)
            printStatValue("$id${v.second}", v.third)
            printTestFinished(n, v.third, includeStatValue = false)
        }

        statInfosArray.filterNotNull()
            .map { it.keys }
            .fold(mutableSetOf<String>(), { acc, keys -> acc.addAll(keys); acc })
            .filter { it != TEST_KEY && it != ERROR_KEY }
            .sorted().forEach { perfCounterName ->
                val values = statInfosArray.map { (it?.get(perfCounterName) as Long) }.toLongArray()
                val statInfoMean = calcMean(values)

                val n = "$id : $perfCounterName"
                val mean = statInfoMean.mean.toLong()

                val shortName = if (perfCounterName.endsWith(": time")) n.removeSuffix(": time") else null

                shortName?.let { printTestStarted(it) }
                printStatValue(n, mean)
                shortName?.let { printTestFinished(it, mean, includeStatValue = false) }
            }

        perfTestRawDataMs.addAll(timingsMs.toList())
        append(arrayOf(id, calcMean.mean, calcMean.stdDev))
    }

    private fun toTimingsMs(statInfosArray: Array<StatInfos>) =
        statInfosArray.map { info -> info?.let { it[TEST_KEY] as? Long }?.nsToMs ?: 0L }.toLongArray()

    private fun calcMean(statInfosArray: Array<StatInfos>): Mean = calcMean(toTimingsMs(statInfosArray))

    private fun calcMean(values: LongArray): Mean {
        val mean = values.average()

        val stdDev = if (values.size > 1) (sqrt(
            values.fold(0.0,
                        { accumulator, next -> accumulator + (1.0 * (next - mean)).pow(2) })
        ) / (values.size - 1))
        else 0.0

        val geomMean = geomMean(values.toList())

        return Mean(mean, stdDev, geomMean)
    }

    data class Mean(val mean: Double, val stdDev: Double, val geomMean: Double)

    private fun append(values: Array<Any>) {
        require(values.size == header.size) { "Expected ${header.size} values, actual ${values.size} values" }
        with(statsOutput) {
            appendln(values.joinToString { it.toString() })
            flush()
        }
    }

    fun append(file: String, id: String, nanoTime: Long) {
        val ms = nanoTime.nsToMs
        append(arrayOf(file, id, ms))
    }

    fun <SV, TV> perfTest(
        testName: String,
        warmUpIterations: Int = 5,
        iterations: Int = 20,
        setUp: (TestData<SV, TV>) -> Unit = { },
        test: (TestData<SV, TV>) -> Unit,
        tearDown: (TestData<SV, TV>) -> Unit = { }
    ) {

        tcSuite(testName) {
            warmUpPhase(warmUpIterations, testName, setUp, test, tearDown)
            val statInfoArray = mainPhase(iterations, testName, setUp, test, tearDown)

            assertEquals(iterations, statInfoArray.size)
            appendTimings(testName, statInfoArray)
        }
    }

    private fun printTimings(
        prefix: String,
        statInfoArray: Array<StatInfos>,
        attemptFn: (Int) -> String = { attempt -> "#$attempt" }
    ) {
        for (statInfoIndex in statInfoArray.withIndex()) {
            val attempt = statInfoIndex.index
            val statInfo = statInfoIndex.value!!
            val n = "$name: $prefix ${attemptFn(attempt)}"
            printTestStarted(n)
            val t = statInfo[ERROR_KEY] as? Throwable
            if (t != null) printTestFinished(n, t) else {
                for ((k, v) in statInfo) {
                    if (k == TEST_KEY) continue
                    printStatValue("$n $k", v)
                }

                printTestFinished(n, (statInfo[TEST_KEY] as Long).nsToMs)
            }
        }
    }

    fun printWarmUpTimings(
        prefix: String,
        warmUpStatInfosArray: Array<StatInfos>
    ) = printTimings(prefix, warmUpStatInfosArray) { attempt -> "warm-up #$attempt" }

    fun appendTimings(
        prefix: String,
        statInfosArray: Array<StatInfos>
    ) {
        printTimings(prefix, statInfosArray)
        append("$name: $prefix", statInfosArray)
    }

    private fun <K, T> mainPhase(
        iterations: Int,
        testName: String,
        setUp: (TestData<K, T>) -> Unit,
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit
    ): Array<StatInfos> {
        val statInfosArray = phase(testName, "", iterations, setUp, test, tearDown)

        // do not estimate stability for warm-up
        if (testName.contains(WARM_UP)) {
            val calcMean = calcMean(statInfosArray)
            val stabilityPercentage = round(calcMean.stdDev * 100.0 / calcMean.mean).toInt()
            logMessage { "$testName stability is $stabilityPercentage %" }
            check(stabilityPercentage <= 10) { "$testName is not stable: stability above $stabilityPercentage %" }
        }

        return statInfosArray
    }

    private fun <K, T> warmUpPhase(
        warmUpIterations: Int,
        testName: String,
        setUp: (TestData<K, T>) -> Unit,
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit
    ) {
        val warmUpStatInfosArray = phase(testName, "warm-up", warmUpIterations, setUp, test, tearDown)

        printWarmUpTimings(testName, warmUpStatInfosArray)

        warmUpStatInfosArray.filterNotNull().map { it[ERROR_KEY] as? Exception }.firstOrNull()?.let { throw it }
    }

    private fun <K, T> phase(
        namePrefix: String,
        phaseName: String,
        iterations: Int,
        setUp: (TestData<K, T>) -> Unit,
        test: (TestData<K, T>) -> Unit,
        tearDown: (TestData<K, T>) -> Unit
    ): Array<StatInfos> {
        val statInfosArray = Array<StatInfos>(iterations) { null }
        val testData = TestData<K, T>(null, null)
        val profilerPath = pathToResource("profile/${plainname()}/")
        check(with(File(profilerPath)) { exists() || mkdirs() }) { "unable to mkdirs $profilerPath for $namePrefix" }
        try {
            for (attempt in 0 until iterations) {
                testData.reset()
                triggerGC(attempt)

                val setUpMillis = measureTimeMillis {
                    setUp(testData)
                }
                logMessage { "setup took $setUpMillis ms" }
                val valueMap = HashMap<String, Any>(2 * PerformanceCounter.numberOfCounters + 1)
                statInfosArray[attempt] = valueMap
                try {
                    val activityName = "$namePrefix-${if (phaseName.isEmpty()) "" else "$phaseName-"}$attempt"
                    profilerHandler.startProfiling(activityName)
                    valueMap[TEST_KEY] = measureNanoTime {
                        test(testData)
                    }
                    profilerHandler.stopProfiling(profilerPath, activityName)
                    PerformanceCounter.report { name, counter, nanos ->
                        valueMap["counter \"$name\": count"] = counter.toLong()
                        valueMap["counter \"$name\": time"] = nanos.nsToMs
                    }

                } catch (t: Throwable) {
                    println("# error at $namePrefix #$attempt:")
                    t.printStackTrace()
                    valueMap[ERROR_KEY] = t
                } finally {
                    try {
                        val tearDownMillis = measureTimeMillis {
                            tearDown(testData)
                        }
                        logMessage { "tearDown took $tearDownMillis ms" }
                    } finally {
                        PerformanceCounter.resetAllCounters()
                    }
                }
            }
        } catch (t: Throwable) {
            println("error at $namePrefix:")
            tcPrintErrors(namePrefix, listOf(t))
        }
        return statInfosArray
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

    private fun geomMean(data: List<Long>) = exp(data.fold(0.0, { mul, next -> mul + ln(1.0 * next) }) / data.size)

    override fun close() {
        if (perfTestRawDataMs.isNotEmpty()) {
            val geomMeanMs = geomMean(perfTestRawDataMs.toList()).toLong()
            printStatValue("$name geomMean", geomMeanMs)
            append(arrayOf("$name geomMean", geomMeanMs, 0))
        }
        statsOutput.flush()
        statsOutput.close()
    }

    companion object {
        const val TEST_KEY = "test"
        const val ERROR_KEY = "error"

        const val WARM_UP = "warm-up"

        inline fun runAndMeasure(note: String, block: () -> Unit) {
            val openProjectMillis = measureTimeMillis {
                block()
            }
            logMessage { "$note took $openProjectMillis ms" }
        }

        fun printTestStarted(testName: String) {
            println("##teamcity[testStarted name='$testName' captureStandardOutput='true']")
        }

        fun printTestFinished(testName: String, spentMs: Long, includeStatValue: Boolean = true) {
            if (includeStatValue) {
                printStatValue(testName, spentMs)
            }
            println("##teamcity[testFinished name='$testName' duration='$spentMs']")
        }

        fun printStatValue(name: String, value: Any) {
            println("##teamcity[buildStatisticValue key='$name' value='$value']")
        }

        fun printTestFinished(testName: String, error: Throwable) {
            println("error at $testName:")
            tcPrintErrors(testName, listOf(error))

            printStatValue(testName, -1)
            println("##teamcity[testFinished name='$testName']")
        }

        inline fun tcSuite(name: String, block: () -> Unit) {
            println("##teamcity[testSuiteStarted name='$name']")
            try {
                block()
            } finally {
                println("##teamcity[testSuiteFinished name='$name']")
            }
        }

        inline fun tcTest(name: String, block: () -> Pair<Long, List<Throwable>>) {
            printTestStarted(name)
            val (time, errors) = block()
            tcPrintErrors(name, errors)
            printTestFinished(name, time, includeStatValue = false)
        }

        private fun tcEscape(s: String) = s.replace("|", "||")
            .replace("[", "|[")
            .replace("]", "|]")
            .replace("\r", "|r")
            .replace("\n", "|n")
            .replace("'", "|'")

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
                println("##teamcity[testFailed name='$name' message='Exceptions reported' details='${tcEscape(details)}']")
            }
        }
    }
}

data class TestData<SV, TV>(var setUpValue: SV?, var value: TV?) {
    fun reset() {
        setUpValue = null
        value = null
    }
}