/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirDestructuringDeclarationSymbol private constructor(
    override val backingPsi: KtDestructuringDeclaration?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirVariableSymbol<*>>,
) : KaDestructuringDeclarationSymbol(), KaFirKtBasedSymbol<KtDestructuringDeclaration, FirVariableSymbol<*>> {
    constructor(declaration: KtDestructuringDeclaration, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    override val psi: KtDestructuringDeclaration
        get() = withValidityAssertion {
            backingPsi ?: when (val psi = firSymbol.fir.psi) {
                is KtDestructuringDeclaration -> psi
                is KtParameter -> psi.destructuringDeclaration
                    ?: errorWithAttachment("Expected to find lambda ${KtParameter::class.simpleName} with ${KtDestructuringDeclaration::class.simpleName}") {
                        withPsiEntry("psi", psi)
                        withFirSymbolEntry("firSymbol", firSymbol)
                    }
                else -> errorWithAttachment("Expected to find ${KtDestructuringDeclaration::class.simpleName} or ${KtParameter::class.simpleName} but ${psi?.let { it::class.simpleName }} found") {
                    withPsiEntry("psi", psi)
                    withFirSymbolEntry("firSymbol", firSymbol)
                }
            }
        }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val entries: List<KaVariableSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                psi.entries.map { entry ->
                    entry.symbol
                }
            }
        }

    override fun createPointer(): KaSymbolPointer<KaDestructuringDeclarationSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaDestructuringDeclarationSymbol>()
            ?: throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(SpecialNames.DESTRUCT.asString())
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}