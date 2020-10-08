/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.perf.Stats
import org.jetbrains.kotlin.idea.perf.TestData
import org.jetbrains.kotlin.idea.perf.performanceTest
import org.jetbrains.kotlin.idea.testFramework.commitAllDocuments
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractFirHighlightingPerformanceTest : AbstractHighlightingTest() {
    companion object {
        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val stats: Stats = Stats("firHighlight")

    }

    override fun isFirPlugin() = true

    override fun setUp() {
        super.setUp()

        if (!warmedUp) {
            doWarmUpPerfTest()
            warmedUp = true
        }
    }

    override fun tearDown() {
        commitAllDocuments()
        RunAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable { stats.flush() }
        ).run()
    }

    private fun doWarmUpPerfTest() {
        innerPerfTest(Stats.WARM_UP) {
            myFixture.configureByText(
                KotlinFileType.INSTANCE,
                "class Foo {\n    private val value: String? = null\n}"
            )
        }
    }

    override fun doTest(filePath: String) {
        val testName = getTestName(false)
        val ignore = InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(File(filePath)), "IGNORE_FIR")
        if (ignore) return
        innerPerfTest(testName) {
            myFixture.configureByFile(fileName())
            commitAllDocuments()
            removeInfoMarkers()

            val file = myFixture.file
            val offset = file.textOffset
            assertTrue("side effect: to load the text", offset >= 0)

            // to load AST for changed files before it's prohibited by "fileTreeAccessFilter"
            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
        }
    }

    private fun removeInfoMarkers() {
        ExpectedHighlightingData(editor.document, true, true).init()

        EdtTestUtil.runInEdtAndWait {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }
    }

    private fun innerPerfTest(name: String, setUpBody: (TestData<Unit, MutableList<HighlightInfo>>) -> Unit) {
        performanceTest<Unit, MutableList<HighlightInfo>> {
            name(name)
            stats(stats)
            setUp {
                setUpBody(it)
            }
            test {
                it.value = perfTestCore()
            }
            tearDown {
                assertNotNull("no reasons to validate output as it is a performance test", it.value)
                runWriteAction {
                    myFixture.file.delete()
                }
            }
        }
    }

    private fun perfTestCore(): MutableList<HighlightInfo> = myFixture.doHighlighting()

}