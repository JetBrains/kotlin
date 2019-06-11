/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

class KotlinProjectsPerformanceTest : AbstractKotlinProjectsPerformanceTest() {

    fun testHelloWorldProject() {
        tcSuite("HelloWorld") {
            perfOpenProject("helloKotlin")

            // highlight
            perfHighlightFile("src/HelloMain.kt")
            perfHighlightFile("src/HelloMain2.kt")

            // change document
            perfChangeDocument("src/HelloMain2.kt", "type a single char") { doc ->
                val text = doc.text
                val offset = text.indexOf("println")

                doc.insertString(offset, "p\n")
            }

            perfChangeDocument("src/HelloMain.kt", "type val expression") { doc ->
                val text = doc.text
                val offset = text.indexOf("println")

                doc.insertString(offset, "val s =\n")
            }
        }
    }

    fun testAutoCompletion() {
        tcSuite("AutoCompletion") {

            attempts {
                perfAutoCompletion(
                    "inside of method: println$it",
                    """
                    fun bar() {
                        print<caret>
                    }
                    """,
                    suggestions = arrayOf("println", "print"),
                    type = "l\r",
                    after = """
                    fun bar() {
                        println()
                    }
                    """
                )
            }

            attempts {
                perfAutoCompletion(
                    "outside of method: arrayListOf$it",
                    before = """
                    val f: List<String> = array<caret>

                    fun bar(){ }
                    """,
                    type = "\n",
                    suggestions = arrayOf("arrayOf", "arrayOfNulls", "emptyArray"),
                    after = """
                    val f: List<String> = arrayListOf()

                    fun bar(){ }
                    """
                )
            }
        }
    }

    fun testKotlinProject() {
        tcSuite("Kotlin") {
            perfOpenProject("perfTestProject", "..")

            perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt")

            perfHighlightFile("compiler/psi/src/org/jetbrains/kotlin/psi/KtElement.kt")
        }
    }

}