/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class RawFirBuilderTotalKotlinTestCase : AbstractRawFirBuilderTestCase() {

    fun testTotalKotlinWithExpressionTrees() {
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
            val path = file.path.lowercase()
            if ("testdata" in path ||
                "kotlin-native" in path ||
                "resources" in path ||
                "api/js" in path.replace('\\', '/')
            ) continue
            if (file.extension != "kt") continue
            try {
                val ktFile = createKtFile(file.toRelativeString(root))
                val firFile: FirFile
                time += measureNanoTime {
                    firFile = ktFile.toFirFile()
                }
                totalLength += StringBuilder().also { FirRenderer(it).visitFile(firFile) }.length
                counter++
                firFile.accept(object : FirVisitorVoid() {
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
                            println(calleeReference.diagnostic.reason)
                        } else {
                            normalReferences++
                        }
                        visitStatement(qualifiedAccess)
                    }

                    override fun visitExpression(expression: FirExpression) {
                        when (expression) {
                            is FirExpressionStub -> {
                                expressionStubs++
                                println(expression.psi?.text)
                            }
                            else -> normalExpressions++
                        }
                        expression.acceptChildren(this)
                    }

                    override fun visitStatement(statement: FirStatement) {
                        normalStatements++
                        statement.acceptChildren(this)
                    }

//                    override fun visitErrorDeclaration(errorDeclaration: FirErrorDeclaration) {
//                        errorDeclarations++
//                        println(errorDeclaration.render())
//                        errorDeclaration.psi?.let { println(it) }
//                    }

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
        assertEquals("# of expression stubs", 0, expressionStubs)
        assertEquals("# of error expressions", 0, errorExpressions)
        assertEquals("# of error declarations", 0, errorDeclarations)
        assertEquals("# of error references", 0, errorReferences)
    }

    private fun testConsistency(checkConsistency: FirFile.() -> Unit) {
        val root = File(testDataPath)
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            val path = file.path.lowercase()
            if ("kotlin-native" in path ||
                "testdata" in path ||
                "resources" in path ||
                "api/js" in path.replace('\\', '/')
            ) continue
            if (file.extension != "kt") continue
            val ktFile = createKtFile(file.toRelativeString(root))
            val firFile = ktFile.toFirFile()
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

    fun testPsiConsistency() {
        val root = File(testDataPath)
        var counter = 0
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            val path = file.path.lowercase()
            if ("kotlin-native" in path ||
                "testdata" in path ||
                "resources" in path ||
                "api/js" in path.replace('\\', '/')) continue
            if (file.extension != "kt") continue
            val ktFile = createKtFile(file.toRelativeString(root))
            val firFile: FirFile = ktFile.toFirFile()
            val psiSetViaFir = mutableSetOf<KtElement>()
            val psiSetDirect = mutableSetOf<KtElement>()
            firFile.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    val psi = element.psi as? KtElement
                    if (psi != null) {
                        psiSetViaFir += psi
                    }
                    element.acceptChildren(this)
                }
            })
            ktFile.accept(object : KtTreeVisitor<Nothing?>() {
                override fun visitKtElement(element: KtElement, data: Nothing?): Void? {
                    psiSetDirect += element
                    return super.visitKtElement(element, data)
                }
            })
            psiSetDirect -= psiSetViaFir
            psiSetDirect.removeIf(::isKnownToBeNotTraversedByFirTree)
            if (psiSetDirect.isNotEmpty()) {
                println("Total of $counter files processed successfully")
                println("FILE ${file.toRelativeString(root)} has not traversed PSI elements (total of ${psiSetDirect.size})!")
                for (element in psiSetDirect) {
                    println("Not traversed ${element.javaClass}: ${element.text}")
                    val traversedParent = element.parents.firstOrNull { it in psiSetViaFir }
                    if (traversedParent != null) {
                        println("(traversed parent: ${traversedParent.javaClass} ${traversedParent.text})")
                    }
                }
                println(firFile.render())
                throw AssertionError()
            }
            counter++
        }
    }

    private fun isKnownToBeNotTraversedByFirTree(it: KtElement): Boolean {
        return it is KtPackageDirective || it is KtImportList || it is KtClassBody ||
                it is KtModifierList ||
                it is KtUserType || it is KtNullableType || it is KtFunctionType || it is KtFunctionTypeReceiver ||
                it is KtIntersectionType || it is KtDynamicType ||
                it is KtQualifiedExpression ||
                it is KtPropertyDelegate ||
                it is KtConstructorCalleeExpression && (it.parent is KtAnnotationEntry || it.parent is KtSuperTypeCallEntry) ||
                it is KtValueArgumentList || it is KtParameterList || it is KtTypeParameterList || it is KtTypeArgumentList ||
                it is KtTypeReference && it.parent.parent.parent is KtCallExpression ||
                it is KtSuperTypeList || (it is KtSuperTypeListEntry && it !is KtSuperTypeCallEntry) ||
                it is KtValueArgument || it is KtLambdaArgument || it is KtValueArgumentName ||
                it is KtContainerNodeForControlStructureBody || it is KtContainerNode ||
                it is KtStringTemplateEntry ||
                it is KtOperationReferenceExpression ||
                it is KtLabelReferenceExpression ||
                it is KtConstructorDelegationReferenceExpression ||
                it is KtParenthesizedExpression ||
                it is KtLabeledExpression ||
                it is KtAnnotatedExpression ||
                it is KtWhenConditionWithExpression ||
                it is KtFinallySection ||
                it is KtObjectLiteralExpression ||// TODO: KT-24089 (support of dynamic)
                // NB: KtAnnotation is processed via its KtAnnotationEntries
                it is KtFileAnnotationList || it is KtAnnotationUseSiteTarget || it is KtAnnotation ||
                it is KtInitializerList || it is KtEnumEntrySuperclassReferenceExpression ||
                it is KtLambdaExpression ||
                it is KtTypeConstraintList ||
                it is KtTypeConstraint ||
                it is KtStringTemplateExpression && it.entries.size <= 1 ||
                it is KtDestructuringDeclaration && it.parent is KtParameter ||
                it is KtArrayAccessExpression && it.parent is KtBinaryExpression ||
                it is KtCallExpression && it.parent is KtQualifiedExpression ||
                it is KtNameReferenceExpression &&
                (it.parent is KtUserType || it.parent is KtInstanceExpressionWithLabel ||
                        it.parent is KtValueArgumentName || it.parent is KtTypeConstraint) ||
                it.getStrictParentOfType<KtPackageDirective>() != null ||
                it.getStrictParentOfType<KtImportDirective>() != null ||
                (it is KtPropertyAccessor && !it.hasBody()) ||
                it is KtDestructuringDeclarationEntry && it.text == "_" ||
                it is KtIfExpression && it.parent is KtContainerNodeForControlStructureBody && it.parent.parent is KtIfExpression ||
                it is KtContextReceiverList ||
                it is KtConstantExpression && it.parent.let { parent ->
            parent is KtPrefixExpression && (parent.operationToken == KtTokens.MINUS || parent.operationToken == KtTokens.PLUS)
        }
    }
}
