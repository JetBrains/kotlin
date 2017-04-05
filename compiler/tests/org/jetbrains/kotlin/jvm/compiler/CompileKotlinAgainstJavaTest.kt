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

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.runner.RunWith

@SuppressWarnings("all")
@TestMetadata("compiler/testData/compileKotlinAgainstJava")
@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class CompileKotlinAgainstJavaTest : AbstractCompileJavaAgainstKotlinTest() {

    @TestMetadata("Interface.kt")
    fun testImplementsInterface() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Interface.kt")
        doTest(fileName)
    }

    @TestMetadata("AbstractClass.kt")
    fun testExtendsAbstractClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/AbstractClass.kt")
        doTest(fileName)
    }

    @TestMetadata("Class.kt")
    fun testExtendsClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Class.kt")
        doTest(fileName)
    }

    @TestMetadata("ListImpl.kt")
    fun testExtendsListImpl() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ListImpl.kt")
        doTest(fileName)
    }

    @TestMetadata("CyclicDependencies.kt")
    fun testCyclicDependencies() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/CyclicDependencies.kt")
        doTest(fileName)
    }

    @TestMetadata("Enum.kt")
    fun testEnum() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Enum.kt")
        doTest(fileName)
    }

    @TestMetadata("Singleton.kt")
    fun testSingleton() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Singleton.kt")
        doTest(fileName)
    }

    @TestMetadata("Method.kt")
    fun testMethod() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Method.kt")
        doTest(fileName)
    }

    @TestMetadata("MethodWithArgument.kt")
    fun testMethodWithArgument() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/MethodWithArgument.kt")
        doTest(fileName)
    }

    @TestMetadata("SimpleAnnotation.kt")
    fun testSimpleAnnotation() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/SimpleAnnotation.kt")
        doTest(fileName)
    }

    @TestMetadata("AnnotationWithArguments.kt")
    fun testAnnotationWithArguments() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/AnnotationWithArguments.kt")
        doTest(fileName)
    }

    @TestMetadata("TypeParameter.kt")
    fun testTypeParameter() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/TypeParameter.kt")
        doTest(fileName)
    }

    @TestMetadata("ClassWithTypeParameter.kt")
    fun testClassWithTypeParameter() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ClassWithTypeParameter.kt")
        doTest(fileName)
    }

    @TestMetadata("MethodWithTypeParameter.kt")
    fun testMethodWithTypeParameter() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/MethodWithTypeParameter.kt")
        doTest(fileName)
    }

    @TestMetadata("MethodWithSeveralTypeParameters.kt")
    fun testMethodWithSeveralTypeParameters() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/MethodWithSeveralTypeParameters.kt")
        doTest(fileName)
    }

    @TestMetadata("MethodWithWildcard.kt")
    fun testMethodWithWildcard() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/MethodWithWildcard.kt")
        doTest(fileName)
    }

    @TestMetadata("SimpleWildcard.kt")
    fun testSimpleWildcard() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/SimpleWildcard.kt")
        doTest(fileName)
    }

    @TestMetadata("RawReturnType.kt")
    fun testRawReturnType() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/RawReturnType.kt")
        doTest(fileName)
    }

    @TestMetadata("Vararg.kt")
    fun testVararg() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Vararg.kt")
        doTest(fileName)
    }

    @TestMetadata("StaticNestedClass.kt")
    fun testStaticNestedClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/StaticNestedClass.kt")
        doTest(fileName)
    }

    @TestMetadata("InnerClass.kt")
    fun testInnerClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/InnerClass.kt")
        doTest(fileName)
    }

    @TestMetadata("ReturnType.kt")
    fun testReturnType() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ReturnType.kt")
        doTest(fileName)
    }

    @TestMetadata("ReturnNested.kt")
    fun testReturnNested() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ReturnNested.kt")
        doTest(fileName)
    }

    @TestMetadata("ReturnNestedFQ.kt")
    fun testReturnNestedFQ() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ReturnNestedFQ.kt")
        doTest(fileName)
    }

    @TestMetadata("ReturnInnerInner.kt")
    fun testReturnInnerInner() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ReturnInnerInner.kt")
        doTest(fileName)
    }

    @TestMetadata("SeveralInnerClasses.kt")
    fun testSeveralInnerClasses() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/SeveralInnerClasses.kt")
        doTest(fileName)
    }

    @TestMetadata("ReturnTypeResolution.kt")
    fun testReturnTypeResolution() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ReturnTypeResolution.kt")
        doTest(fileName)
    }

    @TestMetadata("AsteriskInImport.kt")
    fun testAsteriskInImport() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/AsteriskInImport.kt")
        doTest(fileName)
    }

    @TestMetadata("InterfaceField.kt")
    fun testInterfaceField() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/InterfaceField.kt")
        doTest(fileName)
    }

}