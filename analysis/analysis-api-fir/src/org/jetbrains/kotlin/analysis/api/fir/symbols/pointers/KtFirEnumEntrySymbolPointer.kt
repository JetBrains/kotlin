/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbolPointer(
    private val ownerPointer: KtSymbolPointer<KtClassOrObjectSymbol>,
    private val name: Name,
) : KtSymbolPointer<KtEnumEntrySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtEnumEntrySymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val owner = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        val enumClass = owner?.firSymbol?.fir as? FirRegularClass ?: return null
        if (enumClass.classKind != ClassKind.ENUM_CLASS) return null
        val enumEntry = enumClass.enumEntryByName(name) ?: return null
        return analysisSession.firSymbolBuilder.buildEnumEntrySymbol(enumEntry.symbol)
    }

    private fun FirRegularClass.enumEntryByName(name: Name): FirEnumEntry? =
        declarations.firstOrNull { member ->
            member is FirEnumEntry && member.name == name
        } as FirEnumEntry?

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = other === this ||
            other is KtFirEnumEntrySymbolPointer &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
