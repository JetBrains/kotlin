/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import java.io.File

// You can see "How To" via link: https://jetbrains.quip.com/xQ2WAUy9bZmy/How-to-use-AggregateTest-task
open class AggregateTest : Test() { // Inherit from Test to see test results in IDEA Test viewer
    private var patterns: MutableMap<String, MutableList<String>> = mutableMapOf()

    @InputFile
    lateinit var testPatternFile: File

    init {
        // Set empty FileCollection to avoid NPE when initializing a base 'Test' class
        classpath = project.objects.fileCollection()
        testClassesDirs = project.objects.fileCollection()

        project.gradle.taskGraph.whenReady {
            if (allTasks.filterIsInstance<AggregateTest>().isNotEmpty()) {
                initPatterns()
                allTasks.filterIsInstance<Test>().forEach { testTask -> subTaskConfigure(testTask) }

                if (!project.gradle.startParameter.taskNames.all { project.tasks.findByPath(it) is AggregateTest }) {
                    logger.warn("Please, don't use AggregateTest and non-AggregateTest test tasks together. You can get incorrect results.")
                }
            }
        }
    }

    private fun initPatterns() {
        if (!testPatternFile.exists())
            throw GradleException("File with test patterns is not found")
        testPatternFile
            .readLines()
            .asSequence()
            .filter { it.isNotEmpty() }
            .forEach { line ->
                // patternType is exclude or include value
                val (pattern, patternType) = line.split(',').map { it.trim() }
                patterns.getOrPut(patternType) { mutableListOf() }.add(pattern)
            }
    }

    private fun subTaskConfigure(testTask: Test) {
        testTask.doNotTrackState("State is tracked by AggregateTest task")
        testTask.ignoreFailures = true
        testTask.filter {
            isFailOnNoMatchingTests = false
            patterns["include"]?.let {
                it.forEach { pattern ->
                    includeTestsMatching(pattern)
                }
            }
            patterns["exclude"]?.let {
                it.forEach { pattern ->
                    excludeTestsMatching(pattern)
                }
            }
        }
    }

    @Override
    @TaskAction
    override fun executeTests() {
        // Do nothing
    }
}