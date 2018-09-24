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

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.completion.test.assertInstanceOf
import org.jetbrains.kotlin.idea.facet.configureFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class KtLightAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testBooleanAnnotationDefaultValue() {
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

        val annotation = myFixture.findClass("AnnotatedClass").methods.first { it.name == "bar" }.parameterList.parameters.single()
                .expectAnnotations(2).single { it.qualifiedName == "Qualifier" }
        val annotationAttributeVal = annotation.findAttributeValue("value") as PsiExpression
        assertTextRangeAndValue("\"foo\"", "foo", annotationAttributeVal)
        TestCase.assertEquals(
            PsiType.getJavaLangString(psiManager, GlobalSearchScope.everythingScope(project)),
            annotationAttributeVal.type
        )
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
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
    }

    fun testConstants() {
        myFixture.addClass(
            """
            public @interface StringAnnotation {
                  String value();
            }
        """.trimIndent()
        )
        myFixture.addClass(
            """
            public class Constants {

                public static final String MY_CONSTANT = "67";
            }

        """.trimIndent()
        )

        myFixture.configureByText(
            "AnnotatedClass.kt", """
            @StringAnnotation(Constants.MY_CONSTANT)
            open class AnnotatedClass
        """.trimIndent()
        )
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiLiteral
        assertTextAndRange("Constants.MY_CONSTANT", annotationAttributeVal)
        TestCase.assertEquals("67", annotationAttributeVal.value)
        TestCase.assertEquals(
            PsiType.getJavaLangString(psiManager, GlobalSearchScope.everythingScope(project)),
            (annotationAttributeVal as PsiExpression).type
        )
    }


    fun testLiteralReplace() {
        myFixture.addClass(
            """
            public @interface StringAnnotation {
                  String value();
            }
        """.trimIndent()
        )

        myFixture.configureByText(
            "AnnotatedClass.kt", """
            @StringAnnotation("oldValue")
            open class AnnotatedClass
        """.trimIndent()
        )
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiLiteral
        assertTextAndRange("\"oldValue\"", annotationAttributeVal)
        TestCase.assertEquals("oldValue", annotationAttributeVal.value)
        runWriteAction {
            CommandProcessor.getInstance().runUndoTransparentAction {
                annotationAttributeVal.replace(
                    JavaPsiFacade.getElementFactory(project).createExpressionFromText("\"newValue\"", annotationAttributeVal)
                )
            }
        }
        myFixture.checkResult(
            """
            @StringAnnotation("newValue")
            open class AnnotatedClass
        """.trimIndent()
        )
    }

    fun testReferences() {
        myFixture.addClass(
            """
            public @interface StringAnnotation {
                  String value();
            }
        """.trimIndent()
        )

        myFixture.configureByText(
            "AnnotatedClass.kt", """
            @StringAnnotation("someText")
            open class AnnotatedClass
        """.trimIndent()
        )
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiLiteral
        assertTextAndRange("\"someText\"", annotationAttributeVal)
        TestCase.assertTrue(
            "String literal references should be available via light literal",
            annotationAttributeVal.references.any {
                /* FileReferences are injected in every string, so we use them as indicator that KtElement references are available there */
                it is FileReference
            })
    }

    fun testClassLiteral() {
        myFixture.addClass(
            """
            public @interface ClazzAnnotation {
                  Class<?> cls();
            }
        """.trimIndent()
        )

        myFixture.configureByText(
            "AnnotatedClass.kt", """
            @ClazzAnnotation(cls = String::class)
            class AnnotatedClass
        """.trimIndent()
        )
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("cls") as PsiClassObjectAccessExpression
        assertTextAndRange("String::class", annotationAttributeVal)
        TestCase.assertEquals(
            PsiType.getJavaLangString(myFixture.psiManager, GlobalSearchScope.everythingScope(project)),
            annotationAttributeVal.operand.type
        )
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
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
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
        assertIsKtLightAnnotation("Anno3()", innerAnnotationAttributeVal)
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
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
        TestCase.assertTrue(annotations.first().parameterList.attributes.any { (it.value == annotationAttributeVal) })
        assertTextAndRange("arrayOf(\"abc\", \"def\")", annotationAttributeVal)

        annotationAttributeVal as PsiArrayInitializerMemberValue
        assertTextAndRange("\"abc\"", annotationAttributeVal.initializers[0])
        assertTextAndRange("\"def\"", annotationAttributeVal.initializers[1])
    }

    fun testKotlinAnnotationWithStringArrayLiteral() {
        configureKotlinVersion("1.2")
        myFixture.configureByText("AnnotatedClass.kt", """
            annotation class Anno(val params: Array<String>)
            @Anno(params = ["abc", "def"])
            class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("params") as PsiElement
        assertTextAndRange("[\"abc\", \"def\"]", annotationAttributeVal)

        annotationAttributeVal as PsiArrayInitializerMemberValue
        assertTextAndRange("\"abc\"", annotationAttributeVal.initializers[0].assertInstanceOf<PsiLiteral>())
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

        val annotation = myFixture.findClass("AnnotatedClass").expectAnnotations(1).single()

        val annotationAttributeVal = annotation.findAttributeValue("anno2") as PsiArrayInitializerMemberValue
        annotationAttributeVal.parent.assertInstanceOf<PsiNameValuePair>().let { pair ->
            TestCase.assertEquals("anno2", pair.name)
        }

        assertTextAndRange("arrayOf(Anno2(1), Anno2(2))", annotationAttributeVal)
        annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
            assertTextAndRange("Anno2(1)", innerAnnotationAttributeVal)
            assertIsKtLightAnnotation("Anno2(1)", innerAnnotationAttributeVal)
            innerAnnotationAttributeVal as PsiAnnotation
            val value = innerAnnotationAttributeVal.findAttributeValue("v").assertInstanceOf<PsiLiteral>()
            assertTextAndRange("1", value)
            TestCase.assertEquals(PsiType.INT, value.assertInstanceOf<PsiExpression>().type)
        }


        val attributeValueFromParameterList = annotation.parameterList.attributes.single().value as PsiArrayInitializerMemberValue
        assertTextAndRange("arrayOf(Anno2(1), Anno2(2))", attributeValueFromParameterList)
        attributeValueFromParameterList.initializers[0].let { innerAnnotationAttributeVal ->
            assertTextAndRange("Anno2(1)", innerAnnotationAttributeVal)
            assertIsKtLightAnnotation("Anno2(1)", innerAnnotationAttributeVal)
        }

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

        val annotations = myFixture.findClass("MyAnnotated").expectAnnotations(1)
        annotations[0].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("Inner()", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("Inner()", innerAnnotationAttributeVal)
                assertIsKtLightAnnotation("Inner()", innerAnnotationAttributeVal)
            }
            UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
            TestCase.assertTrue(annotation.parameterList.attributes.any { it.value == annotationAttributeVal })
        }

    }

    fun testVarargWithSpread() {
        myFixture.addClass("""
            public @interface Annotation {
                String[] value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @Annotation(value = *arrayOf("a", "b", "c"))
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("Annotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiArrayInitializerMemberValue
        assertTextAndRange("arrayOf(\"a\", \"b\", \"c\")", annotationAttributeVal)
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
        for ((i, arg) in listOf("\"a\"", "\"b\"", "\"c\"").withIndex()) {
            assertTextAndRange(arg, annotationAttributeVal.initializers[i])
        }
    }

    fun testVarargWithSpreadComplex() {
        myFixture.addClass("""
            public @interface Annotation {
                String[] value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @Annotation(value = arrayOf(*arrayOf("a", "b"), "c", *arrayOf("d", "e")))
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("Annotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiArrayInitializerMemberValue
        assertTextAndRange("arrayOf(*arrayOf(\"a\", \"b\"), \"c\", *arrayOf(\"d\", \"e\"))", annotationAttributeVal)
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
        for ((i, arg) in listOf("arrayOf(\"a\", \"b\")", "\"c\"", "arrayOf(\"d\", \"e\")").withIndex()) {
            assertTextAndRange(arg, annotationAttributeVal.initializers[i])
        }
    }

    fun testVarargWithArrayLiteral() {
        myFixture.addClass("""
            public @interface Annotation {
                String[] value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @Annotation(value = ["a", "b", "c"])
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("Annotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiArrayInitializerMemberValue
        assertTextAndRange("[\"a\", \"b\", \"c\"]", annotationAttributeVal)
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
        for ((i, arg) in listOf("\"a\"", "\"b\"", "\"c\"").withIndex()) {
            assertTextAndRange(arg, annotationAttributeVal.initializers[i])
        }
    }

    fun testVarargWithSingleArg() {
        myFixture.addClass("""
            public @interface Annotation {
                String[] value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @Annotation(value = "a")
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("Annotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiArrayInitializerMemberValue
        assertTextAndRange("\"a\"", annotationAttributeVal)
        UsefulTestCase.assertInstanceOf(annotationAttributeVal.parent, PsiNameValuePair::class.java)
        for ((i, arg) in listOf("\"a\"").withIndex()) {
            assertTextAndRange(arg, annotationAttributeVal.initializers[i])
        }
    }

    fun testVarargWithArrayLiteralAndSpread() {
        myFixture.addClass("""
            public @interface Annotation {
                String[] value();
            }
        """.trimIndent())

        myFixture.configureByText("AnnotatedClass.kt", """
            @Annotation(*["a", "b", "c"])
            open class AnnotatedClass
        """.trimIndent())
        myFixture.testHighlighting("Annotation.java", "AnnotatedClass.kt")

        val annotations = myFixture.findClass("AnnotatedClass").expectAnnotations(1)
        val annotationAttributeVal = annotations.first().findAttributeValue("value") as PsiArrayInitializerMemberValue
        assertTextAndRange("[\"a\", \"b\", \"c\"]", annotationAttributeVal)
        for ((i, arg) in listOf("\"a\"", "\"b\"", "\"c\"").withIndex()) {
            assertTextAndRange(arg, annotationAttributeVal.initializers[i])
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
            assertTextAndRange("\"1\"", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"1\"", innerAnnotationAttributeVal)
            }
        }
        annotations[2].let { annotation ->
            val annotationAttributeVal = annotation.findAttributeValue("value") as PsiElement
            assertTextAndRange("(\"1\", \"2\")", annotationAttributeVal)
            annotationAttributeVal as PsiArrayInitializerMemberValue
            annotationAttributeVal.initializers[0].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"1\"", innerAnnotationAttributeVal)
            }
            annotationAttributeVal.initializers[1].let { innerAnnotationAttributeVal ->
                assertTextAndRange("\"2\"", innerAnnotationAttributeVal)
            }
        }

    }

    fun testJavaKeywordsInName() {
        myFixture.configureByText(
            "AnnotatedClass.kt", """
            package my.public.place

            annotation class Anno1(val import: String)

            @Anno1(import = "full")
            class AnnotatedClass
        """.trimIndent()
        )

        val annotations = myFixture.findClass("my.public.place.AnnotatedClass").expectAnnotations(1)
        val annotation = annotations.first()
        TestCase.assertEquals("my.public.place.Anno1", annotation.qualifiedName)
        assertTextAndRange("\"full\"", annotation.findAttributeValue("import")!!)
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
        assertTextAndRange("true", annotation.findAttributeValue("i")!!)
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
        TestCase.assertEquals("mismatch for $psiElement of ${psiElement.javaClass}", expected, psiElement.text)
        TestCase.assertEquals(expected, psiElement.textRange.substring(psiElement.containingFile.text))
        TestCase.assertEquals(psiElement, PsiAnchor.create(psiElement).retrieve())
    }

    private fun assertIsKtLightAnnotation(expected: String, psiElement: PsiElement) {
        TestCase.assertEquals(expected, (psiElement as KtLightAnnotationForSourceEntry).kotlinOrigin.text)
    }

    private fun assertTextRangeAndValue(expected: String, value: Any?, psiElement: PsiElement) {
        assertTextAndRange(expected, psiElement)
        val result = JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(psiElement)
        TestCase.assertEquals(value, result)
        val smartPointer = SmartPointerManager.getInstance(psiElement.project).createSmartPsiElementPointer(psiElement)
        assertTextAndRange(expected, smartPointer.element!!)
    }

    private fun PsiModifierListOwner.expectAnnotations(number: Int): Array<PsiAnnotation> =
        this.modifierList!!.annotations.apply {
            assertEquals(
                "expected $number annotation(s), found [${this.joinToString(", ") { it.qualifiedName ?: "unknown" }}]",
                number, size
            )
        }

    private fun configureKotlinVersion(version: String) {
        WriteAction.run<Throwable> {
            val modelsProvider = IdeModifiableModelsProviderImpl(project)
            val facet = module.getOrCreateFacet(modelsProvider, useProjectSettings = false)
            facet.configureFacet(version, LanguageFeature.State.DISABLED, null, modelsProvider)
            modelsProvider.commit()
        }
    }

}