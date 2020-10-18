/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl.ensureIndexesUpToDate
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.perf.Stats.Companion.WARM_UP
import org.jetbrains.kotlin.idea.perf.util.removeInfoMarkers
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.testFramework.commitAllDocuments

/**
 * inspired by @see AbstractHighlightingTest
 */
abstract class AbstractPerformanceHighlightingTest : KotlinLightCodeInsightFixtureTestCase() {

    companion object {
        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val stats: Stats = Stats("highlight")

    }

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
        innerPerfTest(WARM_UP) {
            myFixture.configureByText(
                KotlinFileType.INSTANCE,
                "class Foo {\n    private val value: String? = null\n}"
            )
        }
    }

    private fun testName(): String {
        val javaClass = this.javaClass
        val testName = getTestName(false)
        return if (javaClass.isMemberClass) {
            "${javaClass.simpleName} - $testName"
        } else {
            testName
        }
    }

    protected fun doPerfTest(unused: String) {
        val testName = testName()
        innerPerfTest(testName) {
            myFixture.configureByFile(fileName())

            val project = myFixture.project
            commitAllDocuments()
            removeInfoMarkers()

            val file = myFixture.file
            val offset = file.textOffset
            assertTrue("side effect: to load the text", offset >= 0)

            // to load AST for changed files before it's prohibited by "fileTreeAccessFilter"
            ensureIndexesUpToDate(project)
        }
    }

    private fun innerPerfTest(name: String, setUpBody: (TestData<Unit, MutableList<HighlightInfo>>) -> Unit) {
        performanceTest<Unit, MutableList<HighlightInfo>> {
            name(name)
            stats(stats)
            setUp {
                setUpBody(it)
            }
            test { it.value = perfTestCore() }
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