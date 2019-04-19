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
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.nio.file.Paths

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class LightTree2FirConverterTestCases : AbstractRawFirBuilderTestCase() {
    private fun executeTest(filePath: String) {
        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val lightTree2Fir = LightTree2Fir(true, parserDefinition, lexer).buildFirFile(Paths.get(filePath)).render()

        val file = createKtFile(filePath)
        val firFile = file.toFirFile(stubMode = true)
        val firFileDump = StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()

        TestCase.assertEquals(firFileDump, lightTree2Fir)
    }

    fun testComplexTypes() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/complexTypes.kt")
    }

    fun testDerivedClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/derivedClass.kt")
    }

    fun testEnums() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/enums.kt")
    }

    fun testEnums2() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/enums2.kt")
    }

    fun testExpectActual() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/expectActual.kt")
    }

    fun testF() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/F.kt")
    }

    fun testFunctionTypes() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/functionTypes.kt")
    }

    fun testGenericFunctions() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/genericFunctions.kt")
    }

    fun testNestedClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/nestedClass.kt")
    }

    fun testNestedOfAliasedType() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/NestedOfAliasedType.kt")
    }

    fun testNestedSuperType() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/NestedSuperType.kt")
    }

    fun testNoPrimaryConstructor() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/noPrimaryConstructor.kt")
    }

    fun testSimpleClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleClass.kt")
    }

    fun testSimpleFun() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleFun.kt")
    }

    fun testSimpleTypeAlias() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleTypeAlias.kt")
    }

    fun testTypeAliasWithGeneric() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeAliasWithGeneric.kt")
    }

    fun testTypeParameterVsNested() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeParameterVsNested.kt")
    }

    fun testTypeParameters() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeParameters.kt")
    }

    fun testWhere() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/where.kt")
    }
}