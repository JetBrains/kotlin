import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import java.util.concurrent.TimeUnit
import java.io.*
import kotlin.time.Duration.Companion.nanoseconds

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val moduleStatsSerializer = ListSerializer(UnitStatsSerializer)

fun main(args: Array<String>) {
    val jsonText = File(args.first()).readText()
    val normalizedJson = if (!jsonText.startsWith('[')) {
        '[' + jsonText.trimEnd().removeSuffix(",") + ']'
    } else {
        jsonText
    }

    val modulesStats = ModulesStats(Json.decodeFromString(moduleStatsSerializer, normalizedJson))
    val totalStats = modulesStats.totalStats

    println("# Info")
    println()
    println("* Platform: ${totalStats.platform}")
    println("* Has errors: ${totalStats.hasErrors}")
    println()

    val totalTime = totalStats.totalTime()
    println("# Time ratio")
    println()
    println(String.format("| %18s | %7s | %7s | %7s |", "", "Nano", "User", "Cpu"))
    printTimeAndRatio("INIT", totalStats.initStats!!.time, totalTime)
    printTimeAndRatio("ANALYSIS", totalStats.analysisStats!!.time, totalTime)
    printTimeAndRatio("TRANSLATION to IR", totalStats.irGenerationStats!!.time, totalTime)
    printTimeAndRatio("IR LOWERING", totalStats.irLoweringStats!!.time, totalTime)
    printTimeAndRatio("BACKEND", totalStats.backendStats!!.time, totalTime)
    println("")
    printTimeAndRatio("FIND JAVA CLASS", totalStats.findJavaClassStats!!.time, totalTime)
    printTimeAndRatio("FIND KOTLIN CLASS", totalStats.findKotlinClassStats!!.time, totalTime)
    println("-".repeat(50))
    printTimeAndRatio("TOTAL", totalTime, totalTime)
    println()

    val analysisStatsTime = totalStats.analysisStats!!.time

    modulesStats.printModulesBy("star imports") { it.analysisStats!!.starImportsCount }
    /*modulesStats.printModulesBy("total time") { it.totalTime().nano }
    modulesStats.printModulesBy("total time (user)") { it.totalTime().userNano }
    modulesStats.printModulesBy("total time (cpu)") { it.totalTime().cpuNano }*/
    modulesStats.printModulesBy(
        "analysis time",
        printer = { stats, nanos ->
            String.format("%d ms, %.2f%%", TimeUnit.NANOSECONDS.toMillis(nanos!!), nanos.toDouble() / analysisStatsTime.nano * 100)
        }
    ) { it.analysisStats!!.time.nano }
    modulesStats.printModulesBy(
        "analysis time (user)",
        printer = { stats, nanos ->
            String.format("%d ms, %.2f%%", TimeUnit.NANOSECONDS.toMillis(nanos!!), nanos.toDouble() / analysisStatsTime.userNano * 100)
        }
    ) { it.analysisStats!!.time.userNano }

    modulesStats.printModulesBy("Find Kotlin Class Bytes") {
        it.findKotlinClassStats!!.bytesCount
    }
    modulesStats.printModulesBy("Find Java Class Bytes") {
        it.findJavaClassStats!!.bytesCount
    }
    modulesStats.printModulesBy("Find Kotlin ms", printer = {
            stats, nanos -> "${TimeUnit.NANOSECONDS.toMillis(nanos!!)} ms}"
    }) {
        it.findKotlinClassStats!!.time.userNano
    }
    modulesStats.printModulesBy("Find Java ms", printer = {
        stats, nanos -> "${TimeUnit.NANOSECONDS.toMillis(nanos!!)} ms}"
    }) {
        it.findJavaClassStats!!.time.userNano
    }

    /*modulesStats.printModulesBy("LPS", max = false) { unitStats ->
        unitStats.entitiesPerSecond({ it.initStats!!.linesCount.toLong() }) {
            it.totalTime().nano
        }
    }*/
    /*modulesStats.printModulesBy("LPS (user time)", max = false, printer = {
        unitStats, r -> "lps: $r, lines count: ${unitStats.analysisStats!!.allNodesCount}"
    }) { unitStats ->
        unitStats.entitiesPerSecond({ it.analysisStats!!.allNodesCount.let { it -> if (it < 1000) Long.MAX_VALUE else it.toLong() }}) {
            it.analysisStats!!.time.userNano
        }
    }*/
    /*modulesStats.printModulesBy("FIR nodes per second", max = false) { unitStats ->
        unitStats.entitiesPerSecond({ it.analysisStats!!.leafNodesCount.toLong() }) {
            it.totalTime().nano
        }
    }*/
    println("Total time: ${totalTime.millis} ms")
    println("Total Java class bytes count: ${totalStats.findJavaClassStats!!.bytesCount} bytes")
    println("Total Kotlin class bytes count: ${totalStats.findKotlinClassStats!!.bytesCount} bytes")
}

private fun printTimeAndRatio(name: String, time: Time, wholeTime: Time) {
    val timeRatio = time / wholeTime
    println(
        String.format(
            "| %18s | %6.2f%% | %6.2f%% | %6.2f%% |",
            name,
            timeRatio.nanosRatio * 100,
            timeRatio.userNanosRatio * 100,
            timeRatio.cpuNanosRatio * 100
        )
    )
}

private fun <R : Comparable<R>> ModulesStats.printModulesBy(
    description: String,
    max: Boolean = true,
    printer: ((UnitStats, R?) -> String) = { _, r -> r.toString() },
    selector: (UnitStats) -> R?,
) {
    val descriptionPrefix: String
    val sortedModules = if (max) {
        descriptionPrefix = "Max by"
        modulesStats.sortedByDescending(selector)
    } else {
        descriptionPrefix = "Min by"
        modulesStats.sortedBy(selector)
    }
    println("# $descriptionPrefix $description")
    println()
    sortedModules.take(16).forEach {
        println("  * ${it.name} (${printer(it, selector(it))})")
    }
    println("  * ...")
    println("")
}

