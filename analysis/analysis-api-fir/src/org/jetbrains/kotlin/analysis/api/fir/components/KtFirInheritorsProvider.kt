/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtInheritorsProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors

internal class KtFirInheritorsProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtInheritorsProvider(), KtFirAnalysisSessionComponent {
    override fun getInheritorsOfSealedClass(
        classSymbol: KtNamedClassOrObjectSymbol
    ): List<KtNamedClassOrObjectSymbol> {
        require(classSymbol.modality == Modality.SEALED)
        require(classSymbol is KtFirNamedClassOrObjectSymbol)

        val inheritorClassIds = classSymbol.firSymbol.fir.getSealedClassInheritors(analysisSession.useSiteSession)

        return with(analysisSession) {
            inheritorClassIds.mapNotNull { getClassOrObjectSymbolByClassId(it) as? KtNamedClassOrObjectSymbol }
        }
    }

    override fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol> {
        require(classSymbol.classKind == KtClassKind.ENUM_CLASS)
        return with(analysisSession) {
            classSymbol.getStaticDeclaredMemberScope().getCallableSymbols().filterIsInstance<KtEnumEntrySymbol>().toList()
        }
    }

}