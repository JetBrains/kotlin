/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.name.Name

internal class KaFirEnumEntrySymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaClassSymbol>,
    private val name: Name,
    originalSymbol: KaEnumEntrySymbol?,
) : KaBaseCachedSymbolPointer<KaEnumEntrySymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaEnumEntrySymbol? {
        require(analysisSession is KaFirSession)
        val owner = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        val enumClass = owner?.firSymbol?.fir as? FirRegularClass ?: return null
        if (enumClass.classKind != ClassKind.ENUM_CLASS) return null
        val enumEntry = enumClass.enumEntryByName(name) ?: return null
        return analysisSession.firSymbolBuilder.buildEnumEntrySymbol(enumEntry.symbol)
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun FirRegularClass.enumEntryByName(name: Name): FirEnumEntry? =
        declarations.firstOrNull { member ->
            member is FirEnumEntry && member.name == name
        } as FirEnumEntry?

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirEnumEntrySymbolPointer &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
