/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirEnumEntrySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirEnumEntrySymbol(
    override val firSymbol: FirEnumEntrySymbol,
    override val analysisSession: KaFirSession,
) : KaEnumEntrySymbol(), KaFirSymbol<FirEnumEntrySymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val callableId: CallableId? get() = withValidityAssertion { firSymbol.getCallableId() }

    override val enumEntryInitializer: KaFirEnumEntryInitializerSymbol? by cached {
        if (firSymbol.fir.initializer == null) {
            return@cached null
        }

        firSymbol.fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val initializerExpression = firSymbol.fir.initializer
        check(initializerExpression is FirAnonymousObjectExpression) {
            "Unexpected enum entry initializer: ${initializerExpression?.javaClass}"
        }

        val classifierBuilder = analysisSession.firSymbolBuilder.classifierBuilder
        classifierBuilder.buildAnonymousObjectSymbol(initializerExpression.anonymousObject.symbol) as? KaFirEnumEntryInitializerSymbol
            ?: error("The anonymous object symbol for an enum entry initializer should be a ${KaFirEnumEntryInitializerSymbol::class.simpleName}")
    }

    override fun createPointer(): KaSymbolPointer<KaEnumEntrySymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaEnumEntrySymbol>(this)
            ?: KaFirEnumEntrySymbolPointer(analysisSession.createOwnerPointer(this), firSymbol.name)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
