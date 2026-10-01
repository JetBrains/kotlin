/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithoutTestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
@ObsoleteTestInfrastructure
class RawFirBuilderTotalKotlinTestCase : AbstractRawFirBuilderTestCase() {

    @Test
    fun testTotalKotlinWithExpressionTrees() {
        // Back from /compiler/fir/raw-fir/<module>
        val path = "$testDataPath/../../../.."
        val root = File(path)
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
        println("BASE PATH: ${root.normalize().absolutePath}")
        path.walkRepositoryKotlinFilesWithoutTestData { file ->
            try {
                val ktFile = createKtFile(file.absolutePath)
                val firFile: FirFile
                time += measureNanoTime {
                    firFile = ktFile.toFirFile()
                }
                totalLength += FirRenderer().renderElementAsString(firFile).length
                counter++
                firFile.accept(object : FirVisitor<Unit, FirElement>() {
                    override fun visitElement(element: FirElement, data: FirElement) {
                        element.acceptChildren(this, element)
                    }

                    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: FirElement) {
                        errorExpressions++
                        println(errorExpression.render())
                        errorExpression.psi?.let { println(it) }
                    }

                    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: FirElement) {
                        val calleeReference = qualifiedAccessExpression.calleeReference
                        if (calleeReference.isError()) {
                            errorReferences++
                            println((calleeReference as FirDiagnosticHolder).diagnostic.reason)
                        } else {
                            normalReferences++
                        }
                        visitStatement(qualifiedAccessExpression, data)
                    }

                    override fun visitExpression(expression: FirExpression, data: FirElement) {
                        when (expression) {
                            is FirExpressionStub -> {
                                if (data !is FirProperty || data.isCatchParameter != true) {
                                    expressionStubs++
                                    println(expression.psi?.text)
                                }
                            }
                            else -> normalExpressions++
                        }
                        expression.acceptChildren(this, expression)
                    }

                    override fun visitStatement(statement: FirStatement, data: FirElement) {
                        normalStatements++
                        statement.acceptChildren(this, statement)
                    }

//                    override fun visitErrorDeclaration(errorDeclaration: FirErrorDeclaration) {
//                        errorDeclarations++
//                        println(errorDeclaration.render())
//                        errorDeclaration.psi?.let { println(it) }
//                    }

                    override fun visitDeclaration(declaration: FirDeclaration, data: FirElement) {
                        normalDeclarations++
                        declaration.acceptChildren(this, declaration)
                    }
                }, firFile)
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
        assertEquals(0, expressionStubs) { "# of expression stubs" }
        assertEquals(0, errorExpressions) { "# of error expressions" }
        assertEquals(0, errorDeclarations) { "# of error declarations" }
        assertEquals(0, errorReferences) { "# of error references" }
    }

    private fun testConsistency(checkConsistency: FirFile.() -> Unit) {
        // Back from /compiler/fir/raw-fir/<module>
        val path = "$testDataPath/../../../.."
        val root = File(path)
        path.walkRepositoryKotlinFilesWithoutTestData { file ->
            val ktFile = createKtFile(file.absolutePath)
            val firFile = ktFile.toFirFile()
            try {
                firFile.checkConsistency()
            } catch (e: Throwable) {
                println("EXCEPTION in: " + file.toRelativeString(root))
                throw e
            }
        }
    }

    @Test
    fun testVisitConsistency() {
        testConsistency { checkChildren() }
    }

    @Test
    fun testTransformConsistency() {
        testConsistency { checkTransformedChildren() }
    }
}
