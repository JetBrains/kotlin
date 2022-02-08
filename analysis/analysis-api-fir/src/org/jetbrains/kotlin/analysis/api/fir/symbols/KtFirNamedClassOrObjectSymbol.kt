/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirClassOrObjectInLibrarySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.descriptors.ClassKind
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
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtNamedClassOrObjectSymbol(), KtFirSymbol<FirRegularClassSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { firSymbol.classId.takeUnless { it.isLocal } }

    /* FirRegularClass modality is not modified by STATUS, so it can be taken from RAW */
    override val modality: Modality
        get() = withValidityAssertion {
            firSymbol.fir.modality
                ?: when (classKind) { // default modality
                    KtClassKind.INTERFACE -> Modality.ABSTRACT
                    // Enum class should not be `final`, since its entries extend it.
                    // It could be either `abstract` w/o ctor, or empty modality w/ private ctor.
                    KtClassKind.ENUM_CLASS -> Modality.OPEN
                    else -> Modality.FINAL
                }
        }


    /* FirRegularClass visibility is not modified by STATUS only for Unknown, so it can be taken from RAW */
    override val visibility: Visibility
        get() = when (val possiblyRawVisibility = firSymbol.fir.visibility) {
            Visibilities.Unknown -> if (firSymbol.fir.isLocal) Visibilities.Local else Visibilities.Public
            else -> possiblyRawVisibility
        }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

    override val isInner: Boolean get() = withValidityAssertion { firSymbol.isInner }
    override val isData: Boolean get() = withValidityAssertion { firSymbol.isData }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isFun: Boolean get() = withValidityAssertion { firSymbol.isFun }
    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isExternal }

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


    override val classKind: KtClassKind
        get() = withValidityAssertion {
            when (firSymbol.classKind) {
                ClassKind.INTERFACE -> KtClassKind.INTERFACE
                ClassKind.ENUM_CLASS -> KtClassKind.ENUM_CLASS
                ClassKind.ENUM_ENTRY -> KtClassKind.ENUM_ENTRY
                ClassKind.ANNOTATION_CLASS -> KtClassKind.ANNOTATION_CLASS
                ClassKind.CLASS -> KtClassKind.CLASS
                ClassKind.OBJECT -> if (firSymbol.isCompanion) KtClassKind.COMPANION_OBJECT else KtClassKind.OBJECT
            }
        }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                firSymbol.isLocal -> KtSymbolKind.LOCAL
                firSymbol.classId.isNestedClass -> KtSymbolKind.CLASS_MEMBER
                else -> KtSymbolKind.TOP_LEVEL
            }
        }


    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        if (symbolKind == KtSymbolKind.LOCAL) {
            throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(classIdIfNonLocal?.asString().orEmpty())
        }
        return KtFirClassOrObjectInLibrarySymbolPointer(classIdIfNonLocal!!)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
