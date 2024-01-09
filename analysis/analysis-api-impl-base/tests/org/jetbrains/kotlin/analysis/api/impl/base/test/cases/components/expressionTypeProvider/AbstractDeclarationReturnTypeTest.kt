/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.utils.getNameWithPositionString
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractDeclarationReturnTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val actual = buildString {
            mainFile.accept(object : KtTreeVisitor<Int>() {
                override fun visitDeclaration(declaration: KtDeclaration, indent: Int): Void? {
                    if (declaration is KtTypeParameter) return null
                    append(" ".repeat(indent))
                    if (declaration is KtClassLikeDeclaration) {
                        appendLine(declaration.getNameWithPositionString())
                    } else {
                        analyseForTest(declaration) {
                            val returnType = declaration.getReturnKtType()
                            append(declaration.getNameWithPositionString())
                            append(" : ")
                            appendLine(returnType.render(position = Variance.INVARIANT))
                        }
                    }
                    return super.visitDeclaration(declaration, indent + 2)
                }
            }, 0)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
