/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * A [KaTypeRelationChecker][org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker] test which checks its expected output with
 * a [resultDirective] boolean value.
 */
abstract class AbstractTypeRelationTest : AbstractAnalysisApiBasedTest() {
    protected abstract val resultDirective: StringDirective

    protected abstract fun KaSession.checkExpectedResult(expectedResult: Boolean, mainFile: KtFile, testServices: TestServices)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expectedResult = testServices.moduleStructure.allDirectives
            .singleValue(resultDirective)
            .toBooleanStrict()

        analyseForTest(mainFile) {
            checkExpectedResult(expectedResult, mainFile, testServices)
        }
    }

    protected fun KaSession.getTypeAtMarker(mainFile: KtFile, testServices: TestServices, caretTag: String? = null): KaType {
        val element = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtElement>(mainFile, caretTag)
            ?: testServices.expressionMarkerProvider.getSelectedElements(mainFile).singleOrNull()

        return when (element) {
            is KtProperty -> element.symbol.returnType
            is KtExpression -> element.expressionType ?: error("Expected the selected expression to have a type.")
            else -> error("Expected a property or an expression.")
        }
    }
}
