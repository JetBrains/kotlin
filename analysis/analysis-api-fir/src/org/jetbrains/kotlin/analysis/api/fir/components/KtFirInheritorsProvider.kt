/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaInheritorsProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors

internal class KaFirInheritorsProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaInheritorsProvider(), KaFirSessionComponent {
    override fun getInheritorsOfSealedClass(
        classSymbol: KaNamedClassOrObjectSymbol
    ): List<KaNamedClassOrObjectSymbol> {
        require(classSymbol.modality == Modality.SEALED)
        require(classSymbol is KaFirNamedClassOrObjectSymbol)

        val inheritorClassIds = classSymbol.firSymbol.fir.getSealedClassInheritors(analysisSession.useSiteSession)

        return with(analysisSession) {
            inheritorClassIds.mapNotNull { getClassOrObjectSymbolByClassId(it) as? KaNamedClassOrObjectSymbol }
        }
    }

    override fun getEnumEntries(classSymbol: KaNamedClassOrObjectSymbol): List<KaEnumEntrySymbol> {
        require(classSymbol.classKind == KaClassKind.ENUM_CLASS)
        return with(analysisSession) {
            classSymbol.getStaticDeclaredMemberScope().getCallableSymbols().filterIsInstance<KaEnumEntrySymbol>().toList()
        }
    }

}