/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.hasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFirSamConstructorSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KaFirSession,
) : KaSamConstructorSymbol(), KaFirSymbol<FirNamedFunctionSymbol> {
    override val token: KaLifetimeToken get() = builder.token
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val contextReceivers: List<KaContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion {
            firSymbol.createKtValueParameters(builder)
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.hasStableParameterNames
        }

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }

    override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val callableId: CallableId? get() = withValidityAssertion { firSymbol.getCallableId() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override fun createPointer(): KaSymbolPointer<KaSamConstructorSymbol> = withValidityAssertion {
        val callableId = firSymbol.callableId
        return KaFirSamConstructorSymbolPointer(ClassId(callableId.packageName, callableId.callableName))
    }
}
