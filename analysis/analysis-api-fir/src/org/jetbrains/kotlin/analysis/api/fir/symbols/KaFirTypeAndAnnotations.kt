/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

context(analysisSession: KaFirSession)
internal fun FirClassSymbol<*>.superTypesList(): List<KaType> = resolvedSuperTypeRefs.mapToKtType()

context(analysisSession: KaFirSession)
private fun List<FirTypeRef>.mapToKtType(
): List<KaType> = map { typeRef ->
    firSymbolBuilder.typeBuilder.buildKtType(typeRef)
}

context(analysisSession: KaFirSession)
internal fun FirCallableSymbol<*>.returnType(): KaType =
    firSymbolBuilder.typeBuilder.buildKtType(resolvedReturnType)

