/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.module.impl.ProjectLoadingErrorsHeadlessNotifier
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.perf.util.*
import org.jetbrains.kotlin.idea.testFramework.ProjectOpenAction
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3RunnerWithInners::class)
class HighlightWholeProjectPerformanceTest : UsefulTestCase() {

    override fun setUp() {
        val allowedErrorDescription = setOf(
            "Unknown artifact type: war",
            "Unknown artifact type: exploded-war"
        )

        ProjectLoadingErrorsHeadlessNotifier.setErrorHandler(
            { errorDescription ->
                val description = errorDescription.description
                if (description !in allowedErrorDescription) {
                    throw RuntimeException(description)
                } else {
                    logMessage { "project loading error: '$description' at '${errorDescription.elementName}'" }
                }
            }, testRootDisposable
        )
    }

    fun testHighlightAllKtFilesInProject() {
        val emptyProfile = System.getProperty("emptyProfile", "false").toBoolean()
        val projectSpecs = projectSpecs()
        for (projectSpec in projectSpecs) {
            val projectName = projectSpec.name
            val projectPath = projectSpec.path

            suite(suiteName = "$projectName project") {
                app {
                    warmUpProject()

                    with(config) {
                        warmup = 1
                        iterations = 3
                    }

                    try {
                        project(ExternalProject(projectPath, ProjectOpenAction.GRADLE_PROJECT), refresh = true) {
                            profile(if (emptyProfile) EmptyProfile else DefaultProfile)

                            val projectDir = File(projectPath)

                            val ktFiles = projectDir.allFilesWithExtension("kt").toList()
                            logStatValue("number of kt files", ktFiles.size)
                            val topMidLastFiles =
                                limitedFiles(ktFiles, 10)
                                    .map {
                                        val path = it.path
                                        it to path.substring(path.indexOf(projectPath) + projectPath.length + 1)
                                    }
                            logStatValue("limited number of kt files", topMidLastFiles.size)

                            topMidLastFiles.forEach {
                                logMessage { "${it.second} fileSize: ${it.first.length()}" }
                            }

                            topMidLastFiles.forEachIndexed { idx, pair ->
                                val file = pair.first
                                val fileName = pair.second
                                logMessage { "$idx / ${topMidLastFiles.size} : $fileName fileSize: ${file.length()}" }

                                try {
                                    fixture(fileName).use {
                                        measure<List<HighlightInfo>>(fileName) {
                                            test = {
                                                highlight(it)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // nothing as it is already caught by perfTest
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // nothing as it is already caught by perfTest
                    }
                }

            }
        }
    }

    private fun limitedFiles(ktFiles: List<File>, partPercent: Int): Collection<File> {
        val sortedBySize = ktFiles
            .filter { it.length() > 0 }
            .map { it to it.length() }.sortedByDescending { it.second }
        val percentOfFiles = (sortedBySize.size * partPercent) / 100

        val topFiles = sortedBySize.take(percentOfFiles).map { it.first }
        val midFiles =
            sortedBySize.take(sortedBySize.size / 2 + percentOfFiles / 2).takeLast(percentOfFiles).map { it.first }
        val lastFiles = sortedBySize.takeLast(percentOfFiles).map { it.first }

        return LinkedHashSet(topFiles + midFiles + lastFiles)
    }

    private fun projectSpecs(): List<ProjectSpec> {
        val projects = System.getProperty("performanceProjects") ?: return emptyList()
        return projects.split(",").map {
            val idx = it.indexOf("=")
            if (idx <= 0) ProjectSpec(it, "../$it") else ProjectSpec(it.substring(0, idx), it.substring(idx + 1))
        }.filter {
            val path = File(it.path)
            path.exists() && path.isDirectory
        }
    }

    private data class ProjectSpec(val name: String, val path: String)
}