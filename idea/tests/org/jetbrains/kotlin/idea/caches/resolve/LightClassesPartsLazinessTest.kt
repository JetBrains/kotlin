/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.impl.PsiJavaParserFacadeImpl
import org.jetbrains.kotlin.idea.completion.test.withServiceRegistered
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.WrappedTypeFactory
import org.jetbrains.kotlin.utils.sure
import org.mockito.Mockito

class LightClassesPartsLazinessTest : KotlinLightCodeInsightFixtureTestCase() {
    private fun doTestForDeferredTypes(fileName: String, block: (WrappedTypeFactoryWithCounter, PsiClass) -> Unit) {
        val ktFile = prepareKtFile(fileName)

        val wrappedTypeFactoryWithCounter = WrappedTypeFactoryWithCounter()

        project.withServiceRegistered<WrappedTypeFactory, Unit>(wrappedTypeFactoryWithCounter) {
            val psiClass = findClass("A", ktFile, project).sure { "Class was not found" }
            wrappedTypeFactoryWithCounter.assertZero()

            block(wrappedTypeFactoryWithCounter, psiClass)
        }
    }

    private fun prepareKtFile(fileName: String): KtFile {
        val testDataPath = "compiler/testData/asJava/lightClassesPartsLaziness/$fileName"
        myFixture.configureByFiles(testDataPath)
        return myFixture.file as KtFile
    }

    private fun doTestForDeferredCompileTimeInitializer(
        fileName: String,
        block: (Function0<Boolean>, PsiClass) -> Unit
    ) {
        val ktFile = prepareKtFile(fileName)
        val mockForEvaluator = Mockito.mock(ConstantExpressionEvaluator::class.java)

        var wasCalled = false

        Mockito.`when`(
            mockForEvaluator.evaluateExpression(
                anyMockitoInstance(), anyMockitoInstance(), anyMockitoInstance()
            )
        ).thenAnswer {
            wasCalled = true
            null
        }

        project.withServiceRegistered<ConstantExpressionEvaluator, Unit>(mockForEvaluator) {
            val psiClass = findClass("A", ktFile, project).sure { "Class was not found" }
            block({ wasCalled }, psiClass)
        }
    }

    private fun PsiClass.findSingleMethod(name: String): PsiMethod =
        findMethodsByName(name, false).let {
            assertEquals(1, it.size)
            it[0]
        }

    private fun PsiClass.findSingleField(name: String): PsiField =
        findFieldByName(name, false).sure { "$name field was not found" }

    fun testFunWithDeferredType() {
        doTestForDeferredTypes("functionType.kt") { wrappedTypeFactoryWithCounter, psiClass ->
            val psiMethod = psiClass.findSingleMethod("foo")

            val signature = psiMethod.getSignature(PsiSubstitutor.EMPTY)
            wrappedTypeFactoryWithCounter.assertZero()

            assertEquals("foo", signature.name)
            assertEquals(PsiJavaParserFacadeImpl.getPrimitiveType("int"), signature.parameterTypes[0])
            wrappedTypeFactoryWithCounter.assertZero()

            assertEquals("java.lang.String", psiMethod.returnType?.canonicalText)
            wrappedTypeFactoryWithCounter.assertSingle()
        }
    }

    fun testValWithDeferredType() {
        doTestForDeferredTypes("valType.kt") { wrappedTypeFactoryWithCounter, psiClass ->
            val psiMethod = psiClass.findSingleMethod("getFoo")
            val signature = psiMethod.getSignature(PsiSubstitutor.EMPTY)

            assertEquals(0, signature.parameterTypes.size)
            val psiField = psiClass.findSingleField("foo")
            assertTrue(psiField.hasModifier(JvmModifier.PRIVATE))
            wrappedTypeFactoryWithCounter.assertZero()

            assertEquals("java.lang.String", psiMethod.returnType?.canonicalText)
            wrappedTypeFactoryWithCounter.assertSingle()

            assertEquals("java.lang.String", psiField.type.canonicalText)
            wrappedTypeFactoryWithCounter.assertSingle()
        }
    }

    fun testVarWithDeferredType() {
        doTestForDeferredTypes("varType.kt") { wrappedTypeFactoryWithCounter, psiClass ->
            val getterPsiMethod = psiClass.findSingleMethod("getFoo")
            val signature = getterPsiMethod.getSignature(PsiSubstitutor.EMPTY)

            assertEquals(0, signature.parameterTypes.size)
            val psiField = psiClass.findSingleField("foo")
            assertTrue(psiField.hasModifier(JvmModifier.PRIVATE))
            wrappedTypeFactoryWithCounter.assertZero()

            val setterPsiMethod = psiClass.findSingleMethod("setFoo")
            val setterSignature = setterPsiMethod.getSignature(PsiSubstitutor.EMPTY)
            wrappedTypeFactoryWithCounter.assertSingle()

            assertEquals("java.lang.String", setterSignature.parameterTypes[0].canonicalText)
            assertEquals("java.lang.String", getterPsiMethod.returnType?.canonicalText)
            assertEquals("java.lang.String", psiField.type.canonicalText)

            wrappedTypeFactoryWithCounter.assertSingle()
        }
    }

    fun testCompileTimeInitializer() {
        doTestForDeferredCompileTimeInitializer("compileTimeInitializer.kt") { wasComputed, psiClass ->
            assertFalse(wasComputed())

            val psiField = psiClass.findSingleField("x")
            assertTrue(psiField.hasModifier(JvmModifier.STATIC))
            assertTrue(psiField.hasModifier(JvmModifier.PUBLIC))
            assertFalse(wasComputed())

            // Actually, it should be not null, but we have only mock ConstantExpressionEvaluator
            assertNull(psiField.initializer)

            assertTrue(wasComputed())
        }
    }

    private class WrappedTypeFactoryWithCounter : WrappedTypeFactory(LockBasedStorageManager.NO_LOCKS) {
        var counterForRecursionIntolerant = 0
        override fun createRecursionIntolerantDeferredType(trace: BindingTrace, computation: () -> KotlinType): KotlinType {
            return super.createRecursionIntolerantDeferredType(trace) {
                counterForRecursionIntolerant++
                computation()
            }
        }

        fun assertZero() {
            assertEquals(0, counterForRecursionIntolerant)
        }

        fun assertSingle() {
            assertEquals(1, counterForRecursionIntolerant)
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
