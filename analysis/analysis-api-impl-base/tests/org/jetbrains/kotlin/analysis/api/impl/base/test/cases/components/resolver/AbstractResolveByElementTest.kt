/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveByElementTest : AbstractResolveTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtExpression>(mainFile)
            ?: testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(mainFile)

        doResolveTest(element.elementToResolve, testServices)
    }

    protected open fun doResolveTest(element: KtElement, testServices: TestServices) {
        analyseForTest(element) {
            processElement(
                element = element,
                testServices = testServices,
                renderedSymbol = { testServices.assertions.assertEqualsToTestDataFileSibling(it, "symbol.txt") },
                renderedCall = { testServices.assertions.assertEqualsToTestDataFileSibling(it, "call.txt") },
                renderedCandidates = { testServices.assertions.assertEqualsToTestDataFileSibling(it, "callCandidates.txt") },
            )
        }
    }
}
