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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.java.annotations

// see KtFileLightClassTest
class LightClassFromTextTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testSimple() {
        myFixture.configureByText("Dummy.kt", "") as KtFile
        val classes = classesFromText("class C {}\nobject O {}")
        assertEquals(2, classes.size)
        assertEquals("C", classes[0].qualifiedName)
        assertEquals("O", classes[1].qualifiedName)
    }

    fun testFileClass() {
        myFixture.configureByText("A.kt", "fun f() {}") as KtFile
        val classes = classesFromText("fun g() {}", fileName = "A.kt")

        assertEquals(1, classes.size)
        val facadeClass = classes.single()
        assertEquals("AKt", facadeClass.qualifiedName)

        val gMethods = facadeClass.findMethodsByName("g", false)
        assertEquals(1, gMethods.size)
        assertEquals(PsiType.VOID, gMethods.single().returnType)

        val fMethods = facadeClass.findMethodsByName("f", false)
        assertEquals(0, fMethods.size)
    }

    fun testMultifileClass() {
        myFixture.configureByFiles("multifile1.kt", "multifile2.kt")

        val facadeClass = classesFromText("""
        @file:kotlin.jvm.JvmMultifileClass
        @file:kotlin.jvm.JvmName("Foo")

        fun jar() {
        }

        fun boo() {
        }
         """).single()

        assertEquals(1, facadeClass.findMethodsByName("jar", false).size)
        assertEquals(1, facadeClass.findMethodsByName("boo", false).size)
        assertEquals(0, facadeClass.findMethodsByName("bar", false).size)
        assertEquals(0, facadeClass.findMethodsByName("foo", false).size)
    }

    fun testReferenceToOuterContext() {
        val contextFile = myFixture.configureByText("Example.kt", "package foo\n class Example") as KtFile

        val syntheticClass = classesFromText("""
        package bar

        import foo.Example

        class Usage {
            fun f(): Example = Example()
        }
         """).single()

        val exampleClass = contextFile.classes.single()
        assertEquals("Example", exampleClass.name)

        val f = syntheticClass.findMethodsByName("f", false).single()
        assertEquals(exampleClass, (f.returnType as PsiClassType).resolve())
    }

    fun testHeaderDeclarations() {
        val contextFile = myFixture.configureByText("Header.kt", "header class Foo\n\nheader fun foo()\n") as KtFile
        val headerClass = contextFile.declarations.single { it is KtClassOrObject }
        assertEquals(0, headerClass.toLightElements().size)
        val headerFunction = contextFile.declarations.single { it is KtNamedFunction }
        assertEquals(0, headerFunction.toLightElements().size)
    }

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

    private fun classesFromText(text: String, fileName: String = "A.kt"): Array<out PsiClass> {
        val file = KtPsiFactory(project).createFileWithLightClassSupport(fileName, text, myFixture.file)
        val classes = file.classes
        return classes
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/asJava/fileLightClass/"
    }
}