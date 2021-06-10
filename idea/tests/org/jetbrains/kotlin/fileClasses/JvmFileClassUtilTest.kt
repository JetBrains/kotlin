/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fileClasses

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.test.AstAccessControl.dropPsiAndTestWithControlledAccessToAst
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class JvmFileClassUtilTest : KotlinLightCodeInsightFixtureTestCaseBase() {
    fun testStubAccessWithACorruptedJvmName() {
        doTestJvmNameStubAccess(
            """
            <!ILLEGAL_JVM_NAME!>@JvmName(<!NO_VALUE_FOR_PARAMETER!>)<!><!>
            fun foo() {}
        """.trimIndent(), null
        )
    }

    fun testStubAccessWithAValidJvmName() {
        doTestJvmNameStubAccess(
            """
            @JvmName("bar")
            fun foo() {}
        """.trimIndent(), "bar"
        )
    }

    private fun doTestJvmNameStubAccess(content: String, expected: String?) {
        val ktFile = myFixture.configureByText("jvmName.kt", content) as KtFile
        assertNull("file is parsed from AST", ktFile.stub)

        dropPsiAndTestWithControlledAccessToAst(true, ktFile, testRootDisposable) {
            val annotationEntries =
                ktFile.findDescendantStubChildrenByType<KtAnnotationEntry>(KtStubElementTypes.ANNOTATION_ENTRY)
            assertTrue(annotationEntries.all { it.stub != null })
            assertEquals(1, annotationEntries.size)

            with(JvmFileClassUtil.getLiteralStringFromAnnotation(annotationEntries.first())) {
                expected?.run { assertEquals(expected, this) } ?: run { assertNull(this) }
            }
        }
    }

    inline fun <reified T : PsiElement> KtFile.findDescendantStubChildrenByType(elementType: IElementType): List<T> {
        val array = emptyArray<T>()
        val result = mutableListOf<T>()
        stub?.forEachDescendantStubChildren { stubElement ->
            stubElement.getChildrenByType(elementType, array).let { array ->
                array.forEach { result += it }
            }
        }
        return result
    }

    fun StubElement<*>.forEachDescendantStubChildren(action: (StubElement<*>) -> Unit) {
        action(this)
        childrenStubs.forEach {
            it.forEachDescendantStubChildren(action)
        }
    }

}