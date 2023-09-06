/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirEnumEntrySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbol(
    override val firSymbol: FirEnumEntrySymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtEnumEntrySymbol(), KtFirSymbol<FirEnumEntrySymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val containingEnumClassIdIfNonLocal: ClassId? get() = withValidityAssertion { callableIdIfNonLocal?.classId }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val enumEntryInitializer: KtFirEnumEntryInitializerSymbol? by cached {
        if (firSymbol.fir.initializer == null) {
            return@cached null
        }

        firSymbol.fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val initializerExpression = firSymbol.fir.initializer
        check(initializerExpression is FirAnonymousObjectExpression) {
            "Unexpected enum entry initializer: ${initializerExpression?.javaClass}"
        }

        val classifierBuilder = analysisSession.firSymbolBuilder.classifierBuilder
        classifierBuilder.buildAnonymousObjectSymbol(initializerExpression.anonymousObject.symbol) as? KtFirEnumEntryInitializerSymbol
            ?: error("The anonymous object symbol for an enum entry initializer should be a ${KtFirEnumEntryInitializerSymbol::class.simpleName}")
    }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFirEnumEntrySymbolPointer(requireOwnerPointer(), firSymbol.name)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
