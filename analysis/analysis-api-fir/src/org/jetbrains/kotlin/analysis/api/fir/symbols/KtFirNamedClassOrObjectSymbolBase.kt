/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * [KtFirNamedClassOrObjectSymbolBase] provides shared equality and hash code implementations for FIR-based named class or object symbols so
 * that symbols of different kinds can be compared and remain interchangeable.
 */
internal sealed class KtFirNamedClassOrObjectSymbolBase : KtNamedClassOrObjectSymbol(), KtFirSymbol<FirRegularClassSymbol> {
    /**
     * Whether [firSymbol] is computed lazily. Equality will fall back to PSI-equality if one of the symbols is lazy and both classes have
     * an associated [PsiClass].
     */
    open val hasLazyFirSymbol: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is KtFirNamedClassOrObjectSymbolBase) return false

        if (hasLazyFirSymbol || other.hasLazyFirSymbol) {
            val psiClass = psi as? PsiClass ?: return symbolEquals(other)
            val otherPsiClass = other.psi as? PsiClass ?: return symbolEquals(other)
            return PsiEquivalenceUtil.areElementsEquivalent(psiClass, otherPsiClass)
        }
        return symbolEquals(other)
    }

    /**
     * All kinds of non-local named class or object symbols must have the same kind of hash code. The class ID is the best option, as the
     * same class/object may be represented by multiple different symbols.
     */
    override fun hashCode(): Int = classIdIfNonLocal?.hashCode() ?: symbolHashCode()

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Shared Operations
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override val superTypes: List<KtType> by cached {
        firSymbol.superTypesList(builder)
    }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtNamedClassOrObjectSymbol>(this)?.let { return it }

        return when (val symbolKind = symbolKind) {
            KtSymbolKind.LOCAL ->
                throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classIdIfNonLocal?.asString() ?: name.asString())

            KtSymbolKind.CLASS_MEMBER, KtSymbolKind.TOP_LEVEL ->
                KtFirClassLikeSymbolPointer(classIdIfNonLocal!!, KtNamedClassOrObjectSymbol::class)

            else -> throw UnsupportedSymbolKind(this::class, symbolKind)
        }
    }
}
