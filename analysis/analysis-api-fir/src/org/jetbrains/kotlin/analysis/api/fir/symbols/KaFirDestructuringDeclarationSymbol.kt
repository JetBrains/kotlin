/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirDestructuringDeclarationSymbol(
    override val firSymbol: FirVariableSymbol<*>,
    override val analysisSession: KaFirSession,
) : KaDestructuringDeclarationSymbol(), KaFirSymbol<FirVariableSymbol<*>> {

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

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaFirAnnotationListForDeclaration.create(firSymbol, builder) }

    override val entries: List<KaVariableSymbol>
        get() = withValidityAssertion {
            psi.entries.map { entry ->
                with(analysisSession) { entry.symbol }
            }
        }

    override fun createPointer(): KaSymbolPointer<KaDestructuringDeclarationSymbol> {
        return KaPsiBasedSymbolPointer.createForSymbolFromSource<KaDestructuringDeclarationSymbol>(this)
            ?: throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(SpecialNames.DESTRUCT.asString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KaFirDestructuringDeclarationSymbol && firSymbol == other.firSymbol
    }

    override fun hashCode(): Int {
        return firSymbol.hashCode()
    }
}