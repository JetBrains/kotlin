/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.utils.getNameWithPositionString
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReturnTargetSymbolTest : AbstractAnalysisApiSingleFileTest() {
    val commentRegex = Regex("""/\* (.+@\(.+\)|null) \*/""")
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val original = ktFile.text
        val actual = buildString {
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (element is LeafPsiElement) {
                        append(element.text)
                    }
                    super.visitElement(element)
                }

                override fun visitReturnExpression(expression: KtReturnExpression) {
                    expression.returnKeyword.accept(this)
                    expression.labeledExpression?.accept(this)
                    analyseForTest(expression) {
                        val target = expression.getReturnTargetSymbol()
                        append("/* " + target?.getNameWithPositionString() + " */")
                    }
                    expression.returnedExpression?.accept(this)
                }

                override fun visitComment(comment: PsiComment) {
                    // Skip such comments so that test become idempotent
                    if (comment.text.matches(commentRegex)) return
                    super.visitComment(comment)
                }
            })
        }
        if (actual != original) {
            testServices.assertions.assertEqualsToFile(testDataPath, actual)
        }
    }
}