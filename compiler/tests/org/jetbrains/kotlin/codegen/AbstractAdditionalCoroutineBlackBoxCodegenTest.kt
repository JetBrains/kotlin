/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.junit.Assert
import java.io.File

abstract class AbstractAdditionalCoroutineBlackBoxCodegenTest : AbstractBlackBoxCodegenTest() {
    companion object {
        private val callLambda = """
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        x()
        |    }
        """.trimMargin()

        private val sendToLambda = """
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        myInvokeLater { x() }
        |    }
        |
        |    fun myInvokeLater(block: () -> Unit) {
        |        block()
        |    }
        """.trimMargin()

        private val makeRunnableSamConstructor = """
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        myInvokeLater(Runnable { x() })
        |    }
        |
        |    fun myInvokeLater(r: java.lang.Runnable) {
        |        r.run()
        |    }
        """.trimMargin()

        private val makeRunnableSamAdapter = """
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        val t = Thread { x() }
        |        t.run() // run in same Thread
        |    }
        |
        """.trimMargin()

        // That's how asyncUI's implementation should look like (but it should call SwingUtilities.invokeLater instead if Thread)
        private val bothSamAndPlainExecution1 = """
        |    val TRUE = hashCode() % 2 == 0 || hashCode() xor 1 == 1
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        if (TRUE) {
        |           x()
        |        } else {
        |           val t = Thread { x() }
        |           t.run()
        |        }
        |    }
        |
        """.trimMargin()

        private val bothSamAndPlainExecution2 = """
        |    val FALSE = !(hashCode() % 2 == 0 || hashCode() xor 1 == 1)
        |    operator inline fun interceptResume(crossinline x: () -> Unit) {
        |        if (FALSE) {
        |           x()
        |        } else {
        |           val t = Thread { x() }
        |           t.run()
        |        }
        |    }
        |
        """.trimMargin()

        private val noInline = """
        |    operator fun interceptResume(x: () -> Unit) {
        |        x()
        |    }
        """.trimMargin()

        @JvmField
        val ALL_REPLACEMENTS =
                listOf(callLambda, sendToLambda, makeRunnableSamConstructor, makeRunnableSamAdapter, bothSamAndPlainExecution1,
                       bothSamAndPlainExecution2, noInline
                )

        const val INTERCEPT_RESUME_PLACEHOLDER = "// INTERCEPT_RESUME_PLACEHOLDER"
    }

    override fun doMultiFileTest(wholeFile: File, files: MutableList<TestFile>, javaFilesDir: File?) {
        // add "// NO_INTERCEPT_RESUME_TESTS" directive and run "Generate tests"
        // if you think that interceptResume is irrelevant for the test
        Assert.assertTrue("No placeholder in $wholeFile", files.any { it.content.contains(INTERCEPT_RESUME_PLACEHOLDER) })
        for (replacement in ALL_REPLACEMENTS) {

            try {
                super.doMultiFileTest(
                        wholeFile,
                        files.map { TestFile(it.name, it.content.replace(INTERCEPT_RESUME_PLACEHOLDER, replacement)) },
                        javaFilesDir
                )
            }
            catch(t: Throwable) {
                throw RuntimeException("Fail for replacement: \n$replacement", t)
            }

            this.initializedClassLoader = null
        }
    }
}
