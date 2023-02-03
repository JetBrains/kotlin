/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirNamedClassOrObjectSymbol(
    override val firSymbol: FirRegularClassSymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtNamedClassOrObjectSymbol(), KtFirSymbol<FirRegularClassSymbol> {
    override val token: KtLifetimeToken get() = builder.token
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { firSymbol.getClassIdIfNonLocal() }

    /* FirRegularClass modality is not modified by STATUS, so it can be taken from RAW */
    override val modality: Modality
        get() = withValidityAssertion {
            firSymbol.fir.modality
                ?: when (classKind) { // default modality
                    KtClassKind.INTERFACE -> Modality.ABSTRACT
                    else -> Modality.FINAL
                }
        }


    /* FirRegularClass visibility is not modified by STATUS only for Unknown, so it can be taken from RAW */
    override val visibility: Visibility
        get() = withValidityAssertion {
            when (val possiblyRawVisibility = firSymbol.fir.visibility) {
                Visibilities.Unknown -> if (firSymbol.fir.isLocal) Visibilities.Local else Visibilities.Public
                else -> possiblyRawVisibility
            }
        }

    override val annotationsList by cached {
        KtFirAnnotationListForDeclaration.create(
            firSymbol,
            analysisSession.useSiteSession,
            token,
        )
    }

    override val isInner: Boolean get() = withValidityAssertion { firSymbol.isInner }
    override val isData: Boolean get() = withValidityAssertion { firSymbol.isData }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isFun: Boolean get() = withValidityAssertion { firSymbol.isFun }
    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isExternal }

    override val contextReceivers: List<KtContextReceiver> get() = withValidityAssertion { firSymbol.createContextReceivers(builder) }

    override val companionObject: KtFirNamedClassOrObjectSymbol? by cached {
        firSymbol.companionObjectSymbol?.let {
            builder.classifierBuilder.buildNamedClassOrObjectSymbol(it)
        }
    }

    override val superTypes: List<KtType> by cached {
        firSymbol.superTypesAndAnnotationsListForRegularClass(builder)
    }

    override val typeParameters = withValidityAssertion {
        firSymbol.fir.typeParameters.filterIsInstance<FirTypeParameter>().map { typeParameter ->
            builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
        }
    }


    @OptIn(KtAnalysisApiInternals::class)
    override val classKind: KtClassKind
        get() = withValidityAssertion {
            firSymbol.classKind.toKtClassKind(isCompanionObject = firSymbol.isCompanion)
        }

    override val symbolKind: KtSymbolKind get() = withValidityAssertion { getSymbolKind() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtNamedClassOrObjectSymbol>(this)?.let { return it }

        when (val symbolKind = symbolKind) {
            KtSymbolKind.LOCAL ->
                throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classIdIfNonLocal?.asString() ?: name.asString())

            KtSymbolKind.CLASS_MEMBER, KtSymbolKind.TOP_LEVEL ->
                KtFirClassLikeSymbolPointer(classIdIfNonLocal!!, KtNamedClassOrObjectSymbol::class)

            else -> throw UnsupportedSymbolKind(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
