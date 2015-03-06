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

package org.jetbrains.kotlin.idea.actions

import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.psi.JetVisitorVoid
import com.intellij.psi.PsiElement
import com.intellij.ide.actions.CopyReferenceAction
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import java.util.ArrayList
import kotlin.test.assertEquals

public class QualifiedNamesTest: LightCodeInsightTestCase() {
    fun testClassRef() {
        LightPlatformCodeInsightTestCase.configureFromFileText(
                "class.kt",
                """
                    package foo.bar

                    class Klass {
                        class Nested

                        default object {
                        }
                    }

                    object Object {
                    }

                    val anonymous = object {
                    }
                """
        )
        assertEquals(listOf("foo.bar.Klass", "foo.bar.Klass.Nested", "foo.bar.Klass.Default", "foo.bar.Object", "foo.bar.anonymous", null),
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
        assertEquals(listOf("foo.bar.Klass", "foo.bar.Klass.memberFun", "foo.bar.Klass.memberVal", "foo.bar.topLevelFun", "foo.bar.topLevelVal"),
                     getQualifiedNamesForDeclarations())
    }

    private fun getQualifiedNamesForDeclarations(): List<String?> {
        val result = ArrayList<String?>()
        LightPlatformCodeInsightTestCase.myFile.accept(object : JetVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitNamedDeclaration(declaration: JetNamedDeclaration) {
                result.add(CopyReferenceAction.elementToFqn(declaration))
                super.visitNamedDeclaration(declaration)
            }
        })
        return result
    }
}
