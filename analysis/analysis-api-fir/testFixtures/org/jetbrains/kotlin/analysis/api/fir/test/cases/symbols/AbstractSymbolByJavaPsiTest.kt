/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByJavaPsiTest : AbstractSymbolTest() {
    context(_: KaSession)
    override fun collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val referenceExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtReferenceExpression>(ktFile)
        val symbolByReference = referenceExpression.resolveSymbolPreferringCall() ?: error("Failed to resolve expression")
        return SymbolsData(listOfNotNull(symbolByReference.symbolByJavaPsi))
    }
}
