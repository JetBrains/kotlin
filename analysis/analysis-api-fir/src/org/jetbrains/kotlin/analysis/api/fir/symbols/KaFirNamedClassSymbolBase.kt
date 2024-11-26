/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirNestedInLocalClassFromCompilerPluginSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

/**
 * [KaFirNamedClassSymbolBase] provides shared equality and hash code implementations for FIR-based named class or object symbols so
 * that symbols of different kinds can be compared and remain interchangeable.
 */
internal sealed class KaFirNamedClassSymbolBase<P : PsiElement> : KaNamedClassSymbol(), KaFirPsiSymbol<P, FirRegularClassSymbol> {
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    /**
     * All kinds of non-local named class or object symbols must have the same kind of hash code. The class ID is the best option, as the
     * same class/object may be represented by multiple different symbols.
     */
    override fun hashCode(): Int = classId?.hashCode() ?: psiOrSymbolHashCode()

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Shared Operations
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override fun createPointer(): KaSymbolPointer<KaNamedClassSymbol> = withValidityAssertion {
        if (this is KaFirKtBasedSymbol<*, *>) {
            psiBasedSymbolPointerOfTypeIfSource<KaNamedClassSymbol>()?.let { return it }
        }

        when (val symbolKind = location) {
            KaSymbolLocation.LOCAL ->
                throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())

            KaSymbolLocation.CLASS -> {
                val classId = classId
                if (classId == null) {
                    checkWithAttachment(
                        origin == KaSymbolOrigin.PLUGIN,
                        { "A nested in local class without PSI should have the `KaSymbolOrigin.PLUGIN` origin but was $origin" }
                    ) {
                        withSymbolAttachment("symbol", analysisSession, this@KaFirNamedClassSymbolBase)
                    }
                    val container = with(analysisSession) { containingSymbol }
                    checkWithAttachment(
                        container is KaNamedClassSymbol,
                        { "Container should be `${KaNamedClassSymbol::class.simpleName}` but was `${container?.let { it::class }}`" }
                    ) {
                        withSymbolAttachment("symbol", analysisSession, this@KaFirNamedClassSymbolBase)
                    }

                    val firOrigin = firSymbol.fir.origin
                    checkWithAttachment(
                        firOrigin is FirDeclarationOrigin.Plugin,
                        { "Expected `FirDeclarationOrigin.Plugin` but was $firOrigin" }
                    ) {
                        withSymbolAttachment("symbol", analysisSession, this@KaFirNamedClassSymbolBase)
                    }
                    KaFirNestedInLocalClassFromCompilerPluginSymbolPointer(
                        container.createPointer(),
                        name,
                        firOrigin.key,
                        this
                    )
                } else {
                    KaFirClassLikeSymbolPointer(classId, KaNamedClassSymbol::class, this)
                }
            }
            KaSymbolLocation.TOP_LEVEL ->
                KaFirClassLikeSymbolPointer(classId!!, KaNamedClassSymbol::class, this)

            else -> throw KaUnsupportedSymbolLocation(this::class, symbolKind)
        }
    }
}
