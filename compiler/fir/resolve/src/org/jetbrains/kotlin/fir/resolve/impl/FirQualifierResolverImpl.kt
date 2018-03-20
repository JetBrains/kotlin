/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeKotlinTypeProjectionInImpl
import org.jetbrains.kotlin.fir.types.impl.ConeKotlinTypeProjectionOutImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance

class FirQualifierResolverImpl(val session: FirSession) : FirQualifierResolver {

    private fun List<FirQualifierPart>.toTypeProjections() = flatMap {
        it.typeArguments.map {
            when (it) {
                is FirStarProjection -> StarProjection
                is FirTypeProjectionWithVariance -> {
                    val type = (it.type as FirResolvedType).type
                    when (it.variance) {
                        Variance.INVARIANT -> type
                        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionInImpl(type)
                        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOutImpl(type)
                    }
                }
                else -> error("!")
            }
        }
    }

    private fun FirMemberDeclaration.toConeKotlinType(fqName: ClassId, parts: List<FirQualifierPart>): ConeKotlinType? {
        return when (this) {
            is FirClass -> {
                ConeClassTypeImpl(fqName, parts.toTypeProjections())
            }
            is FirTypeAlias -> {
                ConeAbbreviatedTypeImpl(
                    abbreviationFqName = fqName,
                    typeArguments = parts.toTypeProjections(),
                    directExpansion = (this.abbreviatedType as FirResolvedType).type
                )

            }
            else -> error("!")
        }
    }

    override fun resolveTypeWithPrefix(parts: List<FirQualifierPart>, prefix: ClassId): ConeKotlinType? {
        val firProvider = FirProvider.getInstance(session)

        val fqName = ClassId(
            prefix.packageFqName,
            parts.fold(prefix.relativeClassName) { prefix, suffix -> prefix.child(suffix.name) },
            false
        )
        val foundClassifier = firProvider.getFirClassifierByFqName(fqName)

        return foundClassifier?.toConeKotlinType(fqName, parts)
    }

    override fun resolveType(parts: List<FirQualifierPart>): ConeKotlinType? {
        val firProvider = FirProvider.getInstance(session)

        if (parts.isNotEmpty()) {
            val lastPart = mutableListOf<FirQualifierPart>()
            val firstPart = parts.toMutableList()

            while (firstPart.isNotEmpty()) {
                lastPart.add(0, firstPart.last())
                firstPart.removeAt(firstPart.lastIndex)

                val fqName = ClassId(firstPart.toFqName(), lastPart.toFqName(), false)
                val foundClassifier = firProvider.getFirClassifierByFqName(fqName)

                if (foundClassifier != null) {
                    return foundClassifier.toConeKotlinType(fqName, parts)
                }
            }
            return null
        } else {
            return null
        }
    }

    private fun List<FirQualifierPart>.toFqNameUnsafe() = toFqName().toUnsafe()
    private fun List<FirQualifierPart>.toFqName() = fold(FqName.ROOT) { a, b -> a.child(b.name) }
}