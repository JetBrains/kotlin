/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirConstructorSymbol(
    override val firSymbol: FirConstructorSymbol,
    override val firResolveSession: LLFirResolveSession,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorSymbol(), KtFirSymbol<FirConstructorSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    
    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val valueParameters: List<KtValueParameterSymbol> by cached { firSymbol.createKtValueParameters(builder) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.getHasStableParameterNames(firSymbol.moduleData.session)
        }

    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, firResolveSession.useSiteFirSession, token) }

    override val containingClassIdIfNonLocal: ClassId?
        get() = withValidityAssertion { firSymbol.containingClassLookupTag()?.classId?.takeUnless { it.isLocal } }

    override val isPrimary: Boolean get() = withValidityAssertion { firSymbol.isPrimary }

    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }


    override fun createPointer(): KtSymbolPointer<KtConstructorSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        if (symbolKind == KtSymbolKind.LOCAL) {
            throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException("constructor")
        }

        KtFirConstructorSymbolPointer(requireOwnerPointer(), isPrimary, firSymbol.fir.createSignature())
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
