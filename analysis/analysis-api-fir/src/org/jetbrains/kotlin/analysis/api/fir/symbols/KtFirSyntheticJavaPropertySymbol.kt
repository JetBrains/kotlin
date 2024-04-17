/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirJavaSyntheticPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirSyntheticJavaPropertySymbol(
    override val firSymbol: FirSyntheticPropertySymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtSyntheticJavaPropertySymbol(), KtFirSymbol<FirSyntheticPropertySymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KtReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }


    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }

    override val initializer: KtInitializerValue? by cached { firSymbol.getKtConstantInitializer(builder) }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modality }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached {
        KtFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val getter: KtPropertyGetterSymbol
        get() = withValidityAssertion {
            builder.callableBuilder.buildGetterSymbol(firSymbol.getterSymbol!!)
        }
    override val javaGetterSymbol: KtFunctionSymbol
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return builder.functionLikeBuilder.buildFunctionSymbol(fir.getter.delegate.symbol)
        }
    override val javaSetterSymbol: KtFunctionSymbol?
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return fir.setter?.delegate?.let { builder.functionLikeBuilder.buildFunctionSymbol(it.symbol) }
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
        }

    override val backingFieldSymbol: KtBackingFieldSymbol?
        get() = null

    override val isFromPrimaryConstructor: Boolean get() = withValidityAssertion { false }
    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }

    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override val origin: KtSymbolOrigin get() = withValidityAssertion { KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol> = withValidityAssertion {
        KtFirJavaSyntheticPropertySymbolPointer(
            ownerPointer = requireOwnerPointer(),
            propertyName = name,
            isSynthetic = firSymbol is SyntheticSymbol,
        )
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
