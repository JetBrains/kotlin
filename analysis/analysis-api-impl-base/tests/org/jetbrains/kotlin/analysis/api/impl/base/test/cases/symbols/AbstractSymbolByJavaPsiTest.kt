package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByJavaPsiTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val referenceExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtReferenceExpression>(ktFile)
        val symbolByReference = referenceExpression.mainReference.resolveToSymbol() ?: error("Failed to resolve reference")

        val symbolByJavaPsi = when (val javaPsi = symbolByReference.psi) {
            is PsiClass -> javaPsi.getNamedClassSymbol()
            is PsiMember -> javaPsi.getCallableSymbol()
            null -> error("Failed to find psi for symbol")
            else -> unexpectedElementError<PsiElement>(javaPsi)
        }

        return SymbolsData(listOfNotNull(symbolByJavaPsi))
    }
}