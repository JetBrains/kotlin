/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression

class KtFirCollectionLiteralReference(
    expression: KtCollectionLiteralExpression
) : KtCollectionLiteralReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = element.getOrBuildFirSafe<FirArrayOfCall>(firResolveState) ?: return emptyList()
        val type = fir.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return listOfNotNull(arrayOfSymbol(arrayOf))
        val call = arrayTypeToArrayOfCall[type.lookupTag.classId] ?: arrayOf
        return listOfNotNull(arrayOfSymbol(call))
    }

    private fun KtFirAnalysisSession.arrayOfSymbol(identifier: Name): KtSymbol? {
        val fir = firResolveState.rootModuleSession.symbolProvider.getTopLevelCallableSymbols(kotlinPackage, identifier).firstOrNull {
            /* choose (for byte array)
             * public fun byteArrayOf(vararg elements: kotlin.Byte): kotlin.ByteArray
             */
            (it as? FirFunctionSymbol<*>)?.fir?.valueParameters?.singleOrNull()?.isVararg == true
        }?.fir as? FirSimpleFunction ?: return null
        return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(fir)
    }

    companion object {
        private val kotlinPackage = FqName("kotlin")
        private val arrayOf = Name.identifier("arrayOf")
        private val arrayTypeToArrayOfCall = run {
            StandardClassIds.primitiveArrayTypeByElementType.values + StandardClassIds.unsignedArrayTypeByElementType.values
        }.associateWith { it.correspondingArrayOfCallFqName() }

        private fun ClassId.correspondingArrayOfCallFqName(): Name =
            Name.identifier("${shortClassName.identifier.decapitalize()}Of")

    }
}
