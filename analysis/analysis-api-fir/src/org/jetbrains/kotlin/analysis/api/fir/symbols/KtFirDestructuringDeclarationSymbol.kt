/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KtFirDestructuringDeclarationSymbol(
    override val firSymbol: FirVariableSymbol<*>,
    override val analysisSession: KtFirAnalysisSession,
) : KtDestructuringDeclarationSymbol(), KtFirSymbol<FirVariableSymbol<*>> {

    init {
        require(firSymbol.name == SpecialNames.DESTRUCT)
    }

    override val psi: KtDestructuringDeclaration
        get() = withValidityAssertion {
            when (val psi = firSymbol.fir.psi) {
                is KtDestructuringDeclaration -> psi
                is KtParameter -> psi.destructuringDeclaration
                    ?: errorWithAttachment("Expected to find lambda ${KtParameter::class} with ${KtDestructuringDeclaration::class}") {
                        withPsiEntry("psi", psi)
                    }
                else -> errorWithAttachment("Expected to find ${KtDestructuringDeclaration::class} or ${KtParameter::class} but ${psi?.let { it::class }} found") {
                    withPsiEntry("psi", psi)
                }
            }
        }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion { KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token) }

    override val entries: List<KtLocalVariableSymbol>
        get() = withValidityAssertion {
            psi.entries.map { entry ->
                with(analysisSession) { entry.getDestructuringDeclarationEntrySymbol() }
            }
        }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtDestructuringDeclarationSymbol> {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource<KtDestructuringDeclarationSymbol>(this)
            ?: throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(SpecialNames.DESTRUCT.asString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KtFirDestructuringDeclarationSymbol && firSymbol == other.firSymbol
    }

    override fun hashCode(): Int {
        return firSymbol.hashCode()
    }
}