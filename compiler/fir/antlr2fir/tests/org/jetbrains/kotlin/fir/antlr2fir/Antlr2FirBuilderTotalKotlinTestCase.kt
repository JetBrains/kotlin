/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.antlr2fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirErrorDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import java.io.File
import kotlin.system.measureNanoTime

class Antlr2FirBuilderTotalKotlinTestCase : AbstractRawFirBuilderTestCase() {
    private fun testTotalKotlinWithGivenMode(stubMode: Boolean) {
        val root = File(testDataPath)
        var counter = 0
        var time = 0L
        var totalLength = 0
        var expressionStubs = 0
        var errorExpressions = 0
        var normalExpressions = 0
        var normalStatements = 0
        var errorDeclarations = 0
        var normalDeclarations = 0
        var errorReferences = 0
        var normalReferences = 0

        var ktExpressions = 0
        var ktDeclarations = 0
        var ktReferences = 0
        println("BASE PATH: $testDataPath")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue
            try {
                println(file.name)
                val ktFile = createKtFile(file.toRelativeString(root))
                var firFile: FirFile? = null
                time += measureNanoTime {
                    firFile = Antlr2Fir(stubMode).buildFirFile(file)
                }
                totalLength += StringBuilder().also { FirRenderer(it).visitFile(firFile!!) }.length
                counter++
                firFile?.accept(object : FirVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        element.acceptChildren(this)
                    }

                    override fun visitErrorExpression(errorExpression: FirErrorExpression) {
                        errorExpressions++
                        println(errorExpression.render())
                        errorExpression.psi?.let { println(it) }
                    }

                    override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess) {
                        val calleeReference = qualifiedAccess.calleeReference
                        if (calleeReference is FirErrorNamedReference) {
                            errorReferences++
                            println(calleeReference.errorReason)
                        } else {
                            normalReferences++
                        }
                        super.visitQualifiedAccess(qualifiedAccess)
                    }

                    override fun visitExpression(expression: FirExpression) {
                        when (expression) {
                            is FirExpressionStub -> {
                                expressionStubs++
                                if (!stubMode) {
                                    println(expression.psi?.text)
                                }
                            }
                            else -> normalExpressions++
                        }
                        expression.acceptChildren(this)
                    }

                    override fun visitStatement(statement: FirStatement) {
                        normalStatements++
                        statement.acceptChildren(this)
                    }

                    override fun visitErrorDeclaration(errorDeclaration: FirErrorDeclaration) {
                        errorDeclarations++
                        println(errorDeclaration.render())
                        errorDeclaration.psi?.let { println(it) }
                    }

                    override fun visitDeclaration(declaration: FirDeclaration) {
                        normalDeclarations++
                        declaration.acceptChildren(this)
                    }
                })
                ktFile.accept(object : KtTreeVisitor<Nothing?>() {
                    override fun visitReferenceExpression(expression: KtReferenceExpression, data: Nothing?): Void? {
                        ktReferences++
                        expression.acceptChildren(this)
                        return null
                    }

                    override fun visitExpression(expression: KtExpression, data: Nothing?): Void? {
                        ktExpressions++
                        expression.acceptChildren(this)
                        return null
                    }

                    override fun visitDeclaration(dcl: KtDeclaration, data: Nothing?): Void? {
                        ktDeclarations++
                        dcl.acceptChildren(this)
                        return null
                    }
                })

            } catch (e: Exception) {
                if (counter > 0) {
                    println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
                }
                println("EXCEPTION in: " + file.toRelativeString(root))
                throw e
            }
        }
        println("SUCCESS!")
        println("TOTAL LENGTH: $totalLength")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
        println("EXPRESSION STUBS: $expressionStubs")
        println("ERROR EXPRESSIONS: $errorExpressions")
        println("NORMAL EXPRESSIONS: $normalExpressions")
        println("NORMAL STATEMENTS: $normalStatements")
        println("ERROR DECLARATIONS: $errorDeclarations")
        println("NORMAL DECLARATIONS: $normalDeclarations")
        println("ERROR REFERENCES: $errorReferences")
        println("NORMAL REFERENCES: $normalReferences")
        println("KT EXPRESSIONS: $ktExpressions")
        println("KT DECLARATIONS: $ktDeclarations")
        println("KT REFERENCES: $ktReferences")
        if (!stubMode) {
            KtParsingTestCase.assertEquals(0, expressionStubs)
        }
        KtParsingTestCase.assertEquals(0, errorExpressions)
        KtParsingTestCase.assertEquals(0, errorDeclarations)
        KtParsingTestCase.assertEquals(0, errorReferences)
    }

    private fun performanceCompare(stubMode: Boolean) {
        val dirWithData = File("$testDataPath/compiler/fir/psi2fir/testData/rawBuilder/declarations")
        val root = File(testDataPath)
        var counter = 0
        var timeFir = 0L
        var timeAntlr = 0L
        var totalLength = 0

        println("BASE PATH: ${dirWithData.absolutePath}")
        for (file in dirWithData.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.extension != "kt") continue
            try {
                println(file.name)
                var firFile: FirFile? = null
                timeFir += measureNanoTime {
                    firFile = createKtFile(file.toRelativeString(root)).toFirFile(stubMode)
                }
                timeAntlr += measureNanoTime {
                    firFile = Antlr2Fir(stubMode).buildFirFile(file)
                }
                totalLength += StringBuilder().also { FirRenderer(it).visitFile(firFile!!) }.length
                counter++

            } catch (e: Exception) {
                if (counter > 0) {
                    println("ANTLR: TIME PER FILE: ${(timeAntlr / counter) * 1e-6} ms, COUNTER: $counter")
                    println("FIR: TIME PER FILE: ${(timeFir / counter) * 1e-6} ms, COUNTER: $counter")
                }
                println("EXCEPTION in: " + file.toRelativeString(root))
                throw e
            }
        }
        println("SUCCESS!")
        println("TOTAL LENGTH: $totalLength")
        println("ANTLR: TIME PER FILE: ${(timeAntlr / counter) * 1e-6} ms, COUNTER: $counter")
        println("FIR: TIME PER FILE: ${(timeFir / counter) * 1e-6} ms, COUNTER: $counter")
    }

    fun testTotalKotlinWithExpressionTrees() {
        testTotalKotlinWithGivenMode(stubMode = false)
    }

    fun testTotalKotlinWithDeclarationsOnly() {
        testTotalKotlinWithGivenMode(stubMode = true)
    }

    fun testPerformance() {
        performanceCompare(stubMode = true)
    }
}
