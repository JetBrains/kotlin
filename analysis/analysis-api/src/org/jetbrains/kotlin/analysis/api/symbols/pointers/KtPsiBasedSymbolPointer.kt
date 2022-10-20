/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

public class KtPsiBasedSymbolPointer<S : KtSymbol> private constructor(private val psiPointer: SmartPsiElementPointer<out KtElement>) :
    KtSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        val psi = psiPointer.element ?: return null

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) {
            when (psi) {
                is KtDeclaration -> psi.getSymbol()
                is KtFile -> psi.getFileSymbol()
                else -> {
                    error("Unexpected declaration to restore: ${psi::class}, text:\n ${psi.text}")
                }
            }
        } as S?
    }

    public companion object {
        public fun <S : KtSymbol> createForSymbolFromSource(symbol: S): KtPsiBasedSymbolPointer<S>? {
            if (disablePsiPointer || symbol.origin != KtSymbolOrigin.SOURCE) return null

            val psi = when (val psi = symbol.psi) {
                is KtDeclaration -> psi
                is KtFile -> psi
                is KtObjectLiteralExpression -> psi.objectDeclaration
                else -> return null
            }

            return KtPsiBasedSymbolPointer(psi.createSmartPointer())
        }

        public fun <S : KtSymbol> createForSymbolFromPsi(ktElement: KtElement): KtPsiBasedSymbolPointer<S>? {
            if (disablePsiPointer) return null

            return KtPsiBasedSymbolPointer(ktElement.createSmartPointer())
        }

        @TestOnly
        @Synchronized
        public fun <T> withDisabledPsiBasedPointers(action: () -> T): T {
            disablePsiPointer = true
            return try {
                action()
            } finally {
                disablePsiPointer = false
            }
        }

        @Volatile
        private var disablePsiPointer: Boolean = false
    }
}
