/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toArrayOfFactoryName
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name

internal object KaFirArrayOfSymbolProvider {
    internal fun KaFirSession.arrayOfSymbol(identifier: Name): KaNamedFunctionSymbol? {
        val firSymbol = firSession.symbolProvider.getTopLevelCallableSymbols(
            packageFqName = StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
            name = identifier,
        ).firstOrNull {
            /* choose (for a byte array)
             * public fun byteArrayOf(vararg elements: kotlin.Byte): kotlin.ByteArray
             */
            (it as? FirNamedFunctionSymbol)?.fir?.valueParameters?.singleOrNull()?.isVararg == true
        } as? FirNamedFunctionSymbol ?: return null

        return firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(firSymbol)
    }

    internal fun KaFirSession.arrayOfSymbol(collectionLiteral: FirCollectionLiteral): KaNamedFunctionSymbol? {
        val type = collectionLiteral.resolvedType as? ConeClassLikeType ?: return null
        val factoryName = toArrayOfFactoryName(type, firSession, eagerlyReturnNonPrimitive = true) ?: return null
        return arrayOfSymbol(factoryName)
    }
}
