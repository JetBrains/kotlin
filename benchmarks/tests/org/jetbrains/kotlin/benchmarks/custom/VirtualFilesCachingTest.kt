/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("KotlinConstantConditions") // Avoid warnings on generation constant changing during experimenting

package org.jetbrains.kotlin.benchmarks.custom

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.jvm.compiler.AbstractKotlinCompilerIntegrationTest
import org.jetbrains.kotlin.stats.ModulesReportsData
import org.jetbrains.kotlin.stats.StatsCalculator
import org.jetbrains.kotlin.util.UnitStats
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Reproduction of https://youtrack.jetbrains.com/issue/KT-75655
 *
 * It simulates multiple modules by running separated compilations for them.
 * Currently, it checks redundant deserialization only for standard dependencies (jdk, stdlib)
 */
class VirtualFilesCachingTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String get() = tmpdir.absolutePath

    fun test() {
        val modulesCount = 2000

        testDataDirectory.mkdirs()

        printTimeStamp()
        println("Modules count: $modulesCount")
        println()

        val kotlinFiles = buildList {
            for (moduleIndex in 0..<modulesCount) {
                add(File(testDataDirectory, "file${moduleIndex}.kt").apply {
                    // Write just an empty file, it's allowed
                    writeText("")
                })
            }
        }

        fun getDurationAndStats(mode: VirtualFilesCachingMode): Pair<Duration, UnitStats> {
            val duration: Duration
            val totalStats: UnitStats

            println("Mode: $mode")

            when (mode) {
                VirtualFilesCachingMode.Warmup -> {
                    val compiler = K2JVMCompiler()
                    val emptyFile = File(testDataDirectory, "empty.kt").apply {
                        writeText("fun empty() {}")
                    }

                    duration = measureTime {
                        val (output, exitCode) = compileKotlin(
                            emptyFile.name,
                            testDataDirectory,
                            expectedFileName = null,
                            compiler = compiler,
                        )
                        assertEmpty(output)
                        assertEquals(ExitCode.OK, exitCode)
                    }

                    totalStats = compiler.defaultPerformanceManager.unitStats
                }

                VirtualFilesCachingMode.SingleModule -> {
                    val compiler = K2JVMCompiler()
                    duration = measureTime {
                        val (output, exitCode) = compileKotlin(
                            kotlinFiles.first().name,
                            testDataDirectory,
                            expectedFileName = null,
                            additionalSources = kotlinFiles.drop(1).map { it.name },
                            compiler = compiler,
                        )
                        assertEmpty(output)
                        assertEquals(ExitCode.OK, exitCode)
                    }

                    totalStats = compiler.defaultPerformanceManager.unitStats
                }

                VirtualFilesCachingMode.MultipleModules -> {
                    val aggregatedStats = mutableMapOf<String, UnitStats>()
                    duration = measureTime {
                        for (kotlinFile in kotlinFiles) {
                            val compiler = K2JVMCompiler()
                            val (output, exitCode) = compileKotlin(
                                kotlinFile.name,
                                testDataDirectory,
                                expectedFileName = null,
                                compiler = compiler,
                            )
                            assertEmpty(output)
                            assertEquals(ExitCode.OK, exitCode)
                            aggregatedStats[kotlinFile.name] = compiler.defaultPerformanceManager.unitStats
                        }
                        assertEquals(kotlinFiles.size, aggregatedStats.size)
                    }
                    totalStats = StatsCalculator(ModulesReportsData(aggregatedStats)).totalStats
                }
            }

            println("Compilation time: ${duration.inWholeMilliseconds} ms")
            val findKotlinClassStats = totalStats.findKotlinClassStats!!
            println("Binary files read count: ${findKotlinClassStats.count}")
            println("Binary files time spend: ${TimeUnit.NANOSECONDS.toMillis(findKotlinClassStats.time.nanos)} ms")
            println()

            return duration to totalStats
        }

        getDurationAndStats(VirtualFilesCachingMode.Warmup)

        val (singleModuleDuration, singleModuleStats) = getDurationAndStats(VirtualFilesCachingMode.SingleModule)

        // Simulate compilation of multiple modules
        val (multipleModulesDuration, multipleModulesStats) = getDurationAndStats(VirtualFilesCachingMode.MultipleModules)

        printTimeDiff(
            multipleModulesDuration.inWholeNanoseconds,
            singleModuleDuration.inWholeNanoseconds,
            VirtualFilesCachingMode.MultipleModules,
            VirtualFilesCachingMode.SingleModule,
            "compile"
        )

        // We are mostly interested in the deserialization time rather than in the whole compile time
        // Because compilation of multiple modules has a large overhead and reveals little about deserialization performance
        printTimeDiff(
            multipleModulesStats.findKotlinClassStats!!.time.nanos,
            singleModuleStats.findKotlinClassStats!!.time.nanos,
            VirtualFilesCachingMode.MultipleModules,
            VirtualFilesCachingMode.SingleModule,
            "files deserialization"
        )
    }

    private enum class VirtualFilesCachingMode {
        Warmup,
        SingleModule,
        MultipleModules,
    }
}