/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.resolveSupertypesInTheAir
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.declarations.resolvePhase

internal fun FirClassSymbol<*>.superTypesList(builder: KtSymbolByFirBuilder): List<KtType> =
    resolvedSuperTypeRefs.mapToKtType(builder)

internal fun FirRegularClassSymbol.superTypesAndAnnotationsListForRegularClass(builder: KtSymbolByFirBuilder): List<KtType> {
    val fir = fir

    if (fir.resolvePhase >= FirResolvePhase.SUPER_TYPES) {
        return fir.superTypeRefs.mapToKtType(builder)
    }

    return fir.resolveSupertypesInTheAir(builder.rootSession).mapToKtType(builder)
}

private fun List<FirTypeRef>.mapToKtType(
    builder: KtSymbolByFirBuilder,
): List<KtType> = map { typeRef ->
    builder.typeBuilder.buildKtType(typeRef)
}

internal fun FirCallableSymbol<*>.returnType(builder: KtSymbolByFirBuilder): KtType =
    builder.typeBuilder.buildKtType(resolvedReturnType)

internal fun FirCallableSymbol<*>.receiver(builder: KtSymbolByFirBuilder): KtReceiverParameterSymbol? =
    builder.callableBuilder.buildExtensionReceiverSymbol(this)

internal fun FirCallableSymbol<*>.receiverType(builder: KtSymbolByFirBuilder): KtType? =
    resolvedReceiverTypeRef?.let { receiver ->
        builder.typeBuilder.buildKtType(receiver)
    }

