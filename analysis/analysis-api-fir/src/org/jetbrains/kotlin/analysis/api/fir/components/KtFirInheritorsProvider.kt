/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.analysis.api.components.KtInheritorsProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion

internal class KtFirInheritorsProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtInheritorsProvider(), KtFirAnalysisSessionComponent {
    override fun getInheritorsOfSealedClass(
        classSymbol: KtNamedClassOrObjectSymbol
    ): List<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        require(classSymbol.modality == Modality.SEALED)
        require(classSymbol is KtFirNamedClassOrObjectSymbol)

        val inheritorClassIds = classSymbol.firRef.withFir { fir ->
            fir.getSealedClassInheritors(analysisSession.rootModuleSession)
        }

        with(analysisSession) {
            inheritorClassIds.mapNotNull { it.getCorrespondingToplevelClassOrObjectSymbol() as? KtNamedClassOrObjectSymbol }
        }
    }

    override fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol> = withValidityAssertion {
        require(classSymbol.classKind == KtClassKind.ENUM_CLASS)
        with(analysisSession) {
            classSymbol.getDeclaredMemberScope().getCallableSymbols().filterIsInstance<KtEnumEntrySymbol>().toList()
        }
    }

}