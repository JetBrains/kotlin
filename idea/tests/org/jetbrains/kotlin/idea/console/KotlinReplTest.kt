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

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.testFramework.PlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.properties.Delegates
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.JVM)
public class KotlinReplTest : PlatformTestCase() {
    private var consoleRunner: KotlinConsoleRunner by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        consoleRunner = KotlinConsoleKeeper.getInstance(project).run(module)!!
    }

    override fun tearDown() {
        consoleRunner.dispose()
        super.tearDown()
    }

    private fun sendCommand(command: String) {
        consoleRunner.consoleView.editorDocument.setText(command)
        consoleRunner.executor.executeCommand()
    }

    private fun waitForExpectedOutput(expectedOutput: String) {
        val endsWithPredicate = { x: String -> x.trim().endsWith(expectedOutput) }

        val historyText = checkHistoryUpdate { endsWithPredicate(it) }

        assertTrue(endsWithPredicate(historyText), "'$expectedOutput' should be printed but document text is:\n$historyText")
    }

    private fun checkHistoryUpdate(maxIterations: Int = 50, sleepTime: Long = 500, stopPredicate: (String) -> Boolean): String {
        val consoleView = consoleRunner.consoleView as LanguageConsoleImpl

        for (i in 1..maxIterations) {
            UIUtil.dispatchAllInvocationEvents()
            consoleView.flushDeferredText()

            val historyText = consoleView.historyViewer.document.text
            if (stopPredicate(historyText)) return historyText

            Thread.sleep(sleepTime)
        }

        return "<empty text>"
    }

    private fun testSimpleCommand(command: String, expectedOutput: String) {
        sendCommand(command)
        waitForExpectedOutput(expectedOutput)
    }

    @Test fun testRunPossibility() {
        val allOk = { x: String -> x.contains(":help for help") }
        val hasErrors = { x: String -> x.contains("Process finished with exit code 1") || x.contains("Exception in") || x.contains("Error") }

        val historyText = checkHistoryUpdate { hasErrors(it) || allOk(it) }

        assertFalse(hasErrors(historyText), "Cannot run kotlin repl")
        assertTrue(allOk(historyText), "Successful run should contain text: ':help for help'")
        assertFalse(consoleRunner.processHandler.isProcessTerminated, "Process accidentally terminated")
    }

    @Test fun testOnePlusOne() = testSimpleCommand("1 + 1", "2")
    @Test fun testPrintlnText() = "Hello, console world!".let { testSimpleCommand("println(\"$it\")", it) }
    @Test fun testDivisionByZeroException() = testSimpleCommand("1 / 0", "java.lang.ArithmeticException: / by zero")

    @Test fun testMultilineSupport() {
        val printText = "Print in multiline!"

        sendCommand("fun f() {\n" +
                    "    println(\"$printText\")\n" +
                    "}\n")
        sendCommand("f()")

        waitForExpectedOutput(printText)
    }

    @Test fun testReadLineSingle() {
        val readLineText = "ReadMe!"

        sendCommand("val a = readLine()")
        sendCommand(readLineText)
        sendCommand("a")
        waitForExpectedOutput(readLineText)
    }

    @Test fun testReadLineMultiple() {
        val readLineTextA = "ReadMe A!"
        val readLineTextB = "ReadMe B!"

        sendCommand("val a = readLine()\n" +
                    "val b = readLine()")
        sendCommand(readLineTextA)
        sendCommand(readLineTextB)

        sendCommand("a")
        waitForExpectedOutput(readLineTextA)
        sendCommand("b")
        waitForExpectedOutput(readLineTextB)
    }

    @Test fun testCorrectAfterError() {
        val message = "MyMessage"
        sendCommand("fun f() { println(x)\n println(y) ")
        sendCommand("println(\"$message\")")
        waitForExpectedOutput(message)
    }
}