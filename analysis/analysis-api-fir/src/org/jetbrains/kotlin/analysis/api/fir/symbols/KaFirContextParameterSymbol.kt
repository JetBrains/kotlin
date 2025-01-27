/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.parameterName
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseContextParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseUnrestorableSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaContextParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter

internal class KaFirContextParameterSymbol private constructor(
    override val backingPsi: KtParameter?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirValueParameterSymbol>,
) : KaContextParameterSymbol(), KaFirKtBasedSymbol<KtParameter, FirValueParameterSymbol> {
    constructor(declaration: KtParameter, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirValueParameterSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtParameter,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.parameterName ?: firSymbol.name }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaContextParameterSymbol>()?.let { return it }

        val ownerSymbol = with(analysisSession) { containingDeclaration }
            ?: error("Containing declaration is expected for a context parameter symbol")

        // Some non-relevant declarations still might have context parameters due to transition from
        // context receivers, so they shouldn't be restored in this case by a non-psi pointer
        if (ownerSymbol !is KaContextParameterOwnerSymbol) {
            return KaBaseUnrestorableSymbolPointer()
        }

        return KaBaseContextParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = (ownerSymbol.firSymbol.fir as FirCallableDeclaration).contextParameters.indexOf(firSymbol.fir),
            originalSymbol = this,
        )
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
