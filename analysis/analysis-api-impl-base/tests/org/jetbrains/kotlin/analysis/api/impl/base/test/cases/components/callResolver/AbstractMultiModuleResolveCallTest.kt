/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractMultiModuleResolveCallTest : AbstractAnalysisApiBasedTest() {
    final override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val (expression, _) =
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtExpression>(moduleStructure, testServices)
                .single()

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                expression.resolveCall()?.let { stringRepresentation(it) }
            }
        } ?: "null"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
