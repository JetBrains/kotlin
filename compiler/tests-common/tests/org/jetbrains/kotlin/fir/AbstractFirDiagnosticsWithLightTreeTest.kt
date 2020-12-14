/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirLightDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.math.abs

abstract class AbstractFirDiagnosticsWithLightTreeTest : AbstractFirDiagnosticsTest() {
    override fun doTest(filePath: String) {
        val file = createTestFileFromPath(filePath)
        val expectedText = KotlinTestUtils.doLoadFile(file)
        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "// IGNORE_LIGHT_TREE")) return

        super.doTest(filePath)
    }

    override val useLightTree: Boolean
        get() = true

    override fun checkDiagnostics(file: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        val fileToDiagnostics = collectDiagnostics(firFiles)
        val missingDiagnostics = mutableListOf<MissingDiagnostic>()
        for (testFile in testFiles) {
            val firFile = firFiles.firstOrNull { it.name == testFile.name } ?: continue
            val ktFile = testFile.ktFile!!
            val basicDiagnostics = fileToDiagnostics[firFile] ?: emptyList()
            val debugInfoDiagnostics: List<FirDiagnostic<*>> =
                collectDebugInfoDiagnostics(firFile, testFile.diagnosedRangesToDiagnosticNames)

            val diagnostics = basicDiagnostics + debugInfoDiagnostics

            val actualDiagnostics = diagnostics.groupBy {
                require(it is FirLightDiagnostic)
                it.element.startOffset
            }.mapValues { (_, diagnostics) -> diagnostics.map { it.factory.name }.countEntries() }

            val existingDiagnostics = testFile.diagnosedRanges.groupBy {
                it.start
            }.mapValues { (_, ranges) ->
                ranges.flatMap { diagnosedRange ->
                    diagnosedRange.getDiagnostics().filterNot { it.name.contains("DEBUG_INFO_") }.map { it.name }
                }.countEntries()
            }

            for (startOffset in actualDiagnostics.keys + existingDiagnostics.keys) {
                val expected = existingDiagnostics[startOffset] ?: emptyMap()
                val actual = actualDiagnostics[startOffset] ?: emptyMap()
                for (name in expected.keys + actual.keys) {
                    if (name == null || name == "SYNTAX") continue
                    val expectedCount = expected[name] ?: 0
                    val actualCount = actual[name] ?: 0
                    if (expectedCount != actualCount) {
                        missingDiagnostics += MissingDiagnostic(startOffset, name, expectedCount - actualCount, ktFile)
                    }
                }
            }

            if (missingDiagnostics.isNotEmpty()) {
                val wasExpected = missingDiagnostics.filter { it.kind == MissingDiagnostic.Kind.WasExpected }
                val isActual = missingDiagnostics.filter { it.kind == MissingDiagnostic.Kind.IsActual }

                if (wasExpected.sumBy { it.count } == isActual.sumBy { it.count }) return

                val message = buildString {
                    fun MissingDiagnostic.errorMessage(): String {
                        val position = DiagnosticUtils.getLineAndColumnInPsiFile(ktFile, TextRange(startOffset, startOffset + 1))
                        // I don't know why but somehow line is greater for 3 than line in real file
                        return "  $count of $name at ${position.line - 3}:${position.column}"
                    }

                    if (wasExpected.isNotEmpty()) {
                        appendLine("Some diagnostics was expected:")
                        wasExpected.forEach {
                            appendLine(it.errorMessage())
                        }
                    }
                    if (isActual.isNotEmpty()) {
                        appendLine("Some new diagnostics:")
                        isActual.forEach {
                            appendLine(it.errorMessage())
                        }
                    }

                }
                kotlin.test.fail(message)
            }
        }

    }

    private fun <T> List<T>.countEntries(): Map<T, Int> {
        return groupBy { it }.mapValues { (_, value) -> value.size }
    }

    private class MissingDiagnostic(val startOffset: Int, val name: String, diff: Int, @Suppress("UNUSED_PARAMETER") ktFile: KtFile) {
        val kind: Kind = if (diff > 0) Kind.WasExpected else Kind.IsActual
        val count: Int = abs(diff)

        enum class Kind {
            WasExpected,
            IsActual
        }
    }
}
