/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl

internal class FirQualifierPartPointer(qualifierPart: FirQualifierPart, builder: KaSymbolByFirBuilder) {
    private val name = qualifierPart.name
    private val typeArgumentListPointer = FirTypeArgumentListPointer(qualifierPart.typeArgumentList, builder)

    context(analysisSession: KaFirSession)
    fun restore(): FirQualifierPart? {
        val firTypeArgumentList = typeArgumentListPointer.restore() ?: return null
        return FirQualifierPartImpl(source = null, name, firTypeArgumentList)
    }
}

private class FirTypeArgumentListPointer(typeArgumentList: FirTypeArgumentList, builder: KaSymbolByFirBuilder) {
    private val typeArgumentPointers = typeArgumentList.typeArguments
        .map { ConeTypeProjectionPointer(it.toConeTypeProjection(), builder) }

    context(analysisSession: KaFirSession)
    fun restore(): FirTypeArgumentList? {
        return FirTypeArgumentListImpl(source = null).apply {
            for (typeArgumentPointer in typeArgumentPointers) {
                val coneTypeArgument = typeArgumentPointer.restore() ?: return null

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
