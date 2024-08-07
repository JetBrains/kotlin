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
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.asKaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
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
    override val psi: PsiElement? get() = withValidityAssertion { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val classId: ClassId? get() = withValidityAssertion { firSymbol.getClassId() }

    override val visibility: KaSymbolVisibility
        get() = withValidityAssertion {
            // TODO: We should use resolvedStatus, because it can be altered by status-transforming compiler plugins. See KT-58572
            when (val possiblyRawVisibility = firSymbol.fir.visibility) {
                Visibilities.Unknown -> KaSymbolVisibility.PUBLIC
                else -> possiblyRawVisibility.asKaSymbolVisibility
            }
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { firSymbol.visibility }

    override val typeParameters: List<KaTypeParameterSymbol> get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val expandedType: KaType get() = withValidityAssertion { builder.typeBuilder.buildKtType(firSymbol.resolvedExpandedTypeRef) }
    override val modality: KaSymbolModality get() = withValidityAssertion { firSymbol.kaSymbolModality }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val location: KaSymbolLocation get() = withValidityAssertion { getSymbolKind() }

    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaTypeAliasSymbol>(this)?.let { return it }

        when (val symbolKind = location) {
            KaSymbolLocation.LOCAL ->
                throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())

            KaSymbolLocation.CLASS, KaSymbolLocation.TOP_LEVEL -> KaFirClassLikeSymbolPointer(classId!!, KaTypeAliasSymbol::class)
            else -> throw KaUnsupportedSymbolLocation(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
