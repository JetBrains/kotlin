/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirTypeAliasSymbol(
    override val firSymbol: FirTypeAliasSymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtTypeAliasSymbol(), KtFirSymbol<FirTypeAliasSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val classIdIfNonLocal: ClassId? get() = withValidityAssertion { firSymbol.getClassIdIfNonLocal() }

    override val visibility: Visibility
        get() = withValidityAssertion {
            when (val possiblyRawVisibility = firSymbol.fir.visibility) {
                Visibilities.Unknown -> Visibilities.Public
                else -> possiblyRawVisibility
            }
        }

    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }

    override val expandedType: KtType by cached { builder.typeBuilder.buildKtType(firSymbol.resolvedExpandedTypeRef) }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token)
    }

    override val symbolKind: KtSymbolKind get() = withValidityAssertion { getSymbolKind() }

    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtTypeAliasSymbol>(this)?.let { return it }

        when (val symbolKind = symbolKind) {
            KtSymbolKind.LOCAL ->
                throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classIdIfNonLocal?.asString() ?: name.asString())

            KtSymbolKind.CLASS_MEMBER, KtSymbolKind.TOP_LEVEL -> KtFirClassLikeSymbolPointer(classIdIfNonLocal!!, KtTypeAliasSymbol::class)
            else -> throw UnsupportedSymbolKind(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
