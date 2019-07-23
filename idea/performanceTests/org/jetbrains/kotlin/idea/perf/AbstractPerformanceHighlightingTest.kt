/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl.ensureIndexesUpToDate
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.junit.AfterClass

/**
 * inspired by @see AbstractHighlightingTest
 */
abstract class AbstractPerformanceHighlightingTest : KotlinLightCodeInsightFixtureTestCase() {

    companion object {
        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val stats: Stats = Stats("highlight")

        @AfterClass
        @JvmStatic
        fun teardown() {
            stats.close()
        }
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
        super.tearDown()
    }

    private fun doWarmUpPerfTest() {
        innerPerfTest("warm-up") {
            myFixture.configureByText(
                KotlinFileType.INSTANCE,
                "class Foo {\n    private val value: String? = null\n}"
            )
        }
    }

    protected fun doPerfTest(filePath: String) {
        val testName = getTestName(false)
        innerPerfTest(testName) {
            myFixture.configureByFile(filePath)

            val project = myFixture.project
            commitAllDocuments()

            val file = myFixture.file
            val offset = file.textOffset
            assertTrue("side effect: to load the text", offset >= 0)

            // to load AST for changed files before it's prohibited by "fileTreeAccessFilter"
            ensureIndexesUpToDate(project)
        }
    }

    private fun innerPerfTest(name: String, setUpBody: (TestData<Unit, MutableList<HighlightInfo>>) -> Unit) {
        stats.perfTest<Unit, MutableList<HighlightInfo>>(
            testName = name,
            setUp = { setUpBody(it) },
            test = { it.value = perfTestCore() },
            tearDown = {
                assertNotNull("no reasons to validate output as it is a performance test", it.value)
                runWriteAction {
                    myFixture.file.delete()
                }
            }
        )
    }

    private fun perfTestCore(): MutableList<HighlightInfo> = myFixture.doHighlighting()

}