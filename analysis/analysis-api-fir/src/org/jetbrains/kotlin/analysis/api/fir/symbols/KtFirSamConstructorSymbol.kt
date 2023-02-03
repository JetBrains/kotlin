/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.utils.hasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirSamConstructorSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtSamConstructorSymbol(), KtFirSymbol<FirNamedFunctionSymbol> {
    override val token: KtLifetimeToken get() = builder.token
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val contextReceivers: List<KtContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion {
            firSymbol.createKtValueParameters(builder)
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.hasStableParameterNames
        }

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }

    override val receiverParameter: KtReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtSamConstructorSymbol> = withValidityAssertion {
        val callableId = firSymbol.callableId
        return KtFirSamConstructorSymbolPointer(ClassId(callableId.packageName, callableId.callableName))
    }
}
