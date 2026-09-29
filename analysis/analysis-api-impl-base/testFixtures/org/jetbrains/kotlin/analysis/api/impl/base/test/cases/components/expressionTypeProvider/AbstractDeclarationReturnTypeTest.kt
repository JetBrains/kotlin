/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.components.returnType
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.getNameWithPositionString
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractDeclarationReturnTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            buildString {
                contextFile.accept(object : KtTreeVisitor<Int>() {
                    override fun visitDeclaration(declaration: KtDeclaration, indent: Int): Void? {
                        if (declaration is KtTypeParameter) return null

                        // Enum entries are class-like declarations with a return type
                        if (declaration is KtClassLikeDeclaration && declaration !is KtEnumEntry) {
                            append(" ".repeat(indent))
                            appendLine(declaration.getNameWithPositionString())
                        } else if (declaration is KtDeclarationWithReturnType) {
                            val returnType = declaration.returnType.render(position = Variance.INVARIANT)
                            append(" ".repeat(indent))
                            append(declaration.getNameWithPositionString())
                            append(" : ")
                            append(returnType)

                            // Function type parameters have no symbol
                            val symbol = if (declaration is KtParameter && declaration.isFunctionTypeParameter) null else declaration.symbol
                            val symbolReturnType = (symbol as? KaCallableSymbol)?.returnType?.render(position = Variance.INVARIANT)
                            if (symbolReturnType != null && symbolReturnType != returnType) {
                                append(" (symbol: ")
                                append(symbolReturnType)
                                append(")")
                            }

                            appendLine()
                        }

                        return super.visitDeclaration(declaration, indent + 2)
                    }

                    // Function literals are not visited as declarations by default
                    override fun visitLambdaExpression(expression: KtLambdaExpression, indent: Int): Void? {
                        return visitDeclaration(expression.functionLiteral, indent)
                    }
                }, 0)
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
