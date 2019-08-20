/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.testFramework.propertyBased.MadTestingUtil
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiChecker
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiCheckerAndHighlightingUpdater
import org.jetbrains.kotlin.idea.testFramework.Fixture
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertNotEquals

class PerformanceProjectsTest : AbstractPerformanceProjectsTest() {

    companion object {

        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val hwStats: Stats = Stats("helloWorld project")

        @JvmStatic
        val timer: AtomicLong = AtomicLong()

        init {
            // there is no @AfterClass for junit3.8
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { hwStats.close() }))
        }

        fun resetTimestamp() {
            timer.set(0)
        }

        fun markTimestamp() {
            timer.set(System.nanoTime())
        }
    }

    override fun setUp() {
        super.setUp()
        // warm up: open simple small project
        if (!warmedUp) {
            warmUpProject(hwStats)

            warmedUp = true
        }
    }

    fun testHelloWorldProject() {
        tcSuite("Hello world project") {
            myProject = perfOpenHelloWorld(hwStats)

            // highlight
            perfHighlightFile("src/HelloMain.kt", hwStats)
            perfHighlightFile("src/HelloMain2.kt", hwStats)
        }
    }

    fun testKotlinProject() {
        tcSuite("Kotlin project") {
            val stats = Stats("kotlin project")
            stats.use {
                perfOpenKotlinProject(it)

                perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt", stats = it)

                perfHighlightFile("idea/idea-analysis/src/org/jetbrains/kotlin/idea/util/PsiPrecedences.kt", stats = it)

                perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtElement.kt", stats = it)
            }
        }
    }

    fun testKotlinProjectCompletionKtFile() {
        tcSuite("Kotlin completion ktFile") {
            val stats = Stats("Kotlin completion ktFile")
            stats.use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfTypeAndAutocomplete(
                    stat,
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    "override fun getDeclarations(): List<KtDeclaration> {",
                    "val q = import",
                    lookupElements = listOf("importDirectives"),
                    note = "in-method getDeclarations-import"
                )

                perfTypeAndAutocomplete(
                    stat,
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    "override fun getDeclarations(): List<KtDeclaration> {",
                    "val q = import",
                    typeAfterMarker = false,
                    lookupElements = listOf("importDirectives"),
                    note = "out-of-method import"
                )
            }
        }
    }

    fun testKotlinProjectCompletionBuildGradle() {
        tcSuite("Kotlin completion gradle.kts") {
            val stats = Stats("kotlin completion gradle.kts")
            stats.use { stat ->
                runAndMeasure("open kotlin project") {
                    perfOpenKotlinProjectFast(stat)
                }

                runAndMeasure("type and autocomplete") {
                    perfTypeAndAutocomplete(
                        stat,
                        "build.gradle.kts",
                        "tasks {",
                        "crea",
                        lookupElements = listOf("create"),
                        note = "tasks-create"
                    )
                }
            }
        }
    }

    fun testKotlinProjectScriptDependenciesBuildGradle() {
        tcSuite("Kotlin scriptDependencies gradle.kts") {
            val stats = Stats("kotlin scriptDependencies gradle.kts")
            stats.use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfScriptDependenciesBuildGradleKts(stat)
                perfScriptDependenciesIdeaBuildGradleKts(stat)
                perfScriptDependenciesJpsGradleKts(stat)
                perfScriptDependenciesVersionGradleKts(stat)
            }
        }
    }

    fun testKotlinProjectBuildGradle() {
        tcSuite("Kotlin gradle.kts") {
            val stats = Stats("kotlin gradle.kts")
            stats.use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfFileAnalysisBuildGradleKts(stat)
                perfFileAnalysisIdeaBuildGradleKts(stat)
                perfFileAnalysisJpsGradleKts(stat)
                perfFileAnalysisVersionGradleKts(stat)
            }
        }
    }

    private fun perfScriptDependenciesBuildGradleKts(it: Stats) {
        perfScriptDependencies("build.gradle.kts", stats = it)
    }

    private fun perfScriptDependenciesIdeaBuildGradleKts(it: Stats) {
        perfScriptDependencies("idea/build.gradle.kts", stats = it, note = "idea/")
    }

    private fun perfScriptDependenciesJpsGradleKts(it: Stats) {
        perfScriptDependencies("gradle/jps.gradle.kts", stats = it, note = "gradle/")
    }

    private fun perfScriptDependenciesVersionGradleKts(it: Stats) {
        perfScriptDependencies("gradle/versions.gradle.kts", stats = it, note = "gradle/")
    }

    private fun perfFileAnalysisBuildGradleKts(it: Stats) {
        perfKtsFileAnalysis("build.gradle.kts", stats = it)
    }

    private fun perfFileAnalysisIdeaBuildGradleKts(it: Stats) {
        perfKtsFileAnalysis("idea/build.gradle.kts", stats = it, note = "idea/")
    }

    private fun perfFileAnalysisJpsGradleKts(it: Stats) {
        perfKtsFileAnalysis("gradle/jps.gradle.kts", stats = it, note = "gradle/")
    }

    private fun perfFileAnalysisVersionGradleKts(it: Stats) {
        perfKtsFileAnalysis("gradle/versions.gradle.kts", stats = it, note = "gradle/")
    }

    private fun perfKtsFileAnalysis(
        fileName: String,
        stats: Stats,
        note: String = ""
    ) {
        val project = myProject!!
        val disposable = Disposer.newDisposable("perfKtsFileAnalysis $fileName")

        MadTestingUtil.enableAllInspections(project, disposable)

        replaceWithCustomHighlighter()

        try {
            highlightFile {
                val testName = "fileAnalysis ${notePrefix(note)}${simpleFilename(fileName)}"
                val extraStats = Stats("${stats.name} $testName")
                val extraTimingsNs = mutableListOf<Long>()

                val warmUpIterations = 3
                val iterations = 10

                stats.perfTest(
                    warmUpIterations = warmUpIterations,
                    iterations = iterations,
                    testName = testName,
                    setUp = perfKtsFileAnalysisSetUp(project, fileName),
                    test = perfKtsFileAnalysisTest(),
                    tearDown = perfKtsFileAnalysisTearDown(extraTimingsNs, project)
                )

                extraStats.printWarmUpTimings(
                    "annotator",
                    Array(warmUpIterations, init = { null }),
                    extraTimingsNs.take(warmUpIterations).toLongArray()
                )

                extraStats.appendTimings(
                    "annotator",
                    Array(iterations, init = { null }),
                    extraTimingsNs.drop(warmUpIterations).toLongArray()
                )
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun replaceWithCustomHighlighter() {
        org.jetbrains.kotlin.idea.testFramework.replaceWithCustomHighlighter(
            testRootDisposable,
            KotlinPsiCheckerAndHighlightingUpdater::class.java.name,
            TestKotlinPsiChecker::class.java.name
        )
    }

    fun perfKtsFileAnalysisSetUp(
        project: Project,
        fileName: String
    ): (TestData<Fixture, Pair<Long, List<HighlightInfo>>>) -> Unit {
        return {
            val fileInEditor = openFileInEditor(project, fileName)

            val file = fileInEditor.psiFile
            val virtualFile = file.virtualFile
            val editor = EditorFactory.getInstance().getEditors(fileInEditor.document, project)[0]
            val fixture = Fixture(project, editor, virtualFile)

            // Note: Kotlin scripts require dependencies to be loaded
            if (isAKotlinScriptFile(fileName)) {
                ScriptDependenciesManager.updateScriptDependenciesSynchronously(fileInEditor.psiFile.virtualFile, project)
            }

            resetTimestamp()
            it.setUpValue = fixture
        }
    }

    fun perfKtsFileAnalysisTest(): (TestData<Fixture, Pair<Long, List<HighlightInfo>>>) -> Unit {
        return {
            it.value = it.setUpValue?.let { fixture ->
                Pair(System.nanoTime(), fixture.doHighlighting())
            }
        }
    }

    fun perfKtsFileAnalysisTearDown(
        extraTimingsNs: MutableList<Long>,
        project: Project
    ): (TestData<Fixture, Pair<Long, List<HighlightInfo>>>) -> Unit {
        return {
            it.setUpValue?.let { fixture ->
                it.value?.let { v ->
                    assertTrue(v.second.isNotEmpty())
                    assertNotEquals(0, timer.get())

                    extraTimingsNs.add(timer.get() - v.first)

                }
                cleanupCaches(project, fixture.vFile)
            }
        }
    }


    class TestKotlinPsiChecker : KotlinPsiChecker() {
        override fun annotate(
            element: PsiElement, holder: AnnotationHolder
        ) {
            super.annotate(element, holder)
            markTimestamp()
        }
    }
}