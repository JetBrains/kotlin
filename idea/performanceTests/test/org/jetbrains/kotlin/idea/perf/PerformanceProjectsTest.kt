/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.testFramework.RunAll
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiChecker
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiCheckerAndHighlightingUpdater
import org.jetbrains.kotlin.idea.perf.Stats.Companion.TEST_KEY
import org.jetbrains.kotlin.idea.perf.Stats.Companion.runAndMeasure
import org.jetbrains.kotlin.idea.perf.util.Metric
import org.jetbrains.kotlin.idea.perf.util.TeamCity.suite
import org.jetbrains.kotlin.idea.testFramework.Fixture
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.cleanupCaches
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.isAKotlinScriptFile
import org.jetbrains.kotlin.idea.testFramework.ProjectOpenAction.GRADLE_PROJECT
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertNotEquals

class PerformanceProjectsTest : AbstractPerformanceProjectsTest() {

    companion object {

        @JvmStatic
        val hwStats: Stats = Stats("helloWorld project")

        @JvmStatic
        val warmUp = WarmUpProject(hwStats)

        @JvmStatic
        val timer: AtomicLong = AtomicLong()

        fun resetTimestamp() {
            timer.set(0)
        }

        fun markTimestamp() {
            timer.set(System.nanoTime())
        }
    }

    override fun setUp() {
        super.setUp()
        warmUp.warmUp(this)
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable { hwStats.flush() }
        ).run()
    }

    fun testHelloWorldProject() {
        suite("Hello world project") {
            myProject = perfOpenProject(stats = hwStats) {
                name("helloKotlin")

                kotlinFile("HelloMain") {
                    topFunction("main") {
                        param("args", "Array<String>")
                        body("""println("Hello World!")""")
                    }
                }

                kotlinFile("HelloMain2") {
                    topFunction("main") {
                        param("args", "Array<String>")
                        body("""println("Hello World!")""")
                    }
                }
            }

            // highlight
            perfHighlightFile("src/HelloMain.kt", hwStats)
            perfHighlightFile("src/HelloMain2.kt", hwStats)
        }
    }

    fun testKotlinProject() {
        suite("Kotlin project") {
            Stats("kotlin project").use {
                perfOpenKotlinProject(it)

                val filesToHighlight = arrayOf(
                    "idea/idea-analysis/src/org/jetbrains/kotlin/idea/util/PsiPrecedences.kt",
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtElement.kt",
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    "core/builtins/native/kotlin/Primitives.kt",

                    "compiler/frontend/src/org/jetbrains/kotlin/cfg/ControlFlowProcessor.kt",
                    "compiler/frontend/src/org/jetbrains/kotlin/cfg/ControlFlowInformationProvider.kt",

                    "compiler/backend/src/org/jetbrains/kotlin/codegen/state/KotlinTypeMapper.kt",
                    "compiler/backend/src/org/jetbrains/kotlin/codegen/inline/MethodInliner.kt"
                )

                filesToHighlight.forEach { file -> perfHighlightFile(file, stats = it) }
                filesToHighlight.forEach { file -> perfHighlightFileEmptyProfile(file, stats = it) }

                perfTypeAndHighlight(
                    it,
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    "override fun getDeclarations(): List<KtDeclaration> {",
                    "val q = import",
                    note = "in-method getDeclarations-import"
                )

                perfTypeAndHighlight(
                    it,
                    "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    "override fun getDeclarations(): List<KtDeclaration> {",
                    "val q = import",
                    typeAfterMarker = false,
                    note = "out-of-method import"
                )
            }
        }
    }

    fun testKotlinProjectCopyAndPaste() {
        suite("Kotlin copy-and-paste") {
            Stats("Kotlin copy-and-paste").use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfCopyAndPaste(
                    stat,
                    sourceFileName = "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
                    targetFileName = "compiler/psi/src/org/jetbrains/kotlin/psi/KtImportInfo.kt"
                )
            }
        }
    }

    fun testKotlinProjectCompletionKtFile() {
        suite("Kotlin completion ktFile") {
            Stats("Kotlin completion ktFile").use { stat ->
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

                perfTypeAndAutocomplete(
                    stat,
                    fileName = "compiler/backend/src/org/jetbrains/kotlin/codegen/state/KotlinTypeMapper.kt",
                    marker = "fun mapOwner(descriptor: DeclarationDescriptor): Type {",
                    insertString = "val b = bind",
                    typeAfterMarker = true,
                    lookupElements = listOf("bindingContext"),
                    note = "in-method completion for KotlinTypeMapper"
                )

                perfTypeAndAutocomplete(
                    stat,
                    fileName = "compiler/backend/src/org/jetbrains/kotlin/codegen/state/KotlinTypeMapper.kt",
                    marker = "fun mapOwner(descriptor: DeclarationDescriptor): Type {",
                    insertString = "val b = bind",
                    typeAfterMarker = false,
                    lookupElements = listOf("bindingContext"),
                    note = "out-of-method completion for KotlinTypeMapper"
                )

                perfTypeAndAutocomplete(
                    stat,
                    fileName = "compiler/tests/org/jetbrains/kotlin/util/ArgsToParamsMatchingTest.kt",
                    marker = "fun testMatchNamed() {",
                    insertString = "testMatch",
                    typeAfterMarker = true,
                    lookupElements = listOf("testMatchNamed"),
                    note = "in-method completion for ArgsToParamsMatchingTest"
                )

                perfTypeAndAutocomplete(
                    stat,
                    fileName = "compiler/tests/org/jetbrains/kotlin/util/ArgsToParamsMatchingTest.kt",
                    marker = "class ArgsToParamsMatchingTest {",
                    insertString = "val me = ",
                    typeAfterMarker = true,
                    lookupElements = listOf("ArgsToParamsMatchingTest"),
                    note = "out-of-method completion for ArgsToParamsMatchingTest"
                )
            }
        }
    }

    fun testKotlinProjectCompletionBuildGradle() {
        suite("Kotlin completion gradle.kts") {
            Stats("kotlin completion gradle.kts").use { stat ->
                runAndMeasure("open kotlin project") {
                    perfOpenKotlinProjectFast(stat)
                }

                runAndMeasure("type and autocomplete") {
                    perfTypeAndAutocomplete(
                        stat,
                        "build.gradle.kts",
                        "tasks {",
                        "reg",
                        lookupElements = listOf("register"),
                        note = "tasks-create"
                    )
                }

                runAndMeasure("type and undo") {
                    perfTypeAndUndo(
                        project(),
                        stat,
                        "build.gradle.kts",
                        "tasks {",
                        "register",
                        note = "type-undo"
                    )
                }

            }
        }
    }

    fun testKotlinProjectScriptDependenciesBuildGradle() {
        suite("Kotlin scriptDependencies gradle.kts") {
            Stats("kotlin scriptDependencies gradle.kts").use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfScriptDependenciesBuildGradleKts(stat)
                perfScriptDependenciesIdeaBuildGradleKts(stat)
//                perfScriptDependenciesJpsGradleKts(stat)
//                perfScriptDependenciesVersionGradleKts(stat)
            }
        }
    }

    fun testKotlinProjectBuildGradle() {
        suite("Kotlin gradle.kts") {
            Stats("kotlin gradle.kts").use { stat ->
                perfOpenKotlinProjectFast(stat)

                perfFileAnalysisBuildGradleKts(stat)
                perfFileAnalysisIdeaBuildGradleKts(stat)
//                perfFileAnalysisJpsGradleKts(stat)
//                perfFileAnalysisVersionGradleKts(stat)
            }
        }
    }

    private fun perfOpenKotlinProjectFast(stats: Stats) = perfOpenKotlinProject(stats, fast = true)

    private fun perfOpenKotlinProject(stats: Stats, fast: Boolean = false) {
        myProject = perfOpenProject(
            name = "kotlin",
            stats = stats,
            note = "",
            path = "../perfTestProject",
            openAction = GRADLE_PROJECT,
            fast = fast
        )
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
        //val disposable = Disposer.newDisposable("perfKtsFileAnalysis $fileName")

        //enableAllInspectionsCompat(project, disposable)

        replaceWithCustomHighlighter()

        highlightFile {
            val testName = "fileAnalysis ${notePrefix(note)}${simpleFilename(fileName)}"
            val extraStats = Stats("${stats.name} $testName")
            val extraTimingsNs = mutableListOf<Map<String, Any>?>()

            val warmUpIterations = 30
            val iterations = 50

            performanceTest<Fixture, Pair<Long, List<HighlightInfo>>> {
                name(testName)
                stats(stats)
                warmUpIterations(warmUpIterations)
                iterations(iterations)
                setUp(perfKtsFileAnalysisSetUp(project, fileName))
                test(perfKtsFileAnalysisTest())
                tearDown(perfKtsFileAnalysisTearDown(extraTimingsNs, project))
                profilerConfig.enabled = true
            }

            val metricChildren = mutableListOf<Metric>()

            extraStats.printWarmUpTimings(
                "annotator",
                extraTimingsNs.take(warmUpIterations).toTypedArray(),
                metricChildren
            )

            extraStats.processTimings(
                "annotator",
                extraTimingsNs.drop(warmUpIterations).toTypedArray(),
                metricChildren
            )
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
            val fixture = Fixture.openFixture(project, fileName)

            // Note: Kotlin scripts require dependencies to be loaded
            if (isAKotlinScriptFile(fileName)) {
                ScriptConfigurationManager.updateScriptDependenciesSynchronously(fixture.psiFile)
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
        extraTimingsNs: MutableList<Map<String, Any>?>,
        project: Project
    ): (TestData<Fixture, Pair<Long, List<HighlightInfo>>>) -> Unit {
        return {
            it.setUpValue?.let { fixture ->
                it.value?.let { v ->
                    assertTrue(v.second.isNotEmpty())
                    assertNotEquals(0, timer.get())

                    extraTimingsNs.add(mapOf(TEST_KEY to (timer.get() - v.first)))

                }
                fixture.use {
                    cleanupCaches(project)
                }
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