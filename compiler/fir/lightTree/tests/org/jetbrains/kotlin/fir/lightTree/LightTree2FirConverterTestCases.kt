/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.nio.file.Paths

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class LightTree2FirConverterTestCases : AbstractRawFirBuilderTestCase() {
    private val testDirPath = "compiler/fir/psi2fir/testData/rawBuilder/declarations"

    private fun executeTest(filePath: String) {
        val lightTree2Fir = LightTree2Fir(stubMode = true, project = myProject).buildFirFile(Paths.get(filePath)).render()

        val file = createKtFile(filePath)
        val firFile = file.toFirFile(stubMode = true)
        val firFileDump = StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()

        TestCase.assertEquals(firFileDump, lightTree2Fir)
    }

    fun testComplexTypes() {
        executeTest("$testDirPath/complexTypes.kt")
    }

    fun testDerivedClass() {
        executeTest("$testDirPath/derivedClass.kt")
    }

    fun testEnums() {
        executeTest("$testDirPath/enums.kt")
    }

    fun testEnums2() {
        executeTest("$testDirPath/enums2.kt")
    }

    fun testExpectActual() {
        executeTest("$testDirPath/expectActual.kt")
    }

    fun testF() {
        executeTest("$testDirPath/F.kt")
    }

    fun testFunctionTypes() {
        executeTest("$testDirPath/functionTypes.kt")
    }

    fun testGenericFunctions() {
        executeTest("$testDirPath/genericFunctions.kt")
    }

    fun testNestedClass() {
        executeTest("$testDirPath/nestedClass.kt")
    }

    fun testNestedOfAliasedType() {
        executeTest("$testDirPath/NestedOfAliasedType.kt")
    }

    fun testNestedSuperType() {
        executeTest("$testDirPath/NestedSuperType.kt")
    }

    fun testNoPrimaryConstructor() {
        executeTest("$testDirPath/noPrimaryConstructor.kt")
    }

    fun testSimpleClass() {
        executeTest("$testDirPath/simpleClass.kt")
    }

    fun testSimpleFun() {
        executeTest("$testDirPath/simpleFun.kt")
    }

    fun testSimpleTypeAlias() {
        executeTest("$testDirPath/simpleTypeAlias.kt")
    }

    fun testTypeAliasWithGeneric() {
        executeTest("$testDirPath/typeAliasWithGeneric.kt")
    }

    fun testTypeParameterVsNested() {
        executeTest("$testDirPath/typeParameterVsNested.kt")
    }

    fun testTypeParameters() {
        executeTest("$testDirPath/typeParameters.kt")
    }

    fun testWhere() {
        executeTest("$testDirPath/where.kt")
    }
}