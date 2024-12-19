/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.hasAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseValueParameterFromDefaultSetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Represents a default value parameter for [KaFirDefaultPropertySetterSymbol] without explicitly
 * declared parameter.
 */
internal class KaFirDefaultSetterValueParameter(
    val owningKaSetter: KaFirDefaultPropertySetterSymbol,
) : KaValueParameterSymbol(), KaFirSymbol<FirValueParameterSymbol> {
    override val firSymbol: FirValueParameterSymbol
        get() = owningKaSetter.firSymbol.valueParameterSymbols.firstOrNull() ?: errorWithAttachment("Setter should have a parameter") {
            withFirSymbolEntry("setter", owningKaSetter.firSymbol)
        }

    override val analysisSession: KaFirSession
        get() = owningKaSetter.analysisSession

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { false }

    override val isVararg: Boolean
        get() = withValidityAssertion { false }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { false }

    override val isNoinline: Boolean
        get() = withValidityAssertion { false }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion { false }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { StandardNames.DEFAULT_VALUE_PARAMETER }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (owningKaSetter.owningKaProperty.backingPsi?.hasAnnotation(AnnotationUseSiteTarget.SETTER_PARAMETER) == false)
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        KaBaseValueParameterFromDefaultSetterSymbolPointer(owningKaSetter.owningKaProperty.createPointer(), this)
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { owningKaSetter.origin }

    override fun equals(other: Any?): Boolean = other === this ||
            other is KaFirDefaultSetterValueParameter &&
            other.owningKaSetter == owningKaSetter

    override fun hashCode(): Int = 31 * owningKaSetter.hashCode() + 1
}
