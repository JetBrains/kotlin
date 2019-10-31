/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.checkers.DiagnosedRange
import org.jetbrains.kotlin.checkers.DiagnosticDiffCallbacks
import org.jetbrains.kotlin.checkers.TestCheckerUtil
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.PositionalTextDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.ParallelDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.registerAllComponents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestFiles
import java.io.File

abstract class AbstractFirResolveWithDiagnosticsTestCase : AbstractFirResolveTestCase() {
    override fun doTest(path: String) {
        val file = File(path)
        val expectedText = KotlinTestUtils.doLoadFile(file)
        val testFiles = createTestFiles(file, expectedText)
        val firFiles = doCreateAndProcessFir(testFiles.mapNotNull { it.ktFile })
        checkFir(path, firFiles)
        checkDiagnostics(file, testFiles, firFiles)
    }

    private fun createCollector(): AbstractDiagnosticCollector {
//        val collector = SimpleDiagnosticsCollector()
        val collector = ParallelDiagnosticsCollector(4)
        collector.registerAllComponents()
        return collector
    }

    private fun checkDiagnostics(file: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        val collector = createCollector()
        val actualText = StringBuilder()
        for ((testFile, firFile) in testFiles zip firFiles) {
            val coneDiagnostics = collector.collectDiagnostics(firFile)
            testFile.getActualText(coneDiagnostics, actualText)
        }
        KotlinTestUtils.assertEqualsToFile(file, actualText.toString())
    }

    private fun createTestFiles(file: File, expectedText: String?): List<TestFile> {
        return TestFiles.createTestFiles(file.name, expectedText, object : TestFiles.TestFileFactory<Nothing?, TestFile> {
            override fun createFile(module: Nothing?, fileName: String, text: String, directives: MutableMap<String, String>): TestFile {
                return TestFile(fileName, text, directives)
            }

            override fun createModule(name: String, dependencies: MutableList<String>, friends: MutableList<String>): Nothing? {
                return null
            }
        })
    }


    private inner class TestFile(
        fileName: String,
        textWithMarkers: String,
        val directives: Map<String, String>
    ) {
        private val diagnosedRanges: MutableList<DiagnosedRange> = mutableListOf()
        private val diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>> = mutableMapOf()
        val ktFile: KtFile? by lazy {
            if (fileName.endsWith(".java")) {
                null
            } else {
                TestCheckerUtil.createCheckAndReturnPsiFile(fileName, clearText, project)
            }
        }
        val actualDiagnostics: MutableList<ActualDiagnostic> = mutableListOf()
        val clearText: String
        val expectedText: String

        init {
            if (fileName.endsWith(".java")) {
                clearText = textWithMarkers
                expectedText = clearText
            } else {
                expectedText = textWithMarkers
                clearText = CheckerTestUtil.parseDiagnosedRanges(addExtracts(expectedText), diagnosedRanges, diagnosedRangesToDiagnosticNames)
            }
        }

        fun addExtracts(text: String): String {
            // TODO
            return text
        }

        fun getActualText(
            coneDiagnostics: Iterable<ConeDiagnostic>,
            actualText: StringBuilder
        ): Boolean {
            val ktFile = this.ktFile
            if (ktFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText)
                return true
            }

            if (ktFile.name.endsWith("CoroutineUtil.kt") && ktFile.packageFqName == FqName("helpers")) return true

            // TODO: report JVM signature diagnostics also for implementing modules

            val ok = booleanArrayOf(true)
            val diagnostics = coneDiagnostics.toActualDiagnostic()
            val filteredDiagnostics = diagnostics // TODO

            actualDiagnostics.addAll(filteredDiagnostics)

            val uncheckedDiagnostics = mutableListOf<PositionalTextDiagnostic>()

            val diagnosticToExpectedDiagnostic =
                CheckerTestUtil.diagnosticsDiff(diagnosedRanges, filteredDiagnostics, object : DiagnosticDiffCallbacks {
                    override fun missingDiagnostic(diagnostic: TextDiagnostic, expectedStart: Int, expectedEnd: Int) {
                        val message = "Missing " + diagnostic.description + PsiDiagnosticUtils.atLocation(
                            ktFile,
                            TextRange(expectedStart, expectedEnd)
                        )
                        System.err.println(message)
                        ok[0] = false
                    }

                    override fun wrongParametersDiagnostic(
                        expectedDiagnostic: TextDiagnostic,
                        actualDiagnostic: TextDiagnostic,
                        start: Int,
                        end: Int
                    ) {
                        val message = "Parameters of diagnostic not equal at position " +
                                PsiDiagnosticUtils.atLocation(ktFile, TextRange(start, end)) +
                                ". Expected: ${expectedDiagnostic.asString()}, actual: $actualDiagnostic"
                        System.err.println(message)
                        ok[0] = false
                    }

                    override fun unexpectedDiagnostic(diagnostic: TextDiagnostic, actualStart: Int, actualEnd: Int) {
                        val message = "Unexpected ${diagnostic.description}${PsiDiagnosticUtils.atLocation(
                            ktFile,
                            TextRange(actualStart, actualEnd)
                        )}"
                        System.err.println(message)
                        ok[0] = false
                    }

                    fun updateUncheckedDiagnostics(diagnostic: TextDiagnostic, start: Int, end: Int) {
                        uncheckedDiagnostics.add(PositionalTextDiagnostic(diagnostic, start, end))
                    }
                })

            actualText.append(
                CheckerTestUtil.addDiagnosticMarkersToText(
                    ktFile,
                    filteredDiagnostics,
                    diagnosticToExpectedDiagnostic,
                    { file -> file.text },
                    uncheckedDiagnostics,
                    false,
                    false
                )
            )

            stripExtras(actualText)

            return ok[0]
        }

        private fun stripExtras(text: StringBuilder): StringBuilder {
            // TODO
            return text
        }
    }

    private fun Iterable<ConeDiagnostic>.toActualDiagnostic(): Collection<ActualDiagnostic> {
        return map { ActualDiagnostic(it.diagnostic, null, true) }
    }
}