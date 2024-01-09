/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderForSetterParameterTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val context = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtProperty>(mainFile)

        analyseForTest(context) { declaration ->
            val propertySymbol = (declaration as KtProperty).getVariableSymbol() as KtPropertySymbol
            val setterSymbol = propertySymbol.setter!!
            val setterParameterSymbol = setterSymbol.valueParameters.single()
            testServices.assertions.assertEquals(propertySymbol, setterSymbol.getContainingSymbol())
            testServices.assertions.assertEquals(setterSymbol, setterParameterSymbol.getContainingSymbol())
        }
    }
}