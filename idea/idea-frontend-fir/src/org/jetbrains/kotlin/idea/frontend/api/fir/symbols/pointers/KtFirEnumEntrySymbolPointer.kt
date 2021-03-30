/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbolPointer(
    private val ownerClassId: ClassId,
    private val name: Name
) : KtSymbolPointer<KtEnumEntrySymbol>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtEnumEntrySymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val enumClass = getEnumClass(analysisSession, ownerClassId)
            ?: return null
        val enumEntry = enumClass.enumEntryByName(name)
            ?: return null
        return analysisSession.firSymbolBuilder.buildEnumEntrySymbol(enumEntry)
    }

    private fun getEnumClass(analysisSession: KtFirAnalysisSession, classId: ClassId): FirRegularClass? {
        val enumClass = analysisSession.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
            ?: return null
        if (enumClass.classKind != ClassKind.ENUM_CLASS) return null
        return enumClass
    }

    private fun FirRegularClass.enumEntryByName(name: Name): FirEnumEntry? =
        declarations.firstOrNull { member ->
            member is FirEnumEntry && member.name == name
        } as FirEnumEntry?
}

