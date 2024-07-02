/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.types.toConeTypeProjection

internal class FirQualifierPartPointer(qualifierPart: FirQualifierPart, builder: KaSymbolByFirBuilder) {
    private val name = qualifierPart.name
    private val typeArgumentPointers = qualifierPart.typeArgumentList.typeArguments
        .map { ConeTypeProjectionPointer(it.toConeTypeProjection(), builder) }

    fun restore(session: KaFirSession): FirQualifierPart? {
        val firTypeArgumentList = FirTypeArgumentListImpl(source = null).apply {
            for (typeArgumentPointer in typeArgumentPointers) {
                val coneTypeArgument = typeArgumentPointer.restore(session) ?: return null

                val typeArgument = when (coneTypeArgument) {
                    ConeStarProjection -> buildStarProjection()
                    is ConeKotlinTypeProjection -> {
                        buildTypeProjectionWithVariance {
                            typeRef = buildResolvedTypeRef { this.type = coneTypeArgument.type }
                            variance = coneTypeArgument.kind.toVariance()
                        }
                    }
                    else -> error("Unexpected type argument kind: $coneTypeArgument")
                }

                typeArguments.add(typeArgument)
            }
        }

        return FirQualifierPartImpl(source = null, name, firTypeArgumentList)
    }
}