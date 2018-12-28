/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.google.common.collect.Lists
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.tests.di.createContainerForTests
import java.io.File
import kotlin.test.assertEquals

private data class DiagnosticData(
    val index: Int,
    val rangeIndex: Int,
    val name: String,
    val startOffset: Int,
    val endOffset: Int
)

private abstract class Test(private vararg val expectedMessages: String) {
    fun test(psiFile: PsiFile, environment: KotlinCoreEnvironment) {
        val bindingContext = JvmResolveUtil.analyze(psiFile as KtFile, environment).bindingContext
        val emptyModule = KotlinTestUtils.createEmptyModule()
        val container = createContainerForTests(environment.project, emptyModule)
        val dataFlowValueFactory = container.dataFlowValueFactory
        val languageVersionSettings = container.expressionTypingServices.languageVersionSettings
        val expectedText = CheckerTestUtil.addDiagnosticMarkersToText(
            psiFile,
            CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
                bindingContext, psiFile,
                false,
                mutableListOf(),
                null,
                false,
                languageVersionSettings,
                dataFlowValueFactory,
                emptyModule
            )
        ).toString()
        val diagnosedRanges = Lists.newArrayList<DiagnosedRange>()

        CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges, mutableMapOf())

        val actualDiagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
            bindingContext,
            psiFile,
            false,
            mutableListOf(),
            null,
            false,
            languageVersionSettings,
            dataFlowValueFactory,
            emptyModule
        )

        makeTestData(actualDiagnostics, diagnosedRanges)

        val expectedMessages = listOf(*expectedMessages)
        val actualMessages = mutableListOf<String>()

        CheckerTestUtil.diagnosticsDiff(diagnosedRanges, actualDiagnostics, object : DiagnosticDiffCallbacks {
            override fun missingDiagnostic(diagnostic: TextDiagnostic, expectedStart: Int, expectedEnd: Int) {
                actualMessages.add(CheckerTestUtilTest.missing(diagnostic.description, expectedStart, expectedEnd))
            }

            override fun wrongParametersDiagnostic(
                expectedDiagnostic: TextDiagnostic,
                actualDiagnostic: TextDiagnostic,
                start: Int,
                end: Int
            ) {
                actualMessages.add(
                    CheckerTestUtilTest.wrongParameters(expectedDiagnostic.asString(), actualDiagnostic.asString(), start, end)
                )
            }

            override fun unexpectedDiagnostic(diagnostic: TextDiagnostic, actualStart: Int, actualEnd: Int) {
                actualMessages.add(CheckerTestUtilTest.unexpected(diagnostic.description, actualStart, actualEnd))
            }
        })

        assertEquals(expectedMessages.joinToString("\n"), actualMessages.joinToString("\n"))
    }

    abstract fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>)
}

class CheckerTestUtilTest : KotlinTestWithEnvironment() {
    private val diagnostics = listOf(
        DiagnosticData(0, 0, "UNUSED_PARAMETER", 8, 9),
        DiagnosticData(1, 1, "CONSTANT_EXPECTED_TYPE_MISMATCH", 56, 57),
        DiagnosticData(2, 2, "UNUSED_VARIABLE", 67, 68),
        DiagnosticData(3, 3, "TYPE_MISMATCH", 98, 99),
        DiagnosticData(4, 4, "NONE_APPLICABLE", 120, 121),
        DiagnosticData(5, 5, "TYPE_MISMATCH", 159, 167),
        DiagnosticData(6, 6, "UNRESOLVED_REFERENCE", 164, 166),
        DiagnosticData(7, 6, "TOO_MANY_ARGUMENTS", 164, 166)
    )

    private fun getTestDataPath() = KotlinTestUtils.getTestDataPathBase() + "/diagnostics/checkerTestUtil"

    override fun createEnvironment(): KotlinCoreEnvironment {
        println(System.getProperty("user.home"))
        System.setProperty("user.dir", "/Users/victor.petukhov/IdeaProjects/kotlin")
        println(System.getProperty("user.dir"))
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL)
    }

    private fun doTest(test: Test) = test.test(
        TestCheckerUtil.createCheckAndReturnPsiFile(
            "test.kt",
            KotlinTestUtils.doLoadFile(getTestDataPath(), "test.kt"),
            project
        ),
        environment
    )

    fun testEquals() {
        doTest(object : Test() {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {}
        })
    }

    fun testMissing() {
        val typeMismatch1 = diagnostics[1]

        doTest(object : Test(missing(typeMismatch1)) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnostics.removeAt(typeMismatch1.index)
            }
        })
    }

    fun testUnexpected() {
        val typeMismatch1 = diagnostics[1]

        doTest(object : Test(unexpected(typeMismatch1)) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnosedRanges.removeAt(typeMismatch1.index)
            }
        })
    }

    fun testBoth() {
        val typeMismatch1 = diagnostics[1]
        val unresolvedReference = diagnostics[6]

        doTest(object : Test(unexpected(typeMismatch1), missing(unresolvedReference)) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnosedRanges.removeAt(typeMismatch1.rangeIndex)
                diagnostics.removeAt(unresolvedReference.index)
            }
        })
    }

    fun testMissingInTheMiddle() {
        val noneApplicable = diagnostics[4]
        val typeMismatch3 = diagnostics[5]

        doTest(object : Test(unexpected(noneApplicable), missing(typeMismatch3)) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnosedRanges.removeAt(noneApplicable.rangeIndex)
                diagnostics.removeAt(typeMismatch3.index)
            }
        })
    }

    fun testWrongParameters() {
        val unused = diagnostics[2]
        val unusedDiagnostic = asTextDiagnostic(unused, "i")
        val range = asDiagnosticRange(unused, unusedDiagnostic)
        val wrongParameter = wrongParameters(unusedDiagnostic, "OI;UNUSED_VARIABLE(a)", unused.startOffset, unused.endOffset)

        doTest(object : Test(wrongParameter) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnosedRanges[unused.rangeIndex] = range
            }
        })
    }

    fun testWrongParameterInMultiRange() {
        val unresolvedReference = diagnostics[6]
        val unusedDiagnostic = asTextDiagnostic(unresolvedReference, "i")
        val toManyArguments = asTextDiagnostic(diagnostics[7])
        val range = asDiagnosticRange(unresolvedReference, unusedDiagnostic, toManyArguments)
        val wrongParameter = wrongParameters(
            unusedDiagnostic,
            "OI;UNRESOLVED_REFERENCE(xx)",
            unresolvedReference.startOffset,
            unresolvedReference.endOffset
        )

        doTest(object : Test(wrongParameter) {
            override fun makeTestData(diagnostics: MutableList<ActualDiagnostic>, diagnosedRanges: MutableList<DiagnosedRange>) {
                diagnosedRanges[unresolvedReference.rangeIndex] = range
            }
        })
    }

    fun testAbstractJetDiagnosticsTest() {
        val test = object : AbstractDiagnosticsTest() {
            init {
                setUp()
            }
        }

        test.doTest(getTestDataPath() + File.separatorChar + "test_with_diagnostic.kt")
    }

    companion object {
        fun wrongParameters(expected: String, actual: String, start: Int, end: Int) =
            "Wrong parameters $expected != $actual at $start to $end"

        fun unexpected(type: String, actualStart: Int, actualEnd: Int) =
            "Unexpected $type at $actualStart to $actualEnd"

        fun missing(type: String, expectedStart: Int, expectedEnd: Int) =
            "Missing $type at $expectedStart to $expectedEnd"

        private fun unexpected(data: DiagnosticData) = unexpected(data.name, data.startOffset, data.endOffset)

        private fun missing(data: DiagnosticData) = missing(data.name, data.startOffset, data.endOffset)

        private fun asTextDiagnostic(diagnosticData: DiagnosticData, vararg params: String) =
            diagnosticData.name + "(" + StringUtil.join(params, "; ") + ")"

        private fun asDiagnosticRange(diagnosticData: DiagnosticData, vararg textDiagnostics: String): DiagnosedRange {
            val range = DiagnosedRange(diagnosticData.startOffset)
            range.end = diagnosticData.endOffset
            for (textDiagnostic in textDiagnostics)
                range.addDiagnostic(textDiagnostic)
            return range
        }
    }
}
