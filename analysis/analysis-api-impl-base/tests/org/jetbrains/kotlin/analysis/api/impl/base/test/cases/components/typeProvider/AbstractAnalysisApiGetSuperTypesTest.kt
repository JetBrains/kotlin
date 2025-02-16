/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForDebug
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiGetSuperTypesTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getTopmostSelectedElement(mainFile)
        expression as? KtExpression ?: error("unexpected expression kind ${expression::class}")

        val actual = executeOnPooledThreadInReadAction {
            analyze(expression) {
                val expectedType = expression.expressionType ?: error("expect to get type of expression '${expression.text}'")
                val directSuperTypes = expectedType.directSupertypes.toList()
                val approximatedDirectSuperTypes = expectedType.directSupertypes(shouldApproximate = true).toList()
                val allSuperTypes = expectedType.allSupertypes.toList()
                val approximatedAllSuperTypes = expectedType.allSupertypes(shouldApproximate = true).toList()

                buildString {
                    fun List<KaType>.print(name: String) {
                        appendLine(name)
                        for (type in this) {
                            appendLine(type.render(KaTypeRendererForDebug.WITH_QUALIFIED_NAMES, position = Variance.INVARIANT))
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