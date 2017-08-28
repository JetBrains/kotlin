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

import com.intellij.psi.*
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.java.annotations

class KtLightAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testBooleanAnnotation() {
        myFixture.addClass("""
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.FIELD)
            public @interface Autowired {
                boolean required() default true;
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            class AnnotatedClass{
                    @Autowired
                    lateinit var bean: String
            }
        """.trimIndent())
        myFixture.testHighlighting("Autowired.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").fields.single()
                .expectAnnotations(2).single { it.qualifiedName == "Autowired" }
        val annotationAttributeVal = annotations.findAttributeValue("required") as PsiElement
        assertTextRangeAndValue("true", true, annotationAttributeVal)
    }

    fun testStringAnnotationWithUnnamedParameter() {
        myFixture.addClass("""
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            public @interface Qualifier {
                String value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            class AnnotatedClass {
                    fun bar(@Qualifier("foo") param: String){}
            }
        """.trimIndent())
        myFixture.testHighlighting("Qualifier.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").methods.first { it.name == "bar" }.parameterList.parameters.single()
                .expectAnnotations(2).single { it.qualifiedName == "Qualifier" }
        val annotationAttributeVal = annotations.findAttributeValue("value") as PsiElement
        assertTextRangeAndValue("\"foo\"", "foo", annotationAttributeVal)
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

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("attribute") as PsiElement
        assertTextAndRange("InnerAnnotation()", annotationAttributeVal)
    }

    fun testAnnotationsInAnnotationsArrayDeclarations() {
        myFixture.addClass("""
            public @interface OuterAnnotation {
                InnerAnnotation[] attributes();
                @interface InnerAnnotation {
                }
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @OuterAnnotation(attributes = arrayOf(OuterAnnotation.InnerAnnotation()))
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("OuterAnnotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("attributes") as PsiArrayInitializerMemberValue
        assertTextAndRange("arrayOf(OuterAnnotation.InnerAnnotation())", annotationAttributeVal)
        assertTextAndRange("InnerAnnotation()", annotationAttributeVal.initializers[0])
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

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("attribute") as PsiElement
        assertTextAndRange("InnerAnnotation()", annotationAttributeVal)
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

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("attribute") as PsiElement
        assertTextAndRange("InnerAnnotation(attribute = OuterAnnotation.InnerAnnotation.InnerInnerAnnotation())", annotationAttributeVal)

        annotationAttributeVal as PsiAnnotation
        val innerAnnotationAttributeVal = annotationAttributeVal.findAttributeValue("attribute") as PsiElement
        assertTextAndRange("InnerInnerAnnotation()", innerAnnotationAttributeVal)
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

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("anno2") as PsiElement
        assertTextAndRange("Anno2(Anno3())", annotationAttributeVal)

        annotationAttributeVal as PsiAnnotation
        val innerAnnotationAttributeVal = annotationAttributeVal.findAttributeValue("anno3") as PsiElement
        assertTextAndRange("Anno3()", innerAnnotationAttributeVal)
    }

    fun testKotlinAnnotationWithStringArray() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno(val params: Array<String>)
            @Anno(arrayOf("abc", "def"))
            class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("params") as PsiElement
        assertTextAndRange("arrayOf(\"abc\", \"def\")", annotationAttributeVal)

        annotationAttributeVal as PsiArrayInitializerMemberValue
        assertTextAndRange("\"abc\"", annotationAttributeVal.initializers[0])
        assertTextAndRange("\"def\"", annotationAttributeVal.initializers[1])
    }

    fun testKotlinAnnotationsArray() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val anno2: Array<Anno2>)
            annotation class Anno2(val v:Int)

            @Anno1(anno2 = arrayOf(Anno2(1), Anno2(2)))
            class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("anno2") as PsiElement
        assertTextAndRange("arrayOf(Anno2(1), Anno2(2))", annotationAttributeVal)

        annotationAttributeVal as PsiArrayInitializerMemberValue
        val innerAnnotationAttributeVal = annotationAttributeVal.initializers[0]
        assertTextAndRange("Anno2(1)", innerAnnotationAttributeVal)
        innerAnnotationAttributeVal as PsiAnnotation
        val value = innerAnnotationAttributeVal.findAttributeValue("v")!!
        assertTextAndRange("1", value)

    }

    fun testVarargAnnotation() {

        myFixture.configureByText("Outer.java", """
            @interface Outer{
                Inner[] value() default {};
            }

            @interface Inner{
            }
        """)
        myFixture.configureByText("AnnotatedClass.kt", """
            @Outer(Inner())
            class MyAnnotated {}
        """.trimIndent())

        val annotations = listOf(myFixture.findClass("MyAnnotated")).let {
            assertEquals(1, it.size)
            it.first().annotations.apply {
                assertEquals(1, it.size)
            }
        }

        annotations[0].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("@Outer(Inner())", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("Inner()", innerAnnotationAttributeVal)
            }
        }

    }

    fun testRepeatableAnnotationsArray() {

        myFixture.configureByText("RAnno.java", """
            import java.lang.annotation.Repeatable;

            @interface RContainer{
                RAnno[] value() default {};
            }

            @Repeatable(RContainer.class)
            public @interface RAnno {
                String[] value() default {};
            }
        """)
        myFixture.configureByText("AnnotatedClass.kt", """
            @RAnno()
            @RAnno("1")
            @RAnno("1", "2")
            class AnnotatedClass
        """.trimIndent())

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(3)
        annotations[0].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("{}", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            TestCase.assertTrue(annotationAttributeVal.initializers.isEmpty())
        }
        annotations[1].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("@RAnno(\"1\")", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"1\"", innerAnnotationAttributeVal)
            }
        }
        annotations[2].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("@RAnno(\"1\", \"2\")", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"1\"", innerAnnotationAttributeVal)
            }
            annotationAttributeVal.initializers[1].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"2\"", innerAnnotationAttributeVal)
            }
        }

    }

    fun testWrongNamesPassed() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val i:Int , val j: Int)

            @Anno1(k = 3, l = 5)
            class AnnotatedClass
        """.trimIndent())

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotation = annotations.first()
        TestCase.assertNull(annotation.findAttributeValue("k"))
        TestCase.assertNull(annotation.findAttributeValue("l"))
        TestCase.assertNull(annotation.findAttributeValue("i"))
        TestCase.assertNull(annotation.findAttributeValue("j"))
    }

    fun testWrongValuesPassed() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val i: Int , val j: Int)

            @Anno1(i = true, j = false)
            class AnnotatedClass
        """.trimIndent())

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotation = annotations.first()
        assertTextAndRange("true", annotation.findAttributeValue("i")!!)
        assertTextAndRange("false", annotation.findAttributeValue("j")!!)
    }

    fun testDuplicateParameters() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val i:Int , val i: Boolean)

            @Anno1(i = true, i = 3)
            class AnnotatedClass
        """.trimIndent())

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotation = annotations.first()
        assertTextAndRange("", annotation.findAttributeValue("i")!!)
    }

    fun testMissingDefault() {
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno1(val i: Int = 0)

            @Anno1()
            class AnnotatedClass
        """.trimIndent())

        val (annotation) = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        assertTextAndRange("0", annotation.findAttributeValue("i")!!)
    }

    private fun assertTextAndRange(expected: String, psiElement: PsiElement) {
        TestCase.assertEquals(expected, psiElement.text)
        TestCase.assertEquals(expected, psiElement.textRange.substring(psiElement.containingFile.text))
    }

    private fun assertTextRangeAndValue(expected: String, value: Any?, psiElement: PsiElement) {
        assertTextAndRange(expected, psiElement)
        val result = JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(psiElement)
        TestCase.assertEquals(value, result)
    }

    private fun PsiModifierListOwner.expectAnnotations(number: Int): Array<PsiAnnotation> =
            this.annotations.apply {
                TestCase.assertEquals("expected one annotation, found ${this.joinToString(", ") { it.qualifiedName ?: "unknown" }}",
                                      number, size)
            }

}