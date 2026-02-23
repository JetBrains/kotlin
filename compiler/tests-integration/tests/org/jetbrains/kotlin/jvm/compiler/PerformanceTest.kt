/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("KotlinConstantConditions") // Avoid warnings on generation constant changing during experimenting

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.stats.ModulesReportsData
import org.jetbrains.kotlin.stats.StatsCalculator
import org.jetbrains.kotlin.util.UnitStats
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTime

class PerformanceTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String get() = tmpdir.absolutePath

    /**
     * Reproduction of https://youtrack.jetbrains.com/issue/KT-83191
     *
     * It simulates a large classpath scenario by creating a large number of modules with nested packages and classes.
     * After that, it measures the kotlin compilation performance of files that import generated classes from first and last roots (best and worst scenarios).
     */
    fun testLargeClasspathsPerformance() {
        // Set up realistic but quite large values to emulate a huge monorepo project
        val rootsCount = 3000

        // Max package depth in each root. The first package is a root package.
        // Probably the number doesn't matter a lot (in terms of roots enumeration), but let's have it realistic
        val packageDepth = 2

        // Set up how packages are branched on each depth level
        // It seems like doesn't matter a lot to reveal the problem
        val packageBranchingDepth = 1

        // Generate the specified number of classes in each package.
        // The total number of classes is rootsCount * packageDepth * packageBranchingDepth * classesInPackageCount
        // Enumeration of children in a specified directory has logarithm complexity, so the number should be large to cause some performance problems.
        // However, it significantly slows down the preparation step, so let's keep it realistic but not too large.
        val classesInPackageCount = 10

        // If true, compile jar instead of class files
        val checkJars = false

        // Simulate a large number of imported classes that we are trying to resolve to simulate expensive roots enumerating
        val importedClassesCount = 6000

        val firstRootIndex = 0
        val lastRootIndex = rootsCount - 1

        // Put the imported classes into the latest package.
        // Probably it makes sense to also check a case when importedClassesDepthIndex is 0
        // because it forces ALL roots to be enumerated (including standard JVM libs) but not only project ones
        val importedClassesDepthIndex = packageDepth - 1
        val importedClassesBranchingDepthIndex = packageBranchingDepth - 1

        printTimeStamp()
        println("Roots count: $rootsCount")
        println("Package depth: $packageDepth")
        println("Package branching depth: $packageBranchingDepth")
        println("Classes in package count: $classesInPackageCount")
        println("Imported classes count: $importedClassesCount")
        println("Generation mode: ${if (checkJars) "jar" else "class files"}")

        data class GenPackageInfo(val packageDir: String, val packageName: String, val packageFragmentName: String) {
            /**
             * Create unique class names.
             * The assumption is: it's the most realistic scenario when almost all identifiers are unique (even disregarding package name)
             * In a real project (even large) the number of classes with the same id but in different package is quite small.
             */
            fun getClassName(rootIndex: Int, index: Int) = "Class_r${rootIndex}" +
                    (if (packageFragmentName.isNotEmpty()) "_${packageFragmentName}" else "") +
                    "_i$index"
        }

        fun generateGenFileInfo(packageDepth: Int, packageBranchingDepthIndex: Int): GenPackageInfo {
            val packageDirBuilder = StringBuilder()
            val packageNameBuilder = StringBuilder()
            var lastPackageFragmentName = ""

            // Just drop the package id in the root (it's also allowed)
            for (packageDepthIndex in 0..<packageDepth) {
                lastPackageFragmentName = "f_${packageDepthIndex}_${packageBranchingDepthIndex}"
                packageDirBuilder.append((if (packageDirBuilder.isEmpty()) "" else "/") + lastPackageFragmentName)
                packageNameBuilder.append((if (packageNameBuilder.isEmpty()) "" else ".") + lastPackageFragmentName)
            }

            return GenPackageInfo(packageDirBuilder.toString(), packageNameBuilder.toString(), lastPackageFragmentName)
        }

        fun generateAndWriteJavaFile(packageDir: File, packageName: String, className: String): File {
            return File(packageDir, "$className.java").apply {
                writeText(
                    buildString {
                        if (packageName.isNotEmpty()) {
                            append("package $packageName;")
                        }
                        append("""public class $className {""")
                        append("""    public static String getValue() { return "$className"; }""")
                        append("}")
                    }
                )
            }
        }

        val classPaths = mutableListOf<File>()
        var totalNumberOfGeneratedJavaFiles = 0

        val generationTime = measureTime {
            for (rootIndex in 0..<rootsCount) {
                val rootName = "root_$rootIndex"
                val rootDir = File("$testDataDirectory/$rootName")

                for (packageDepthIndex in 0..<packageDepth) {
                    for (packageBranchingDepthIndex in 0..<packageBranchingDepth) {
                        val genFileInfo = generateGenFileInfo(packageDepthIndex, packageBranchingDepthIndex)
                        val (packageDir, packageName, _) = genFileInfo
                        val pkgDir = File("$rootDir/$packageDir")
                        pkgDir.mkdirs()

                        // Store imported classes into the last root to reproduce the worst case
                        // Because all roots are being traversed sequentially
                        val classesCount = if (
                            (rootIndex == firstRootIndex || rootIndex == lastRootIndex) && packageDepthIndex == importedClassesDepthIndex
                        ) {
                            importedClassesCount
                        } else {
                            classesInPackageCount
                        }

                        for (importClassIndex in 0..<classesCount) {
                            generateAndWriteJavaFile(
                                pkgDir,
                                packageName,
                                genFileInfo.getClassName(rootIndex, importClassIndex)
                            )
                            totalNumberOfGeneratedJavaFiles++
                        }
                    }
                }

                val libraryName = if (!checkJars) rootName else "$rootName.jar"
                val libraryFile = File(testDataDirectory, libraryName)
                compileLibrary(rootName, destination = libraryFile, cleanupAfterCompilation = true)
                classPaths.add(if (!checkJars) rootDir else libraryFile)
            }
        }

        println("Generation time: ${generationTime.inWholeMilliseconds} ms")
        println("Total number of generated Java classes: $totalNumberOfGeneratedJavaFiles")
        println()

        fun generateAndCompileKotlinFile(compilationType: LargeClasspathsCompilationMode): Duration {
            val fileName = "$compilationType.kt"
            // It looks like it's enough to have only a single kotlin file that simulates multiple files by one super huge import list
            // Because each declaration is being cached once it's resolved.
            File(testDataDirectory, fileName).apply {
                val content = buildString {
                    val rootIndex: Int
                    val importedClassesFileInfo = if (compilationType != LargeClasspathsCompilationMode.Warmup) {
                        rootIndex = if (compilationType == LargeClasspathsCompilationMode.FirstClasspathClasses) {
                            firstRootIndex
                        } else {
                            lastRootIndex
                        }
                        generateGenFileInfo(importedClassesDepthIndex, importedClassesBranchingDepthIndex)
                    } else {
                        rootIndex = -1
                        null
                    }

                    if (importedClassesFileInfo?.packageName?.isNotEmpty() == true) {
                        for (importClassIndex in 0..<importedClassesCount) {
                            appendLine(
                                "import ${importedClassesFileInfo.packageName}.${importedClassesFileInfo.getClassName(rootIndex, importClassIndex)}"
                            )
                        }
                        appendLine()
                    }

                    appendLine("fun ${compilationType.toString().replaceFirstChar { it.lowercase() }}() {")

                    if (importedClassesFileInfo != null) {
                        for (importClassIndex in 0..<importedClassesCount) {
                            // Probably it makes sense to check when it's uncommented,
                            // But in this test we are interested only in jvm indexing performance, but not in FIR resolving
                            //appendLine("    println(${lastGenFileInfo.getClassName(rootIndex, importClassIndex)}.getValue())")
                        }
                    }
                    appendLine('}')
                }
                writeText(content)
            }

            val compilationTime = measureTime {
                val (output, exitCode) = compileKotlin(
                    fileName,
                    testDataDirectory,
                    classPaths,
                    expectedFileName = null,
                )
                assertEmpty(output)
                assertEquals(ExitCode.OK, exitCode)
            }

            println("Compilation time ($fileName): ${compilationTime.inWholeMilliseconds} ms")

            return compilationTime
        }

        generateAndCompileKotlinFile(LargeClasspathsCompilationMode.Warmup)
        val classesFromFirstClasspathCompileTime = generateAndCompileKotlinFile(LargeClasspathsCompilationMode.FirstClasspathClasses)
        val classesFromLastClasspathCompileTime = generateAndCompileKotlinFile(LargeClasspathsCompilationMode.LastClasspathClasses)

        printTimeDiff(
            classesFromLastClasspathCompileTime.inWholeNanoseconds,
            classesFromFirstClasspathCompileTime.inWholeNanoseconds,
            LargeClasspathsCompilationMode.LastClasspathClasses,
            LargeClasspathsCompilationMode.FirstClasspathClasses,
        )
    }

    enum class LargeClasspathsCompilationMode {
        Warmup,
        FirstClasspathClasses,
        LastClasspathClasses;
    }

    /**
     * Reproduction of https://youtrack.jetbrains.com/issue/KT-75655
     *
     * It simulates multiple modules by running separated compilations for them.
     * Currently, it checks redundant deserialization only for standard dependencies (jdk, stdlib)
     */
    fun testVirtualFilesCaching() {
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

    private fun printTimeStamp() {
        println("Timestamp: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
    }

    private fun <T : Enum<T>> printTimeDiff(
        expectedLargeNanos: Long,
        expectedSmallNanos: Long,
        expectedLargeTimeCompileMode: Enum<T>,
        expectedSmallTimeCompileMode: Enum<T>,
        description: String? = null,
    ) {
        val diff = expectedLargeNanos - expectedSmallNanos
        val ratio = expectedLargeNanos.toDouble() / expectedSmallNanos

        println(
            buildString {
                append("${expectedLargeTimeCompileMode}/${expectedSmallTimeCompileMode} diff")
                if (description != null) {
                    append(" ($description)")
                }
                append(": ${TimeUnit.NANOSECONDS.toMillis(diff)} ms (ratio: ${String.format(Locale.ENGLISH, "%.4f", ratio)})")
            }
        )

        assert(diff > 0) { "The number of generated files is too small to provide meaningful performance difference or the problem is already fixed." }

        println()
    }
}