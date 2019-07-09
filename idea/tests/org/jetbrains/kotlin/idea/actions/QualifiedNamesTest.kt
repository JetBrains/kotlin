/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.CopyReferenceAction
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.util.*

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class QualifiedNamesTest: LightCodeInsightTestCase() {
    fun testClassRef() {
        LightPlatformCodeInsightTestCase.configureFromFileText(
                "class.kt",
                """
                    package foo.bar

                    class Klass {
                        class Nested

                        companion object {
                        }
                    }

                    object Object {
                    }

                    val anonymous = object {
                    }
                """
        )
        assertEquals(listOf("foo.bar.Klass", "foo.bar.Klass.Nested", "foo.bar.Klass.Companion", "foo.bar.Object", "foo.bar.ClassKt#getAnonymous", null),
                     getQualifiedNamesForDeclarations())
    }

    fun testFunRef() {
        LightPlatformCodeInsightTestCase.configureFromFileText(
                "fun.kt",
                """
                    package foo.bar

                    class Klass {
                        fun memberFun() {
                        }

                        val memberVal = ":)"
                    }

                    fun topLevelFun()

                    val topLevelVal = ":)"
                """
        )
        assertEquals(listOf("foo.bar.Klass", "foo.bar.Klass#memberFun", "foo.bar.Klass#getMemberVal", "foo.bar.FunKt#topLevelFun", "foo.bar.FunKt#getTopLevelVal"),
                     getQualifiedNamesForDeclarations())
    }

    private fun getQualifiedNamesForDeclarations(): List<String?> {
        val result = ArrayList<String?>()
        LightPlatformCodeInsightTestCase.myFile.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                result.add(CopyReferenceAction.elementToFqn(declaration))
                super.visitNamedDeclaration(declaration)
            }
        })
        return result
    }
}
