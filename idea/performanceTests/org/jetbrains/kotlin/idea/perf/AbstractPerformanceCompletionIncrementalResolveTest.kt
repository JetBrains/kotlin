/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.completion.CompletionBindingContextProvider
import org.jetbrains.kotlin.idea.perf.Stats.Companion.WARM_UP
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.testFramework.commitAllDocuments
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.InTextDirectivesUtils

/**
 * inspired by @see AbstractCompletionIncrementalResolveTest
 */
abstract class AbstractPerformanceCompletionIncrementalResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    private val BEFORE_MARKER = "<before>" // position to invoke completion before
    private val CHANGE_MARKER = "<change>" // position to insert text specified by "TYPE" directive
    private val TYPE_DIRECTIVE_PREFIX = "// TYPE:"
    private val BACKSPACES_DIRECTIVE_PREFIX = "// BACKSPACES:"

    companion object {
        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val stats: Stats = Stats("completion-incremental")

        init {
            // there is no @AfterClass for junit3.8
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { stats.close() }))
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

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
        innerPerfTest(WARM_UP) {
            myFixture.configureByText(
                KotlinFileType.INSTANCE,
                "class Foo {\n    private val value: String? = n<caret>\n}"
            )
        }
    }

    protected fun doPerfTest(unused: String) {
        val testPath = testPath()
        val testName = getTestName(false)
        innerPerfTest(testName) {
            myFixture.configureByFile(fileName())

            val document = myFixture.editor.document
            val beforeMarkerOffset = document.text.indexOf(BEFORE_MARKER)
            assertTrue("\"$BEFORE_MARKER\" is missing in file \"$testPath\"", beforeMarkerOffset >= 0)

            val changeMarkerOffset = document.text.indexOf(CHANGE_MARKER)
            assertTrue("\"$CHANGE_MARKER\" is missing in file \"$testPath\"", changeMarkerOffset >= 0)

            val textToType = InTextDirectivesUtils.findArrayWithPrefixes(document.text, TYPE_DIRECTIVE_PREFIX).singleOrNull()
                ?.let { StringUtil.unquoteString(it) }
            val backspaceCount = InTextDirectivesUtils.getPrefixedInt(document.text, BACKSPACES_DIRECTIVE_PREFIX)
            assertTrue(
                "At least one of \"$TYPE_DIRECTIVE_PREFIX\" and \"$BACKSPACES_DIRECTIVE_PREFIX\" should be defined",
                textToType != null || backspaceCount != null
            )

            val beforeMarker = document.createRangeMarker(beforeMarkerOffset, beforeMarkerOffset + BEFORE_MARKER.length)
            val changeMarker = document.createRangeMarker(changeMarkerOffset, changeMarkerOffset + CHANGE_MARKER.length)
            changeMarker.isGreedyToRight = true

            project.executeWriteCommand("") {
                document.deleteString(beforeMarker.startOffset, beforeMarker.endOffset)
                document.deleteString(changeMarker.startOffset, changeMarker.endOffset)
            }

            editor.caretModel.moveToOffset(beforeMarker.startOffset)
        }
    }

    private fun innerPerfTest(name: String, setUpBody: (TestData<Unit, Array<LookupElement>>) -> Unit) {
        CompletionBindingContextProvider.ENABLED = true
        try {
            performanceTest<Unit, Array<LookupElement>> {
                name(name)
                stats(stats)
                setUp(setUpBody)
                test { it.value = perfTestCore() }
                tearDown {
                    // no reasons to validate output as it is a performance test
                    assertNotNull(it.value)
                    runWriteAction {
                        myFixture.file.delete()
                    }
                }
            }
        } finally {
            CompletionBindingContextProvider.ENABLED = false
        }
    }

    private fun perfTestCore() = myFixture.complete(CompletionType.BASIC)
}