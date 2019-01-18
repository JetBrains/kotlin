/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirErrorDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirAccess
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class RawFirBuilderTotalKotlinTestCase : AbstractRawFirBuilderTestCase() {

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
        println("BASE PATH: $testDataPath")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue
            try {
                val ktFile = createKtFile(file.toRelativeString(root))
                var firFile: FirFile? = null
                time += measureNanoTime {
                    firFile = ktFile.toFirFile(stubMode)
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

                    override fun visitAccess(access: FirAccess) {
                        val calleeReference = access.calleeReference
                        if (calleeReference is FirErrorNamedReference) {
                            errorReferences++
                            println(calleeReference.errorReason)
                        } else {
                            normalReferences++
                        }
                        super.visitAccess(access)
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
        if (!stubMode) {
            assertEquals(0, expressionStubs)
        }
        assertEquals(0, errorExpressions)
        assertEquals(0, errorDeclarations)
        assertEquals(0, errorReferences)
    }

    fun testTotalKotlinWithExpressionTrees() {
        testTotalKotlinWithGivenMode(stubMode = false)
    }

    fun testTotalKotlinWithDeclarationsOnly() {
        testTotalKotlinWithGivenMode(stubMode = true)
    }

    private fun testConsistency(checkConsistency: FirFile.() -> Unit) {
        val root = File(testDataPath)
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue
            val ktFile = createKtFile(file.toRelativeString(root))
            val firFile = ktFile.toFirFile(stubMode = false)
            try {
                firFile.checkConsistency()
            } catch (e: Throwable) {
                println("EXCEPTION in: " + file.toRelativeString(root))
                throw e
            }
        }
    }

    fun testVisitConsistency() {
        testConsistency { checkChildren() }
    }

    fun testTransformConsistency() {
        testConsistency { checkTransformedChildren() }
    }
}