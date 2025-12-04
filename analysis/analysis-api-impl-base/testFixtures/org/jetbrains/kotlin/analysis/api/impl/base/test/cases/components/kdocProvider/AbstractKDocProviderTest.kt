/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.kdocProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.findKDoc
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbols
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Reads the kdoc declarations provided in the source code and checks how they are rendered
 */
abstract class AbstractKDocProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = analyze(mainModule.ktModule) {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                buildString {
                    contextFile.accept(object : KtTreeVisitor<Int>() {
                        override fun visitCallExpression(expression: KtCallExpression, data: Int?): Void? {
                            expression.calleeExpression?.resolveToCall()?.calls?.forEach { call ->
                                call.symbols().forEach { symbol ->
                                    if (symbol !is KaDeclarationSymbol) return@forEach
                                    appendLine(symbol.renderKDoc())
                                    if (symbol is KaFunctionSymbol) {
                                        symbol.valueParameters.forEach { param ->
                                            appendLine(param.renderKDoc())
                                        }
                                    }
                                    appendLine()
                                }
                            }
                            return super.visitCallExpression(expression, data)
                        }

                        override fun visitDeclaration(declaration: KtDeclaration, indent: Int): Void? {
                            val symbol = declaration.symbol
                            appendLine(symbol.renderKDoc())
                            if (symbol is KaValueParameterSymbol) {
                                symbol.generatedPrimaryConstructorProperty?.let { property ->
                                    property.getter?.let { appendLine(it.renderKDoc()) }
                                    property.setter?.let { appendLine(it.renderKDoc()) }
                                }
                            } else if (symbol is KaPropertySymbol) {
                                symbol.getter?.let { appendLine(it.renderKDoc()) }
                                symbol.setter?.let { appendLine(it.renderKDoc()) }
                            }

                            appendLine()

                            return super.visitDeclaration(declaration, indent + 2)
                        }
                    }, 0)
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

@OptIn(KtNonPublicApi::class)
context(session: KaSession)
private fun KaDeclarationSymbol.renderKDoc(): String = buildString {
    val symbolStr = stringRepresentation(this@renderKDoc)
    appendLine("-".repeat(10) + symbolStr.padEnd(maxOf(70, symbolStr.length), '-'))
    append(stringRepresentation(this@renderKDoc.findKDoc()))
}
