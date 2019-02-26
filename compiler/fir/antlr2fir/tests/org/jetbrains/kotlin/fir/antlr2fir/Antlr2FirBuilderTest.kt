/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.antlr2fir.Antlr2FirBuilder
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinLexer
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinParser
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class Antlr2FirBuilderTest(private val stubMode: Boolean = true) {
    fun buildFirFile(path: Path): FirFile {
        val fileName = path.toString().replaceBeforeLast(File.separator, "").replace(File.separator, "")
        return buildFirFile(KotlinLexer(CharStreams.fromPath(path)), fileName)
    }

    fun buildFirFile(input: String): FirFile {
        return buildFirFile(KotlinLexer(CharStreams.fromString(input)))
    }

    private fun buildFirFile(lexer: KotlinLexer, fileName: String = ""): FirFile {
        val tokens = CommonTokenStream(lexer)
        val parser = KotlinParser(tokens)

        // TODO script
        return Antlr2FirBuilder(object : FirSessionBase() {}, stubMode, fileName).buildFirFile(parser.kotlinFile())
    }

}

class RawFirBuilderTest: AbstractRawFirBuilderTestCase() {
    private fun executeTest(filePath: String) {
        val antlr2FirResult = Antlr2FirBuilderTest(true).buildFirFile(Paths.get(filePath)).render()

        val file = createKtFile(filePath)
        val firFile = file.toFirFile(stubMode = true)
        val firFileDump = StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()

        TestCase.assertEquals(firFileDump, antlr2FirResult)
    }

    fun testComplexTypes() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/complexTypes.kt")
    }
    
    fun testDerivedClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/derivedClass.kt")
    }
    
    fun testEnums() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/enums.kt");
    }

    fun testEnums2() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/enums2.kt");
    }
    
    fun testExpectActual() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/expectActual.kt");
    }
    
    fun testF() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/F.kt");
    }
    
    fun testFunctionTypes() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/functionTypes.kt");
    }
    
    fun testGenericFunctions() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/genericFunctions.kt");
    }
    
    fun testNestedClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/nestedClass.kt");
    }

    fun testNestedOfAliasedType() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/NestedOfAliasedType.kt");
    }

    fun testNestedSuperType() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/NestedSuperType.kt");
    }

    fun testNoPrimaryConstructor() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/noPrimaryConstructor.kt");
    }

    fun testSimpleClass() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleClass.kt");
    }

    fun testSimpleFun() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleFun.kt");
    }

    fun testSimpleTypeAlias() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/simpleTypeAlias.kt");
    }

    fun testTypeAliasWithGeneric() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeAliasWithGeneric.kt");
    }

    fun testTypeParameterVsNested() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeParameterVsNested.kt");
    }

    fun testTypeParameters() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/typeParameters.kt");
    }

    fun testWhere() {
        executeTest("compiler/fir/psi2fir/testData/rawBuilder/declarations/where.kt");
    }
}
