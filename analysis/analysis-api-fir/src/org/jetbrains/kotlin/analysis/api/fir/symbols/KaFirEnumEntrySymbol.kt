/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirEnumEntrySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.impl.base.util.callableId
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration

internal class KaFirEnumEntrySymbol private constructor(
    override val backingPsi: KtEnumEntry?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirEnumEntrySymbol>,
) : KaEnumEntrySymbol(), KaFirKtBasedSymbol<KtEnumEntry, FirEnumEntrySymbol> {
    constructor(declaration: KtEnumEntry, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirEnumEntrySymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtEnumEntry,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val enumEntryInitializer: KaFirEnumEntryInitializerSymbol?
        get() = withValidityAssertion {
            if (firSymbol.fir.initializer == null) {
                return@withValidityAssertion null
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
        psiBasedSymbolPointerOfTypeIfSource<KaEnumEntrySymbol>()
            ?: KaFirEnumEntrySymbolPointer(analysisSession.createOwnerPointer(this), name, this)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
