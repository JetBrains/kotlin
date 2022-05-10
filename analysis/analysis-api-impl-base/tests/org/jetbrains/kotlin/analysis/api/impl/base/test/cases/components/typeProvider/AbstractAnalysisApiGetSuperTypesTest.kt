/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiGetSuperTypesTest : AbstractAnalysisApiSingleFileTest(){
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile)

        val actual = executeOnPooledThreadInReadAction {
            analyze(expression) {
                val expectedType = expression.getExpectedType() ?: error("expect to get type of expression '${expression.text}'")
                val directSuperTypes = expectedType.getDirectSuperTypes()
                val approximatedDirectSuperTypes = expectedType.getDirectSuperTypes(shouldApproximate = true)
                val allSuperTypes = expectedType.getAllSuperTypes()
                val approximatedAllSuperTypes = expectedType.getAllSuperTypes(shouldApproximate = true)

                buildString {
                    fun List<KtType>.print(name: String) {
                        appendLine(name)
                        for (type in this) {
                            appendLine(type.render(KtTypeRendererOptions.DEFAULT))
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