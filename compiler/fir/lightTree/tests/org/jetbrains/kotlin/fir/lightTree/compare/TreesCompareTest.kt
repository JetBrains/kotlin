/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.fir.lightTree.walkTopDownWithTestData
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest : AbstractRawFirBuilderTestCase() {
    private fun compareBase(path: String, withTestData: Boolean, compareFir: (File) -> Boolean) {
        var counter = 0
        var errorCounter = 0

        val onEachFile: (File) -> Unit = { file ->
            if (!compareFir(file)) errorCounter++
            counter++
        }
        println("BASE PATH: $path")
        if (!withTestData) {
            path.walkTopDown(onEachFile)
        } else {
            path.walkTopDownWithTestData(onEachFile)
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
        TestCase.assertEquals(0, errorCounter)
    }

    private fun compareAll(stubMode: Boolean) {
        val lightTreeConverter = LightTree2Fir(myProject, stubMode)
        compareBase(System.getProperty("user.dir"), withTestData = false) { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()

            return@compareBase treeFromLightTree == treeFromPsi
        }
    }

    fun testCompareDiagnostics() {
        val lightTreeConverter = LightTree2Fir(myProject, stubMode = false)
        compareBase("compiler/testData/diagnostics/tests", withTestData = true) { file ->
            val notEditedText = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            val text = notEditedText.replace("(<!>)|(<!.*?!>)".toRegex(), "").replaceAfter(".java", "")

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode = false)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()
                .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()
                .replace("<Unsupported LValue.*?>".toRegex(), "<Unsupported LValue>")

            return@compareBase treeFromLightTree == treeFromPsi
        }
    }

    private fun compare(
        stubMode: Boolean,
        path: String = System.getProperty("user.dir"),
        withTestData: Boolean = false,
        visitAnonymousFunction: Boolean = false,
        visitLambdaExpression: Boolean = false,
        visitLocalMembers: Boolean = false,
        visitVariable: Boolean = false,
        visitAnnotation: Boolean = false,
        visitTypeOperatorCall: Boolean = false,
        visitArrayOfCall: Boolean = false,
        visitFunctionCall: Boolean = false,
        visitGetClassCall: Boolean = false,
        visitLoopJump: Boolean = false,
        visitReturnExpression: Boolean = false,
        visitThrowExpression: Boolean = false,
        visitLoops: Boolean = false,
        visitConstExpression: Boolean = false,
        visitQualifiedAccessExpression: Boolean = false,
        visitCallableReferenceAccess: Boolean = false,
        visitTryExpression: Boolean = false,
        visitWhenExpression: Boolean = false,
        visitLambdaArgumentExpression: Boolean = false,
        visitAnonymousObject: Boolean = false
    ) {
        val firVisitor = FirPartialTransformer(
            visitAnonymousFunction = visitAnonymousFunction,
            visitLambdaExpression = visitLambdaExpression,
            visitLocalMembers = visitLocalMembers,
            visitVariable = visitVariable,
            visitAnnotation = visitAnnotation,
            visitTypeOperatorCall = visitTypeOperatorCall,
            visitArrayOfCall = visitArrayOfCall,
            visitFunctionCall = visitFunctionCall,
            visitGetClassCall = visitGetClassCall,
            visitLoopJump = visitLoopJump,
            visitReturnExpression = visitReturnExpression,
            visitThrowExpression = visitThrowExpression,
            visitLoops = visitLoops,
            visitConstExpression = visitConstExpression,
            visitQualifiedAccessExpression = visitQualifiedAccessExpression,
            visitCallableReferenceAccess = visitCallableReferenceAccess,
            visitTryExpression = visitTryExpression,
            visitWhenExpression = visitWhenExpression,
            visitLambdaArgumentExpression = visitLambdaArgumentExpression,
            visitAnonymousObject = visitAnonymousObject
        )
        val lightTreeConverter = LightTree2Fir(myProject, stubMode)
        compareBase(path, withTestData) { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            firVisitor.transformFile(firFileFromLightTree, null)
            //val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firVisitor.transformFile(firFileFromLightTree, null).single) }.toString()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode)
            firVisitor.transformFile(firFileFromPsi, null)
            //val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firVisitor.transformFile(firFileFromPsi, null).single) }.toString()

            return@compareBase firFileFromPsi.areEqualTo(firFileFromLightTree)
        }
    }

    fun testStubCompareAll() {
        compareAll(stubMode = true)
    }

    fun testCompareAll() {
        compareAll(stubMode = false)
    }

    fun testStubCompareWithoutAnnotations() {
        compare(stubMode = true, visitAnnotation = false)
    }

    fun testConstExpression() {
        compare(stubMode = false, visitConstExpression = true, visitAnnotation = true)
    }

    fun testCallExpression() {
        compare(stubMode = false, visitFunctionCall = true, visitLambdaExpression = false)
    }

    fun testQualifiedAccessExpression() {
        compare(stubMode = false, visitFunctionCall = true, visitQualifiedAccessExpression = true)
    }

    fun testTypeOperatorCall() {
        compare(
            stubMode = false,
            visitTypeOperatorCall = true,
            visitFunctionCall = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true
        )
    }

    fun testArrayOfCall() {
        compare(
            stubMode = false,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true
        )
    }

    fun testDoubleColonSuffix() {
        compare(
            stubMode = false,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true
        )
    }

    fun testLocalMembers() {
        compare(
            stubMode = false,
            visitLocalMembers = true,
            visitAnnotation = true,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true
        )
    }

    fun testReturn() {
        compare(
            stubMode = false,
            visitLocalMembers = true,
            visitAnnotation = true,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitReturnExpression = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true
        )
    }

    fun testLambdaExpression() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitAnnotation = true,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitReturnExpression = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true
        )
    }

    fun testLambdaArgument() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitAnnotation = true,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitReturnExpression = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true,
            visitLambdaArgumentExpression = true
        )
    }

    fun testAnonymousObject() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitAnnotation = true,
            visitTypeOperatorCall = true,
            visitArrayOfCall = true,
            visitFunctionCall = true,
            visitGetClassCall = true,
            visitReturnExpression = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitCallableReferenceAccess = true,
            visitLambdaArgumentExpression = true,
            visitAnonymousObject = true
        )
    }

    fun testLoops() {
        compare(
            stubMode = false,
            visitLambdaExpression = true,
            visitAnnotation = true,
            visitFunctionCall = true,
            visitReturnExpression = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true
        )
    }

    fun testVariables() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitVariable = true,
            visitAnnotation = true,
            visitFunctionCall = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitAnonymousObject = true
        )
    }

    fun testLoopJump() {
        compare(
            stubMode = false,
            visitAnnotation = true,
            visitLoopJump = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true
        )
    }

    fun testThrowExpression() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitVariable = true,
            visitAnnotation = true,
            visitFunctionCall = true,
            visitThrowExpression = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitAnonymousObject = true
        )
    }

    fun testTryExpression() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitVariable = true,
            visitAnnotation = true,
            visitFunctionCall = true,
            visitThrowExpression = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitTryExpression = true,
            visitAnonymousObject = true
        )
    }

    fun testWhenExpression() {
        compare(
            stubMode = false,
            visitAnonymousFunction = true,
            visitLambdaExpression = true,
            visitLocalMembers = true,
            visitVariable = true,
            visitAnnotation = true,
            visitFunctionCall = true,
            visitThrowExpression = true,
            visitLoops = true,
            visitConstExpression = true,
            visitQualifiedAccessExpression = true,
            visitWhenExpression = true,
            visitAnonymousObject = true
        )
    }
}
