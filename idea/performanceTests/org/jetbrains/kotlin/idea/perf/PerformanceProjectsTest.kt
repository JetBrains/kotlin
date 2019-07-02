/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.junit.AfterClass
import org.junit.BeforeClass

class PerformanceProjectsTest : AbstractPerformanceProjectsTest() {

    companion object {

        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val hwStats: Stats = Stats("helloWorld project")

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            hwStats.close()
        }
    }

    override fun setUp() {
        super.setUp()
        // warm up: open simple small project
        if (!warmedUp) {
            val project = innerPerfOpenProject("helloKotlin", hwStats, "warm-up")
            val perfHighlightFile = perfHighlightFile(project, "src/HelloMain.kt", hwStats, "warm-up")
            assertTrue("kotlin project has been not imported properly", perfHighlightFile.isNotEmpty())
            closeProject(project)

            warmedUp = true
        }
    }

    fun testHelloWorldProject() {
        tcSuite("Hello world project") {
            perfOpenProject("helloKotlin", hwStats)

            // highlight
            perfHighlightFile("src/HelloMain.kt", hwStats)
            perfHighlightFile("src/HelloMain2.kt", hwStats)
        }
    }

    fun testKotlinProject() {
        tcSuite("Kotlin project") {
            val stats = Stats("kotlin project")
            stats.use {
                perfOpenProject("perfTestProject", stats = it, path = "..")

                perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt", stats = it)

                perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtElement.kt", stats = it)
            }
        }
    }

    fun testKotlinProjectHighlightBuildGradle() {
        tcSuite("Kotlin project highlight build gradle") {
            val stats = Stats("kotlin project highlight build gradle")
            stats.use {
                perfOpenProject("perfTestProject", stats = it, path = "..")

                enableAnnotatorsAndLoadDefinitions()

                perfFileAnalysisBuildGradleKts(it)
                perfFileAnalysisIdeaBuildGradleKts(it)
                perfFileAnalysisJpsGradleKts(it)
                perfFileAnalysisVersionGradleKts(it)
            }
        }
    }

    private fun perfFileAnalysisBuildGradleKts(it: Stats) {
        perfFileAnalysis("build.gradle.kts", stats = it)
    }

    private fun perfFileAnalysisIdeaBuildGradleKts(it: Stats) {
        perfFileAnalysis("idea/build.gradle.kts", stats = it, note = "idea/")
    }

    private fun perfFileAnalysisJpsGradleKts(it: Stats) {
        perfFileAnalysis("gradle/jps.gradle.kts", stats = it, note = "gradle/")
    }

    private fun perfFileAnalysisVersionGradleKts(it: Stats) {
        perfFileAnalysis("gradle/versions.gradle.kts", stats = it, note = "gradle/")
    }

}