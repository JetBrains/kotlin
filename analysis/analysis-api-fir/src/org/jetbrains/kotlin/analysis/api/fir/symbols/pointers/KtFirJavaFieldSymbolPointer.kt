/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirJavaFieldSymbolPointer(
    containingClassId: ClassId,
    private val fieldName: Name
) : KtFirMemberSymbolPointer<KtJavaFieldSymbol>(containingClassId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtJavaFieldSymbol? {
        val javaField =
            candidates.getProperties(fieldName)
                .mapNotNull { it.fir as? FirJavaField }
                .singleOrNull()
                ?: return null

        return firSymbolBuilder.variableLikeBuilder.buildFieldSymbol(javaField.symbol)
    }
}