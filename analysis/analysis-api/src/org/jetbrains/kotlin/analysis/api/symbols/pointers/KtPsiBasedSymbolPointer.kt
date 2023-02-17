/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import kotlin.reflect.KClass

public class KtPsiBasedSymbolPointer<S : KtSymbol> private constructor(
    private val psiPointer: SmartPsiElementPointer<out KtElement>,
    private val expectedClass: KClass<S>,
) : KtSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        val psi = psiPointer.element ?: return null

        val symbol: KtSymbol = with(analysisSession) {
            if (!psi.canBeAnalysed()) return null
            when (psi) {
                is KtDeclaration -> psi.getSymbol()
                is KtFile -> psi.getFileSymbol()
                else -> {
                    error("Unexpected declaration to restore: ${psi::class}, text:\n ${psi.text}")
                }
            }
        }

        if (!expectedClass.isInstance(symbol)) return null

        @Suppress("UNCHECKED_CAST")
        return symbol as S
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtPsiBasedSymbolPointer &&
            other.expectedClass == expectedClass &&
            other.psiPointer == psiPointer

    public constructor(psi: KtElement, expectedClass: KClass<S>) : this(psi.createSmartPointer(), expectedClass)

    public companion object {
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        public inline fun <reified S : KtSymbol> createForSymbolFromSource(symbol: @kotlin.internal.NoInfer S): KtPsiBasedSymbolPointer<S>? {
            return createForSymbolFromSource(symbol, S::class)
        }

        public fun <S : KtSymbol> createForSymbolFromSource(symbol: S, expectedClass: KClass<S>): KtPsiBasedSymbolPointer<S>? {
            ifDisabled { return null }

            if (symbol.origin != KtSymbolOrigin.SOURCE) return null

            val psi = when (val psi = symbol.psi) {
                is KtDeclaration -> psi
                is KtFile -> psi
                is KtObjectLiteralExpression -> psi.objectDeclaration
                else -> return null
            }

            return KtPsiBasedSymbolPointer(psi, expectedClass)
        }


        public fun <S : KtSymbol> createForSymbolFromPsi(ktElement: KtElement, expectedClass: KClass<S>): KtPsiBasedSymbolPointer<S>? {
            ifDisabled { return null }

            return KtPsiBasedSymbolPointer(ktElement, expectedClass)
        }

        public inline fun <reified S : KtSymbol> createForSymbolFromPsi(ktElement: KtElement): KtPsiBasedSymbolPointer<S>? {
            return createForSymbolFromPsi(ktElement, S::class)
        }

        @TestOnly
        public fun <T> withDisabledPsiBasedPointers(disable: Boolean, action: () -> T): T = try {
            disablePsiPointer = true
            disablePsiPointerFlag.set(disable)
            action()
        } finally {
            disablePsiPointerFlag.remove()
        }

        private inline fun ifDisabled(action: () -> Unit) {
            if (!disablePsiPointer) return
            if (disablePsiPointerFlag.get()) {
                action()
            }
        }

        private val disablePsiPointerFlag: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        @Volatile
        private var disablePsiPointer: Boolean = false
    }
}

public fun KtElement.symbolPointer(): KtSymbolPointer<KtSymbol> = KtPsiBasedSymbolPointer(this, KtSymbol::class)
public inline fun <reified S : KtSymbol> KtElement.symbolPointerOfType(): KtSymbolPointer<S> = KtPsiBasedSymbolPointer(this, S::class)
