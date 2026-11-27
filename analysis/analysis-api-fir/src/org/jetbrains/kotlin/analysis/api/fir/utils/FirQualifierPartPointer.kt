/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl

internal class FirQualifierPartPointer(
    qualifierPart: FirQualifierPart,
    analysisSession: KaFirSession,
    guard: ConeTypeRecursionGuard,
) {
    private val name = qualifierPart.name
    private val typeArgumentListPointer = FirTypeArgumentListPointer(qualifierPart.typeArgumentList, analysisSession, guard)

    context(analysisSession: KaFirSession)
    fun restore(guard: ConeTypeRecursionGuard): FirQualifierPart? {
        val firTypeArgumentList = typeArgumentListPointer.restore(guard) ?: return null
        return FirQualifierPartImpl(source = null, name, firTypeArgumentList)
    }
}

private class FirTypeArgumentListPointer(
    typeArgumentList: FirTypeArgumentList,
    analysisSession: KaFirSession,
    guard: ConeTypeRecursionGuard,
) {
    private val typeArgumentPointers = typeArgumentList.typeArguments
        .map { ConeTypeProjectionPointer(it.toConeTypeProjection(), analysisSession, guard) }

    context(analysisSession: KaFirSession)
    fun restore(guard: ConeTypeRecursionGuard): FirTypeArgumentList? {
        return FirTypeArgumentListImpl(source = null).apply {
            for (typeArgumentPointer in typeArgumentPointers) {
                val coneTypeArgument = typeArgumentPointer.restore(guard) ?: return null

                val typeArgument = when (coneTypeArgument) {
                    ConeStarProjection -> buildStarProjection()
                    is ConeKotlinTypeProjection -> {
                        buildTypeProjectionWithVariance {
                            typeRef = buildResolvedTypeRef { this.coneType = coneTypeArgument.type }
                            variance = coneTypeArgument.kind.toVariance()
                        }
                    }
                }

                typeArguments.add(typeArgument)
            }
        }
    }
}
