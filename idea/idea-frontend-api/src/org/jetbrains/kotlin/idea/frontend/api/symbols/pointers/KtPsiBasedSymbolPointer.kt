/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.pointers

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class KtPsiBasedSymbolPointer<S : KtSymbol>(private val psiPointer: SmartPsiElementPointer<out KtDeclaration>) : KtSymbolPointer<S>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        val psi = psiPointer.element ?: return null

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) { psi.getSymbol() } as S?
    }

    companion object {
        fun <S : KtSymbol> createForSymbolFromSource(symbol: S): KtPsiBasedSymbolPointer<S>? {
            if (symbol.origin == KtSymbolOrigin.LIBRARY) return null
            val psi = when(val psi = symbol.psi) {
                is KtDeclaration -> psi
                is KtObjectLiteralExpression -> psi.objectDeclaration
                else -> null
            } ?: return null
            return KtPsiBasedSymbolPointer(psi.createSmartPointer())
        }
    }
}