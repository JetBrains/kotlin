/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ProjectionKind.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.type

internal interface ConeTypePointer {
    fun restore(session: KaFirSession): ConeKotlinType?
}

@KaAnalysisNonPublicApi
internal class ConeClassLikeTypePointer(
    symbol: KaClassLikeSymbol,
    coneType: ConeClassLikeType,
    builder: KaSymbolByFirBuilder
) : ConeTypePointer {
    private val classPointer = symbol.createPointer()
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder) }
    private val isNullable = coneType.isNullable

    override fun restore(session: KaFirSession): ConeClassLikeTypeImpl? {
        val classSymbol = with(session) { classPointer.restoreSymbol() } as? KaFirSymbol<*> ?: return null
        val classFirSymbol = classSymbol.firSymbol as? FirClassLikeSymbol<*> ?: return null

        val typeArguments = buildList(typeArgumentPointers.size) {
            for (typeArgumentPointer in typeArgumentPointers) {
                val typeArgument = typeArgumentPointer.restore(session) ?: return null
                add(typeArgument)
            }
        }

        return ConeClassLikeTypeImpl(
            lookupTag = classFirSymbol.toLookupTag(),
            typeArguments = typeArguments.toTypedArray(),
            isNullable = isNullable
        )
    }
}

@KaAnalysisNonPublicApi
internal class ConeErrorTypePointer(coneType: ConeErrorType, builder: KaSymbolByFirBuilder) : ConeTypePointer {
    @Suppress("SpellCheckingInspection")
    private val isUninferredParameter = coneType.isUninferredParameter

    private val coneDiagnosticPointer = ConeDiagnosticPointer.create(coneType.diagnostic, builder)
    private val delegatedTypePointer = coneType.delegatedType?.let(builder.typeBuilder::buildKtType)?.createPointer()
    private val typeArgumentPointers = coneType.typeArguments.map { ConeTypeProjectionPointer(it, builder) }

    override fun restore(session: KaFirSession): ConeErrorType? {
        val coneDiagnostic = coneDiagnosticPointer.restore(session) ?: return null

        val delegatedConeType = if (delegatedTypePointer != null) {
            val delegatedType = delegatedTypePointer.restore(session) as? KaFirType ?: return null
            delegatedType.coneType
        } else {
            null
        }

        val typeArguments = buildList(typeArgumentPointers.size) {
            for (typeArgumentPointer in typeArgumentPointers) {
                val typeArgument = typeArgumentPointer.restore(session) ?: return null
                add(typeArgument)
            }
        }

        return ConeErrorType(
            diagnostic = coneDiagnostic,
            isUninferredParameter = isUninferredParameter,
            delegatedType = delegatedConeType,
            typeArguments = typeArguments.toTypedArray()
        )
    }
}

@KaAnalysisNonPublicApi
internal class ConeTypeProjectionPointer(projection: ConeTypeProjection, builder: KaSymbolByFirBuilder) {
    private val kind = projection.kind
    private val typePointer = projection.type?.let { builder.typeBuilder.buildKtType(it).createPointer() }

    fun restore(session: KaFirSession): ConeTypeProjection? {
        if (kind == STAR) {
            return ConeStarProjection
        }

        requireNotNull(typePointer)
        val type = typePointer.restore(session) as? KaFirType ?: return null
        val coneType = type.coneType

        return when (kind) {
            IN -> ConeKotlinTypeProjectionIn(coneType)
            OUT -> ConeKotlinTypeProjectionOut(coneType)
            INVARIANT -> coneType
            STAR -> error("Should be handled above")
        }
    }
}