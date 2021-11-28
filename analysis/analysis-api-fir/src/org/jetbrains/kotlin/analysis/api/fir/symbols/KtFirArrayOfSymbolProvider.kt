/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object KtFirArrayOfSymbolProvider {
    internal fun KtFirAnalysisSession.arrayOfSymbol(identifier: Name): KtFirFunctionSymbol? {
        val fir = firResolveState.rootModuleSession.symbolProvider.getTopLevelCallableSymbols(kotlinPackage, identifier).firstOrNull {
            /* choose (for byte array)
             * public fun byteArrayOf(vararg elements: kotlin.Byte): kotlin.ByteArray
             */
            (it as? FirFunctionSymbol<*>)?.fir?.valueParameters?.singleOrNull()?.isVararg == true
        }?.fir as? FirSimpleFunction ?: return null
        return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(fir)
    }

    private val kotlinPackage = FqName("kotlin")
    internal val arrayOf = Name.identifier("arrayOf")
    internal val arrayTypeToArrayOfCall = run {
        StandardClassIds.primitiveArrayTypeByElementType.values + StandardClassIds.unsignedArrayTypeByElementType.values
    }.associateWith { it.correspondingArrayOfCallFqName() }

    private fun ClassId.correspondingArrayOfCallFqName(): Name =
        Name.identifier("${shortClassName.identifier.replaceFirstChar(Char::lowercaseChar)}Of")
}
