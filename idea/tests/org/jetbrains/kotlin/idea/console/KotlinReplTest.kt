/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.console

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestCase
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class KotlinReplTest : PlatformTestCase() {
    private var consoleRunner: KotlinConsoleRunner by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        consoleRunner = KotlinConsoleKeeper.getInstance(project).run(module)!!
    }

    override fun tearDown() {
        val consoleView = consoleRunner.consoleView

        consoleRunner.processHandler.destroyProcess()

        // dispose RunContentDescriptor by reflection
        val getNodeMethod = Disposer.getTree().javaClass.getDeclaredMethod("getNode", Object().javaClass)
        getNodeMethod.isAccessible = true
        val consoleNode = getNodeMethod(Disposer.getTree(), consoleView)
        val getParentMethod = consoleNode.javaClass.getDeclaredMethod("getParent")
        getParentMethod.isAccessible = true
        val consoleParent = getParentMethod(consoleNode)
        val getObjectMethod = consoleParent.javaClass.getDeclaredMethod("getObject")
        getObjectMethod.isAccessible = true
        val descriptor = getObjectMethod(consoleParent) as Disposable

        Disposer.dispose(descriptor)

        super.tearDown()
    }

    fun checkHistoryUpdate(maxIter: Int = 20, sleepTime: Long = 500, p: (String) -> Boolean): String {
        val consoleView = consoleRunner.consoleView as ConsoleViewImpl
        var docHistory: String = ""

        for (i in 1..maxIter) {
            docHistory = consoleRunner.consoleView.historyViewer.document.text.trim()

            if (p(docHistory)) break

            Thread.sleep(sleepTime)
            consoleView.flushDeferredText()
        }

        return docHistory
    }

    @Test fun testOnRunPossibility() {
        val allOk = { x: String -> x.contains(":help for help") }
        val hasErrors = { x: String -> x.contains("Process finished with exit code 1") || x.contains("Exception in") || x.contains("Error") }
        val historyText = checkHistoryUpdate { hasErrors(it) || allOk(it) }

        assertFalse(hasErrors(historyText), "Cannot run kotlin repl")
        assertTrue(allOk(historyText), "Successful run should contain text: ':help for help'")
        assertFalse(consoleRunner.processHandler.isProcessTerminated, "Process accidentally terminated")
    }

    @Test fun testSimpleCommand() {
        consoleRunner.pipeline.submitCommand("1 + 1")
        val docHistory = checkHistoryUpdate { x: String -> x.endsWith("2</output>") }

        assertTrue(docHistory.endsWith("2</output>"), "1 + 1 should be equal 2, but document history is: '$docHistory'")
    }
}