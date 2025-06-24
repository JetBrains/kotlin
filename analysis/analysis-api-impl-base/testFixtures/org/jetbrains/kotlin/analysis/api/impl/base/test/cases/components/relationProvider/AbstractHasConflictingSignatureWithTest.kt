/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractHasConflictingSignatureWithTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val currentDeclaration = testServices.expressionMarkerProvider
                    .getBottommostElementOfTypeAtCaret<KtFunction>(contextFile, "1")
                val otherDeclaration = testServices.expressionMarkerProvider
                    .getBottommostElementOfTypeAtCaret<KtFunction>(contextFile, "2")
                val currentSymbol = currentDeclaration.symbol as KaFunctionSymbol
                val otherSymbol = otherDeclaration.symbol as KaFunctionSymbol

                val hasConflictingSignature = currentSymbol.hasConflictingSignatureWith(otherSymbol, mainModule.ktModule.targetPlatform)

                buildString {
                    appendLine("ARE_CONFLICTING: $hasConflictingSignature")
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
