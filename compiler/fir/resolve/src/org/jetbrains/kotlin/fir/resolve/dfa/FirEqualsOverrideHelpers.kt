/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.collectSymbolsForType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun hasUntrustworthyOverriddenEquals(type: ConeKotlinType, session: FirSession, scopeSession: ScopeSession): Boolean {
    val symbolsForType = collectSymbolsForType(type, session)
    if (symbolsForType.any { it.hasUntrustworthyEqualsOverride(session, scopeSession, checkModality = true) }) return true

    val superTypes = lookupSuperTypes(
        symbolsForType,
        lookupInterfaces = false,
        deep = true,
        session,
        substituteTypes = false
    )
    val superClassSymbols = superTypes.mapNotNull {
        it.fullyExpandedType(session).toRegularClassSymbol(session)
    }

    return superClassSymbols.any { it.hasUntrustworthyEqualsOverride(session, scopeSession, checkModality = false) }
}

private fun FirClassSymbol<*>.hasUntrustworthyEqualsOverride(
    session: FirSession,
    scopeSession: ScopeSession,
    checkModality: Boolean,
): Boolean {
    val status = resolvedStatus
    if (checkModality && status.modality != Modality.FINAL) return true
    if (status.isExpect) return true
    if (isSmartcastPrimitive(classId)) return false
    when (classId) {
        StandardClassIds.Any -> return false
        // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
        StandardClassIds.Float, StandardClassIds.Double -> return true
        // kotlin.Enum has `equals()`, but we know it's reasonable
        StandardClassIds.Enum -> return false
    }

    // When the class belongs to a different module, "equals" contract might be changed without re-compilation
    // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
    // when different modules belong to a single project, so they're totally safe (see KT-50534)
    // if (moduleData != session.moduleData) {
    //     return true
    // }

    val ownerTag = this.toLookupTag()
    return this.unsubstitutedScope(
        session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS
    ).getFunctions(OperatorNameConventions.EQUALS).any {
        !it.isSubstitutionOrIntersectionOverride && it.isEquals(session) && ownerTag.isRealOwnerOf(it)
    }
}

/**
 * Determines if type smart-casting to the specified [ClassId] can be performed when values are
 * compared via equality. Because this is determined using the ClassId, only standard built-in
 * types are considered.
 */
internal fun isSmartcastPrimitive(classId: ClassId?): Boolean {
    return when (classId) {
        // Support other primitives as well: KT-62246.
        StandardClassIds.String,
            -> true

        else -> false
    }
}
