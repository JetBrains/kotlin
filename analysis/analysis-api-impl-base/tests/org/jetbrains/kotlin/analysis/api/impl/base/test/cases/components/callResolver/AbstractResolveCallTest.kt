/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCallTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(mainFile)

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                resolveCall(expression)?.let { stringRepresentation(it) }
            }
        } ?: "null"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KtAnalysisSession.resolveCall(element: KtElement): KtCallInfo? = when (element) {
        is KtValueArgument -> element.getArgumentExpression()?.resolveCall()
        is KtDeclarationModifierList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.resolveCall()
        }
        is KtFileAnnotationList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.resolveCall()
        }
        else -> element.resolveCall()
    }

}
