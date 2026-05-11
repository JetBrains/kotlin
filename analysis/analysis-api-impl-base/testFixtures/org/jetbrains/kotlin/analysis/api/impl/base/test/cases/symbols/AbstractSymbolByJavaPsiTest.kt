/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.util.unexpectedElementError
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByJavaPsiTest : AbstractSymbolTest() {
    @OptIn(KtExperimentalApi::class)
    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val referenceExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtReferenceExpression>(ktFile)
        val symbolByReference = referenceExpression.resolveSymbolPreferringCall() ?: error("Failed to resolve expression")

        val javaPsi = when (symbolByReference) {
            is KaSyntheticJavaPropertySymbol -> symbolByReference.setter?.psi ?: symbolByReference.getter.psi
            else -> symbolByReference.psi
        }

        val symbolByJavaPsi = when (javaPsi) {
            is PsiClass -> javaPsi.namedClassSymbol
            is PsiMember -> javaPsi.callableSymbol
            null -> error("Failed to find psi for symbol")
            else -> unexpectedElementError<PsiElement>(javaPsi)
        }

        return SymbolsData(listOfNotNull(symbolByJavaPsi))
    }
}
