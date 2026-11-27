/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.hasAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirReceiverParameterSymbol private constructor(
    val owningBackingPsi: KtCallableDeclaration?,
    override val analysisSession: KaFirSession,
    val owningKaSymbol: KaCallableSymbol,
) : KaReceiverParameterSymbol(), KaFirKtBasedSymbol<KtTypeReference, FirReceiverParameterSymbol> {
    init {
        requireWithAttachment(
            owningBackingPsi == null || owningBackingPsi.receiverTypeReference != null,
            { "${owningBackingPsi!!::class.simpleName} doesn't have an extension receiver." },
        ) {
            withPsiEntry("declaration", owningBackingPsi)
        }
    }

    override val firSymbol: FirReceiverParameterSymbol
        get() = owningKaSymbol.firSymbol.let { owningFirSymbol ->
            owningFirSymbol.receiverParameterSymbol ?: errorWithAttachment("Receiver parameter is not found") {
                withFirSymbolEntry("callableSymbol", owningFirSymbol)
            }
        }

    override val backingPsi: KtTypeReference? = owningBackingPsi?.receiverTypeReference

    override val lazyFirSymbol: Lazy<FirReceiverParameterSymbol>
        get() = throw UnsupportedOperationException()

    override val returnType: KaType
        get() = entryPointImplementation {
            analysisSession.firSymbolBuilder.typeBuilder.buildKtType(firSymbol.resolvedType)
        }

    override val owningCallableSymbol: KaCallableSymbol
        get() = entryPointImplementation { owningKaSymbol }

    override val origin: KaSymbolOrigin
        get() = entryPointImplementation { owningKaSymbol.origin }

    override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol> = entryPointImplementation {
        KaFirReceiverParameterSymbolPointer(owningKaSymbol.createPointer(), this)
    }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            if (backingPsi?.hasAnnotation(AnnotationUseSiteTarget.RECEIVER) == false)
                KaFirEmptyAnnotationList(this.token)
            else
                KaFirAnnotationListForDeclaration.create(firSymbol = firSymbol)
        }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    override fun hashCode(): Int = psiOrSymbolHashCode()

    companion object {
        context(analysisSession: KaFirSession)
        fun create(
            owningBackingPsi: KtCallableDeclaration?,
            owningKaSymbol: KaCallableSymbol,
        ): KaFirReceiverParameterSymbol? {
            if (owningBackingPsi != null && owningBackingPsi.receiverTypeReference == null) return null
            if (owningBackingPsi == null && owningKaSymbol.firSymbol.fir.receiverParameter == null) return null

            return KaFirReceiverParameterSymbol(owningBackingPsi, analysisSession, owningKaSymbol)
        }
    }
}
