/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression

class KtFirCollectionLiteralReference(
    expression: KtCollectionLiteralExpression
) : KtCollectionLiteralReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = element.getOrBuildFirSafe<FirArrayLiteral>(firResolveSession) ?: return emptyList()
        val type = fir.coneTypeSafe<ConeClassLikeType>() ?: return listOfNotNull(arrayOfSymbol(arrayOf))
        val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
        return listOfNotNull(arrayOfSymbol(call))
    }
}
