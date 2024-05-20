/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirScriptParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

internal abstract class KaFirLocalOrErrorVariableSymbol<E : FirVariable, S : FirVariableSymbol<E>>(
    override val firSymbol: S,
    override val analysisSession: KaFirSession,
) : KaLocalVariableSymbol(), KaFirSymbol<S> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.getAllowedPsi() }

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val symbolKind: KaSymbolKind get() = withValidityAssertion { KaSymbolKind.LOCAL }

    override fun createPointer(): KaSymbolPointer<KaLocalVariableSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaLocalVariableSymbol>(this)?.let { return it }

        if (firSymbol.fir.source?.kind == KtFakeSourceElementKind.ScriptParameter) {
            return KaFirScriptParameterSymbolPointer(name, analysisSession.createOwnerPointer(this))
        }

        throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}

internal class KaFirLocalVariableSymbol(firSymbol: FirPropertySymbol, analysisSession: KaFirSession) :
    KaFirLocalOrErrorVariableSymbol<FirProperty, FirPropertySymbol>(firSymbol, analysisSession) {
    init {
        assert(firSymbol.isLocal)
    }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
}

internal class KaFirErrorVariableSymbol(
    firSymbol: FirErrorPropertySymbol,
    analysisSession: KaFirSession,
) : KaFirLocalOrErrorVariableSymbol<FirErrorProperty, FirErrorPropertySymbol>(firSymbol, analysisSession),
    KaFirSymbol<FirErrorPropertySymbol> {
    override val isVal: Boolean get() = withValidityAssertion { firSymbol.fir.isVal }
}
