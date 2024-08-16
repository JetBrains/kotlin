/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

internal fun FirClassSymbol<*>.superTypesList(builder: KaSymbolByFirBuilder): List<KaType> = resolvedSuperTypeRefs.mapToKtType(builder)

private fun List<FirTypeRef>.mapToKtType(
    builder: KaSymbolByFirBuilder,
): List<KaType> = map { typeRef ->
    builder.typeBuilder.buildKtType(typeRef)
}

internal fun FirCallableSymbol<*>.returnType(builder: KaSymbolByFirBuilder): KaType =
    builder.typeBuilder.buildKtType(resolvedReturnType)

