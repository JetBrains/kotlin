/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.resolveSupertypesInTheAir
import org.jetbrains.kotlin.fir.types.FirTypeRef


internal fun FirRefWithValidityCheck<FirClass>.superTypesList(builder: KtSymbolByFirBuilder): List<KtType> =
    withFir(FirResolvePhase.SUPER_TYPES) { fir ->
        fir.superTypeRefs.mapToKtType(builder)
    }

internal fun FirRefWithValidityCheck<FirRegularClass>.superTypesAndAnnotationsListForRegularClass(builder: KtSymbolByFirBuilder): List<KtType> {
    return withFir { fir ->
        if (fir.resolvePhase >= FirResolvePhase.SUPER_TYPES) {
            fir.superTypeRefs.mapToKtType(builder)
        } else null
    } ?: withFirByType(ResolveType.NoResolve) { fir ->
        fir.resolveSupertypesInTheAir(builder.rootSession).mapToKtType(builder)
    }
}

private fun List<FirTypeRef>.mapToKtType(
    builder: KtSymbolByFirBuilder,
): List<KtType> = map { typeRef ->
    builder.typeBuilder.buildKtType(typeRef)
}

internal fun FirRefWithValidityCheck<FirTypedDeclaration>.returnType(
    typeResolvePhase: FirResolvePhase,
    builder: KtSymbolByFirBuilder
) = withFir(typeResolvePhase) { builder.typeBuilder.buildKtType(it.returnTypeRef) }

internal fun FirRefWithValidityCheck<FirCallableDeclaration>.receiverType(
    builder: KtSymbolByFirBuilder
): KtType? = withFir(FirResolvePhase.TYPES) { fir ->
    fir.receiverTypeRef?.let { receiver ->
        builder.typeBuilder.buildKtType(receiver)
    }
}
