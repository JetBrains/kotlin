/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.java.annotations

class KtLightAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testAnnotationsInAnnotationsDeclarations() {
        myFixture.addClass("""
            public @interface OuterAnnotation {
                InnerAnnotation attribute();
                @interface InnerAnnotation {
                }
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @OuterAnnotation(attribute = OuterAnnotation.InnerAnnotation())
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("OuterAnnotation.java", "AnnotatedClass.kt")

        val headerClass = listOf(myFixture.findClass("AnnotatedClass"))
        assertEquals(1, headerClass.size)
        val annotations = headerClass.first().annotations
        assertEquals(1, annotations.size)
        val annotation = annotations.first()

        val annotationAttributeVal = annotation.findAttributeValue("attribute") as PsiElement

        TestCase.assertEquals("InnerAnnotation()", annotationAttributeVal.text)
        TestCase.assertEquals(TextRange(45, 62), annotationAttributeVal.textRange)
    }

    fun testAnnotationsInAnnotationsFinalDeclarations() {
        myFixture.addClass("""
            public @interface OuterAnnotation {
                InnerAnnotation attribute();
                @interface InnerAnnotation {
                }
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @OuterAnnotation(attribute = OuterAnnotation.InnerAnnotation())
            class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("OuterAnnotation.java", "AnnotatedClass.kt")

        val headerClass = listOf(myFixture.findClass("AnnotatedClass"))
        assertEquals(1, headerClass.size)
        val annotations = headerClass.first().annotations
        assertEquals(1, annotations.size)
        val annotation = annotations.first()

        val annotationAttributeVal = annotation.findAttributeValue("attribute") as PsiElement

        TestCase.assertEquals("InnerAnnotation()", annotationAttributeVal.text)
        TestCase.assertEquals(TextRange(45, 62), annotationAttributeVal.textRange)
    }

    fun testAnnotationsInAnnotationsInAnnotationsDeclarations() {
        myFixture.addClass("""
            public @interface OuterAnnotation {
                InnerAnnotation attribute();
                @interface InnerAnnotation {
                    InnerInnerAnnotation attribute();
                    @interface InnerInnerAnnotation {
                    }
                }
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @OuterAnnotation(attribute = OuterAnnotation.InnerAnnotation(attribute = OuterAnnotation.InnerAnnotation.InnerInnerAnnotation()))
            open class AnnotatedClass //There is another exception if class is not open
        """.trimIndent())
        myFixture.testHighlighting("OuterAnnotation.java", "AnnotatedClass.kt")

        val headerClass = listOf(myFixture.findClass("AnnotatedClass"))
        assertEquals(1, headerClass.size)
        val annotations = headerClass.first().annotations
        assertEquals(1, annotations.size)
        val annotation = annotations.first()

        val annotationAttributeVal = annotation.findAttributeValue("attribute") as PsiElement

        TestCase.assertEquals("InnerAnnotation(attribute = OuterAnnotation.InnerAnnotation.InnerInnerAnnotation())", annotationAttributeVal.text)
        TestCase.assertEquals(TextRange(45, 128), annotationAttributeVal.textRange)

        annotationAttributeVal as PsiAnnotation
        val innerAnnotationAttributeVal = annotationAttributeVal.findAttributeValue("attribute") as PsiElement
        TestCase.assertEquals("InnerInnerAnnotation()", innerAnnotationAttributeVal.text)
        TestCase.assertEquals(TextRange(105, 127), innerAnnotationAttributeVal.textRange)
    }

    fun testKotlinAnnotations() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val anno2: Anno2)
            annotation class Anno2(val anno3: Anno3)
            annotation class Anno3

            @Anno1(Anno2(Anno3()))
            class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("AnnotatedClass.kt")

        val headerClass = listOf(myFixture.findClass("AnnotatedClass"))
        assertEquals(1, headerClass.size)
        val annotations = headerClass.first().annotations
        assertEquals(1, annotations.size)
        val annotation = annotations.first()

        val annotationAttributeVal = annotation.findAttributeValue("anno2") as PsiElement

        TestCase.assertEquals("Anno2(Anno3())", annotationAttributeVal.text)
        TestCase.assertEquals(TextRange(113, 127), annotationAttributeVal.textRange)

        annotationAttributeVal as PsiAnnotation
        val innerAnnotationAttributeVal = annotationAttributeVal.findAttributeValue("anno3") as PsiElement
        TestCase.assertEquals("Anno3()", innerAnnotationAttributeVal.text)
        TestCase.assertEquals(TextRange(119, 126), innerAnnotationAttributeVal.textRange)
    }

}