/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.multiplatformInfoProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpectForActualTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val (declaration, _) = moduleStructure.modules.flatMap { module ->
            val ktFiles = testServices.ktModuleProvider.getModuleFiles(module).filterIsInstance<KtFile>()
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtDeclaration>(ktFiles)
        }.single()

        val expectedSymbolText: String? = executeOnPooledThreadInReadAction {
            analyseForTest(declaration) {
                val expectedSymbols = declaration.getSymbol().getExpectsForActual()
                expectedSymbols.joinToString(separator = "\n") { expectedSymbol ->
                    expectedSymbol.psi?.containingFile?.name + " : " + expectedSymbol.render()
                }
            }
        }

        val actual = buildString {
            appendLine("expected symbols:")
            appendLine(expectedSymbolText)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}