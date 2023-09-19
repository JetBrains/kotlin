/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

@NoMutableState
class FirQualifierResolverImpl(val session: FirSession) : FirQualifierResolver() {

    override fun resolveSymbolWithPrefix(parts: List<FirQualifierPart>, prefix: ClassId): FirClassifierSymbol<*>? {
        val symbolProvider = session.symbolProvider

        val fqName = ClassId(
            prefix.packageFqName,
            parts.drop(1).fold(prefix.relativeClassName) { result, suffix -> result.child(suffix.name) },
            isLocal = false
        )
        return symbolProvider.getClassLikeSymbolByClassId(fqName)
    }

    override fun resolveSymbol(parts: List<FirQualifierPart>): FirClassifierSymbol<*>? {
        if (parts.firstOrNull()?.name?.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
            return resolveSymbol(parts.drop(1))
        }

        val firProvider = session.symbolProvider

        if (parts.isNotEmpty()) {
            val lastPart = mutableListOf<FirQualifierPart>()
            val firstPart = parts.toMutableList()

            while (firstPart.isNotEmpty()) {
                lastPart.add(0, firstPart.last())
                firstPart.removeAt(firstPart.lastIndex)

                val fqName = ClassId(firstPart.toFqName(), lastPart.toFqName(), isLocal = false)
                val foundSymbol = firProvider.getClassLikeSymbolByClassId(fqName)
                if (foundSymbol != null) {
                    return foundSymbol
                }
            }
        }
        return null
    }

    private fun List<FirQualifierPart>.toFqName() = fold(FqName.ROOT) { a, b -> a.child(b.name) }
}
