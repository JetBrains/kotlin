/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFirTypeAliasSymbol(
    override val firSymbol: FirTypeAliasSymbol,
    override val analysisSession: KaFirSession,
) : KaTypeAliasSymbol(), KaFirSymbol<FirTypeAliasSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val classId: ClassId? get() = withValidityAssertion { firSymbol.getClassId() }

    override val visibility: Visibility
        get() = withValidityAssertion {
            when (val possiblyRawVisibility = firSymbol.fir.visibility) {
                Visibilities.Unknown -> Visibilities.Public
                else -> possiblyRawVisibility
            }
        }

    override val typeParameters by cached { firSymbol.createKtTypeParameters(builder) }

    override val expandedType: KaType by cached { builder.typeBuilder.buildKtType(firSymbol.resolvedExpandedTypeRef) }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val location: KaSymbolLocation get() = withValidityAssertion { getSymbolKind() }

    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaTypeAliasSymbol>(this)?.let { return it }

        when (val symbolKind = location) {
            KaSymbolLocation.LOCAL ->
                throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())

            KaSymbolLocation.CLASS, KaSymbolLocation.TOP_LEVEL -> KaFirClassLikeSymbolPointer(classId!!, KaTypeAliasSymbol::class)
            else -> throw UnsupportedSymbolLocation(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
