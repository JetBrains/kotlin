/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.types.Variance

/**
 * Returns whether [subclass] is a strict subtype of [superclass]. Resolves [subclass] to [FirResolvePhase.SUPER_TYPES].
 */
@KaImplementationDetail
fun isSubclassOf(
    subclass: FirClass,
    superclass: FirClass,
    useSiteSession: FirSession,
    allowIndirectSubtyping: Boolean = true,
): Boolean {
    subclass.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

    if (subclass.superConeTypes.any { it.toRegularClassSymbol(useSiteSession) == superclass.symbol }) return true
    if (!allowIndirectSubtyping) return false

    subclass.superConeTypes.forEach { superType ->
        val superOfSub = superType.toRegularClassSymbol(useSiteSession) ?: return@forEach
        if (isSubclassOf(superOfSub.fir, superclass, useSiteSession, allowIndirectSubtyping = true)) return true
    }
    return false
}

/**
 * @see org.jetbrains.kotlin.analysis.api.types.KaType.abbreviation
 */
internal fun KaSymbolByFirBuilder.buildAbbreviatedType(coneType: ConeClassLikeType): KaUsualClassType? {
    return coneType.abbreviatedType?.let { abbreviatedConeType ->
        // If the resulting type is an error type, the abbreviated type couldn't be resolved. As per the contract of
        // `KaType.abbreviatedType`, we should return `null` in such cases. The user can then fall back to the expanded type.
        typeBuilder.buildKtType(abbreviatedConeType) as? KaUsualClassType
    }
}

internal fun ProjectionKind.toVariance(): Variance = when (this) {
    ProjectionKind.OUT -> Variance.OUT_VARIANCE
    ProjectionKind.IN -> Variance.IN_VARIANCE
    ProjectionKind.INVARIANT -> Variance.INVARIANT
    ProjectionKind.STAR -> error("KtStarProjectionTypeArgument should not be directly created")
}

internal fun <C : ConeKotlinType, T : KaType> createTypePointer(
    coneType: C,
    builder: KaSymbolByFirBuilder,
    typeFactory: (C, KaSymbolByFirBuilder) -> T?,
): KaTypePointer<T> {
    val coneTypePointer = coneType.createPointer(builder)
    return KaGenericTypePointer(coneTypePointer, typeFactory)
}

private class KaGenericTypePointer<C : ConeKotlinType, T : KaType>(
    private val coneTypePointer: ConeTypePointer<C>,
    private val typeFactory: (C, KaSymbolByFirBuilder) -> T?
) : KaTypePointer<T> {
    override fun restore(session: KaSession): T? {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        return typeFactory(coneType, session.firSymbolBuilder)
    }
}
