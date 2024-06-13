/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * [KaFirNamedClassOrObjectSymbolBase] provides shared equality and hash code implementations for FIR-based named class or object symbols so
 * that symbols of different kinds can be compared and remain interchangeable.
 */
internal sealed class KaFirNamedClassOrObjectSymbolBase : KaNamedClassOrObjectSymbol(), KaFirSymbol<FirRegularClassSymbol> {
    /**
     * Whether [firSymbol] is computed lazily. Equality will fall back to PSI-equality if one of the symbols is lazy and both classes have
     * an associated [PsiClass].
     */
    open val hasLazyFirSymbol: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is KaFirNamedClassOrObjectSymbolBase) return false

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
    override fun hashCode(): Int = classId?.hashCode() ?: symbolHashCode()

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Shared Operations
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override val superTypes: List<KaType> by cached {
        firSymbol.superTypesList(builder)
    }

    override fun createPointer(): KaSymbolPointer<KaNamedClassOrObjectSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaNamedClassOrObjectSymbol>(this)?.let { return it }

        return when (val symbolKind = symbolKind) {
            KaSymbolKind.LOCAL ->
                throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())

            KaSymbolKind.CLASS_MEMBER, KaSymbolKind.TOP_LEVEL ->
                KaFirClassLikeSymbolPointer(classId!!, KaNamedClassOrObjectSymbol::class)

            else -> throw UnsupportedSymbolKind(this::class, symbolKind)
        }
    }
}
