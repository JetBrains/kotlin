/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

class KtVisitorTest : KotlinTestWithEnvironment() {
    fun testTopLevelFunctionContextVisiting() {
        doTestContextReceiverVisiting("""context(String, Int) fun foo() {}""")
    }

    fun testMemberFunctionContextVisiting() {
        doTestContextReceiverVisiting("""class Foo { context(String, Int) fun foo() {} }""")
    }

    fun testClassContextVisiting() {
        doTestContextReceiverVisiting("""context(String, Int) class Foo""")
    }

    fun testFunctionalParameterContextVisiting() {
        doTestContextReceiverVisiting("""fun foo(fn: context(String, Int) () -> Unit) {}""")
    }

    fun testHiddenTokensInStringConcatenation() {
        val expectedComments = listOf("/* Block comment before plus */", "/* Block comment after plus */", "// Line comment")

        val code = """val s = "s1" ${expectedComments[0]} + ${expectedComments[1]} "s2" + ${expectedComments[2]}
            |"s3"""".trimMargin()

        val actualComments = mutableListOf<String>()
        var insideStringConcatenation = false
        var actualWhitespaceCount = 0
        var actualPlusOperatorCount = 0

        val ktElement = KtPsiFactory(project).createFile(code)
        ktElement.accept(object : KtTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is KtStringTemplateExpression -> {
                        insideStringConcatenation = true
                    }
                    is KtOperationReferenceExpression -> {
                        if (element.operationSignTokenType == org.jetbrains.kotlin.lexer.KtTokens.PLUS) {
                            actualPlusOperatorCount++
                        }
                    }
                    else -> {
                        super.visitElement(element)
                    }
                }
            }

            override fun visitComment(comment: PsiComment) {
                actualComments.add(comment.text)
            }

            override fun visitWhiteSpace(space: PsiWhiteSpace) {
                if (insideStringConcatenation) {
                    actualWhitespaceCount++
                }
            }
        })

        assertEquals(expectedComments, actualComments)
        assertEquals(7, actualWhitespaceCount)
        assertEquals(2, actualPlusOperatorCount)
    }

    private fun doTestContextReceiverVisiting(code: String) {
        val ktElement = KtPsiFactory(project).createFile(code)
        var contextParameterListVisited = false
        var contextReceiverListVisited = false
        ktElement.accept(object : KtTreeVisitorVoid() {
            override fun visitContextParameterList(contextParameterList: KtContextParameterList) {
                contextParameterListVisited = true
            }

            override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
                contextReceiverListVisited = true
            }
        })

        assert(contextParameterListVisited && contextReceiverListVisited) {
            """
            Context parameter list was not visited:

            $code
            
            """.trimIndent()
        }
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable, KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
