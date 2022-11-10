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

public fun KtFile.symbolPointer(): KtSymbolPointer<KtFileSymbol> = KtPsiBasedSymbolPointer(this, KtFileSymbol::class)
public fun KtParameter.symbolPointer(): KtSymbolPointer<KtVariableLikeSymbol> = KtPsiBasedSymbolPointer(this, KtVariableLikeSymbol::class)
public fun KtTypeAlias.symbolPointer(): KtSymbolPointer<KtTypeAliasSymbol> = KtPsiBasedSymbolPointer(this, KtTypeAliasSymbol::class)
public fun KtEnumEntry.symbolPointer(): KtSymbolPointer<KtEnumEntrySymbol> = KtPsiBasedSymbolPointer(this, KtEnumEntrySymbol::class)
public fun KtProperty.symbolPointer(): KtSymbolPointer<KtVariableSymbol> = KtPsiBasedSymbolPointer(this, KtVariableSymbol::class)

public fun KtNamedFunction.symbolPointer(): KtSymbolPointer<KtFunctionLikeSymbol> {
    return KtPsiBasedSymbolPointer(this, KtFunctionLikeSymbol::class)
}

public fun KtConstructor<*>.symbolPointer(): KtSymbolPointer<KtConstructorSymbol> {
    return KtPsiBasedSymbolPointer(this, KtConstructorSymbol::class)
}

public fun KtTypeParameter.symbolPointer(): KtSymbolPointer<KtTypeParameterSymbol> {
    return KtPsiBasedSymbolPointer(this, KtTypeParameterSymbol::class)
}

public fun KtFunctionLiteral.symbolPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol> {
    return KtPsiBasedSymbolPointer(this, KtAnonymousFunctionSymbol::class)
}

public fun KtObjectLiteralExpression.symbolPointer(): KtSymbolPointer<KtAnonymousObjectSymbol> {
    return KtPsiBasedSymbolPointer(this, KtAnonymousObjectSymbol::class)
}

public fun KtClassOrObject.symbolPointer(): KtSymbolPointer<KtClassOrObjectSymbol> {
    return KtPsiBasedSymbolPointer(this, KtClassOrObjectSymbol::class)
}

public fun KtPropertyAccessor.symbolPointer(): KtSymbolPointer<KtPropertyAccessorSymbol> {
    return KtPsiBasedSymbolPointer(this, KtPropertyAccessorSymbol::class)
}
