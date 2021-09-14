/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbolPointer(
    private val ownerClassId: ClassId,
    private val name: Name
) : KtSymbolPointer<KtEnumEntrySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtEnumEntrySymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val enumClass = getEnumClass(analysisSession, ownerClassId)
            ?: return null
        val enumEntry = enumClass.enumEntryByName(name)
            ?: return null
        return analysisSession.firSymbolBuilder.buildEnumEntrySymbol(enumEntry)
    }

    private fun getEnumClass(analysisSession: KtFirAnalysisSession, classId: ClassId): FirRegularClass? {
        val enumClass = analysisSession.firSymbolProvider.getClassLikeSymbolByClassId(classId)?.fir as? FirRegularClass
            ?: return null
        if (enumClass.classKind != ClassKind.ENUM_CLASS) return null
        return enumClass
    }

    private fun FirRegularClass.enumEntryByName(name: Name): FirEnumEntry? =
        declarations.firstOrNull { member ->
            member is FirEnumEntry && member.name == name
        } as FirEnumEntry?
}

