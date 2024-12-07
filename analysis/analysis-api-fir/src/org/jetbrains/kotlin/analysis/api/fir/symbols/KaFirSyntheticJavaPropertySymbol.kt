/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirJavaSyntheticPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirSyntheticJavaPropertySymbol(
    override val firSymbol: FirSyntheticPropertySymbol,
    override val analysisSession: KaFirSession,
) : KaSyntheticJavaPropertySymbol(), KaFirSymbol<FirSyntheticPropertySymbol> {
    override val psi: PsiElement? get() = withValidityAssertion { firSymbol.findPsi() }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }
    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { KaFirReceiverParameterSymbol.create(null, analysisSession, this) }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val isExtension: Boolean
        get() = withValidityAssertion { false }

    override val origin: KaSymbolOrigin
        get() = super<KaSyntheticJavaPropertySymbol>.origin

    override val initializer: KaInitializerValue? get() = withValidityAssertion { firSymbol.getKtConstantInitializer(builder) }

    override val modality: KaSymbolModality get() = withValidityAssertion { firSymbol.kaSymbolModality }
    override val compilerVisibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion { firSymbol.getCallableId() }

    override val getter: KaPropertyGetterSymbol
        get() = withValidityAssertion {
            KaFirSyntheticPropertyGetterSymbol(firSymbol.getterSymbol!!, analysisSession)
        }

    override val javaGetterSymbol: KaNamedFunctionSymbol
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return builder.functionBuilder.buildNamedFunctionSymbol(fir.getter.delegate.symbol)
        }

    override val javaSetterSymbol: KaNamedFunctionSymbol?
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return fir.setter?.delegate?.let { builder.functionBuilder.buildNamedFunctionSymbol(it.symbol) }
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let {
                KaFirSyntheticPropertySetterSymbol(it, analysisSession)
            }
        }

    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }
    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isExternal }

    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol> = withValidityAssertion {
        KaFirJavaSyntheticPropertySymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            propertyName = name,
            isSynthetic = firSymbol is SyntheticSymbol,
        )
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
