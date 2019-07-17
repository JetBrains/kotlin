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
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class TreesCompareTest : AbstractRawFirBuilderTestCase() {
    private fun compareBase(compareFir: (File) -> Boolean) {
        val path = System.getProperty("user.dir")
        var counter = 0
        var errorCounter = 0

        println("BASE PATH: $path")
        path.walkTopDown { file ->
            if (!compareFir(file)) errorCounter++
            counter++
        }
        println("All scanned files: $counter")
        println("Files that aren't equal to FIR: $errorCounter")
        TestCase.assertEquals(0, errorCounter)
    }

    private fun compareAll(stubMode: Boolean) {
        val lightTreeConverter = LightTree2Fir(stubMode, myProject)
        compareBase { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()

            //light tree
            val firFileFromLightTree = lightTreeConverter.buildFirFile(text, file.name)
            val treeFromLightTree = StringBuilder().also { FirRenderer(it).visitFile(firFileFromLightTree) }.toString()

            //psi
            val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
            val firFileFromPsi = ktFile.toFirFile(stubMode)
            val treeFromPsi = StringBuilder().also { FirRenderer(it).visitFile(firFileFromPsi) }.toString()

            return@compareBase treeFromLightTree == treeFromPsi
        }
    }

    private fun compare(
        stubMode: Boolean,
        visitAnonymousFunction: Boolean = false,
        visitLambdaExpression: Boolean = false,
        visitNamedFunction: Boolean = false,
        visitMemberDeclaration: Boolean = false,
        visitVariable: Boolean = false,
        visitAnnotation: Boolean = false,
        visitTypeOperatorCall: Boolean = false,
        visitArrayOfCall: Boolean = false,
        visitFunctionCall: Boolean = false,
        visitGetClassCall: Boolean = false,
        visitBreakExpression: Boolean = false,
        visitContinueExpression: Boolean = false,
        visitReturnExpression: Boolean = false,
        visitThrowExpression: Boolean = false,
        visitForLoop: Boolean = false,
        visitConstExpression: Boolean = false,
        visitQualifiedAccessExpression: Boolean = false,
        visitCallableReferenceAccess: Boolean = false,
        visitTryExpression: Boolean = false,
        visitWhenExpression: Boolean = false,
        visitLambdaArgumentExpression: Boolean = false,
        visitAnonymousObject: Boolean = false,
        visitDoWhileLoop: Boolean = false,
        visitWhileLoop: Boolean = false,
        visitAssignment: Boolean = false
    ) {
        val firVisitor = FirPartialTransformer(
            visitAnonymousFunction = visitAnonymousFunction,
            visitLambdaExpression = visitLambdaExpression,
            visitNamedFunction = visitNamedFunction,
            visitMemberDeclaration = visitMemberDeclaration,
            visitVariable = visitVariable,
            visitAnnotation = visitAnnotation,
            visitTypeOperatorCall = visitTypeOperatorCall,
            visitArrayOfCall = visitArrayOfCall,
            visitFunctionCall = visitFunctionCall,
            visitGetClassCall = visitGetClassCall,
            visitBreakExpression = visitBreakExpression,
            visitContinueExpression = visitContinueExpression,
            visitReturnExpression = visitReturnExpression,
            visitThrowExpression = visitThrowExpression,
            visitForLoop = visitForLoop,
            visitConstExpression = visitConstExpression,
            visitQualifiedAccessExpression = visitQualifiedAccessExpression,
            visitCallableReferenceAccess = visitCallableReferenceAccess,
            visitTryExpression = visitTryExpression,
            visitWhenExpression = visitWhenExpression,
            visitLambdaArgumentExpression = visitLambdaArgumentExpression,
            visitAnonymousObject = visitAnonymousObject,
            visitDoWhileLoop = visitDoWhileLoop,
            visitWhileLoop = visitWhileLoop,
            visitAssignment = visitAssignment
        )
        val lightTreeConverter = LightTree2Fir(stubMode, myProject)
        compareBase { file ->
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
            visitNamedFunction = true,
            visitMemberDeclaration = true,
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
            visitNamedFunction = true,
            visitMemberDeclaration = true,
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
            visitNamedFunction = true,
            visitMemberDeclaration = true,
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
            visitNamedFunction = true,
            visitMemberDeclaration = true,
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
}
