/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirScriptParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

internal abstract class KtFirLocalOrErrorVariableSymbol<E : FirVariable, S : FirVariableSymbol<E>>(
    override val firSymbol: S,
    override val analysisSession: KtFirAnalysisSession,
) : KtLocalVariableSymbol(), KtFirSymbol<S> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.getAllowedPsi() }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val symbolKind: KtSymbolKind get() = withValidityAssertion { KtSymbolKind.LOCAL }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtLocalVariableSymbol>(this)?.let { return it }

        if (firSymbol.fir.source?.kind == KtFakeSourceElementKind.ScriptParameter) {
            return KtFirScriptParameterSymbolPointer(name, requireOwnerPointer())
        }

        throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}

internal class KtFirLocalVariableSymbol(firSymbol: FirPropertySymbol, analysisSession: KtFirAnalysisSession) :
    KtFirLocalOrErrorVariableSymbol<FirProperty, FirPropertySymbol>(firSymbol, analysisSession) {
    init {
        assert(firSymbol.isLocal)
    }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
}

internal class KtFirErrorVariableSymbol(
    firSymbol: FirErrorPropertySymbol,
    analysisSession: KtFirAnalysisSession,
) : KtFirLocalOrErrorVariableSymbol<FirErrorProperty, FirErrorPropertySymbol>(firSymbol, analysisSession),
    KtFirSymbol<FirErrorPropertySymbol> {
    override val isVal: Boolean get() = withValidityAssertion { firSymbol.fir.isVal }
}
