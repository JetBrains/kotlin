/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOf
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirArrayOfSymbolProvider.arrayTypeToArrayOfCall
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.idea.references.KtCollectionLiteralReference
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirCollectionLiteralReference(
    expression: KtCollectionLiteralExpression,
) : KtCollectionLiteralReference(expression), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val fir = element.getOrBuildFirSafe<FirArrayLiteral>(resolutionFacade) ?: return emptyList()

        val type = fir.resolvedType as? ConeClassLikeType ?: return listOfNotNull(arrayOfSymbol(arrayOf))
        val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
        return listOfNotNull(arrayOfSymbol(call))
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }
}
