/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForReceiverParameter
import org.jetbrains.kotlin.analysis.api.fir.hasAnnotation
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirReceiverParameterSymbol private constructor(
    val owningBackingPsi: KtCallableDeclaration?,
    val analysisSession: KaFirSession,
    val owningKaSymbol: KaCallableSymbol,
) : KaReceiverParameterSymbol() {
    val owningFirSymbol: FirCallableSymbol<*>
        get() = owningKaSymbol.firSymbol

    init {
        requireWithAttachment(
            owningBackingPsi == null || owningBackingPsi.receiverTypeReference != null,
            { "${owningBackingPsi!!::class.simpleName} doesn't have an extension receiver." },
        ) {
            withPsiEntry("declaration", owningBackingPsi)
        }
    }

    override val token: KaLifetimeToken
        get() = analysisSession.token

    override val psi: PsiElement?
        get() = withValidityAssertion {
            owningBackingPsi?.receiverTypeReference ?: owningFirSymbol.fir.receiverParameter?.typeRef?.psi
        }

    override val returnType: KaType
        get() = withValidityAssertion {
            owningFirSymbol.resolvedReceiverTypeRef?.let {
                analysisSession.firSymbolBuilder.typeBuilder.buildKtType(it)
            } ?: errorWithAttachment("${owningFirSymbol::class.simpleName} doesn't have an extension receiver") {
                withFirEntry("callable", owningFirSymbol.fir)
            }
        }

    override val owningCallableSymbol: KaCallableSymbol
        get() = withValidityAssertion { owningKaSymbol }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { owningKaSymbol.origin }

    @KaExperimentalApi
    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol> = withValidityAssertion {
        KaBaseReceiverParameterSymbolPointer(owningKaSymbol.createPointer())
    }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (owningBackingPsi?.receiverTypeReference?.hasAnnotation(AnnotationUseSiteTarget.RECEIVER) == false)
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForReceiverParameter.create(owningFirSymbol, builder = analysisSession.firSymbolBuilder)
        }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirReceiverParameterSymbol &&
            other.owningKaSymbol == this.owningKaSymbol

    /**
     * +1 is used to reduce the chance of clash with the original symbol
     * and other dependent parts (like backing field, property accessors, etc.)
     */
    override fun hashCode(): Int = 31 * owningKaSymbol.hashCode() + 1

    companion object {
        fun create(
            owningBackingPsi: KtCallableDeclaration?,
            analysisSession: KaFirSession,
            owningKaSymbol: KaCallableSymbol,
        ): KaFirReceiverParameterSymbol? {
            if (owningBackingPsi != null && owningBackingPsi.receiverTypeReference == null) return null
            if (owningBackingPsi == null && owningKaSymbol.firSymbol.fir.receiverParameter == null) return null

            return KaFirReceiverParameterSymbol(owningBackingPsi, analysisSession, owningKaSymbol)
        }
    }
}
