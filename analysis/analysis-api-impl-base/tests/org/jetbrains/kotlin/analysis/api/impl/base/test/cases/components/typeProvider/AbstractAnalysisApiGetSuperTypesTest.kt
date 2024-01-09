/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForDebug
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiGetSuperTypesTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(mainFile)
        expression as? KtExpression ?: error("unexpected expression kind ${expression::class}")

        val actual = executeOnPooledThreadInReadAction {
            analyze(expression) {
                val expectedType = expression.getKtType() ?: error("expect to get type of expression '${expression.text}'")
                val directSuperTypes = expectedType.getDirectSuperTypes()
                val approximatedDirectSuperTypes = expectedType.getDirectSuperTypes(shouldApproximate = true)
                val allSuperTypes = expectedType.getAllSuperTypes()
                val approximatedAllSuperTypes = expectedType.getAllSuperTypes(shouldApproximate = true)

                buildString {
                    fun List<KtType>.print(name: String) {
                        appendLine(name)
                        for (type in this) {
                            appendLine(type.render(KtTypeRendererForDebug.WITH_QUALIFIED_NAMES, position = Variance.INVARIANT))
                        }
                        appendLine()
                    }
                    directSuperTypes.print("[direct super types]")
                    approximatedDirectSuperTypes.print("[approximated direct super types]")
                    allSuperTypes.print("[all super types]")
                    approximatedAllSuperTypes.print("[approximated all super types]")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}