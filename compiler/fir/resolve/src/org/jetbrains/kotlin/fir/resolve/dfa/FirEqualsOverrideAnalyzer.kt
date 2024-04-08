/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.collectAllSubclasses
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirEqualsOverrideAnalyzer(private val session: FirSession, private val scopeSession: ScopeSession) {
    enum class Status {
        SAFE_FOR_SMART_CAST,
        SAFE_FOR_EXHAUSTIVENESS,
        UNSAFE
    }

    fun computeEqualsOverrideStatus(type: ConeKotlinType): Status {
        val symbolsForType = collectSymbolsForType(type, session)

        var safeOnlyForExhaustiveness = false
        val withEqualsOverrides = symbolsForType.filter { it.hasEqualsOverride(session, checkModality = true, allowSynthetic = false) }
        if (withEqualsOverrides.isNotEmpty()) {
            safeOnlyForExhaustiveness = withEqualsOverrides.any { it.isSealed && !it.hasEqualsOverrideFromInheritors(session) }
            if (!safeOnlyForExhaustiveness) return Status.UNSAFE
        }

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
        val overriddenInParent = superClassSymbols.any { it.hasEqualsOverride(session, checkModality = false, allowSynthetic = false) }
        val superClassesSafeForExhaustiveness = superClassSymbols.all { safeForExhaustiveness(it.classId) }
        return when {
            overriddenInParent -> if (superClassesSafeForExhaustiveness) Status.SAFE_FOR_EXHAUSTIVENESS else Status.UNSAFE
            safeOnlyForExhaustiveness -> Status.SAFE_FOR_EXHAUSTIVENESS
            else -> Status.SAFE_FOR_SMART_CAST
        }
    }

    private fun FirClassSymbol<*>.hasEqualsOverrideFromInheritors(session: FirSession): Boolean {
        require(isSealed)
        return collectAllSubclasses(session).any { it.hasEqualsOverride(session, checkModality = false, allowSynthetic = true) }
    }

    private fun FirBasedSymbol<*>.hasEqualsOverride(session: FirSession, checkModality: Boolean, allowSynthetic: Boolean): Boolean =
        this is FirClassSymbol<*> && hasEqualsOverride(session, checkModality, allowSynthetic)

    private fun FirClassSymbol<*>.hasEqualsOverride(session: FirSession, checkModality: Boolean, allowSynthetic: Boolean): Boolean {
        val status = resolvedStatus
        if (checkModality && status.modality != Modality.FINAL) return true
        if (status.isExpect) return true
        if (isSmartcastPrimitive(classId)) return false
        when (classId) {
            StandardClassIds.Any -> return false
            // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
            StandardClassIds.Float, StandardClassIds.Double -> return true
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
            if (allowSynthetic && it.origin is FirDeclarationOrigin.Synthetic) false
            else !it.isSubstitutionOrIntersectionOverride && it.fir.isEquals(session) && ownerTag.isRealOwnerOf(it)
        }
    }

    /**
     * Determines if type smart-casting to the specified [ClassId] can be performed when values are
     * compared via equality. Because this is determined using the ClassId, only standard built-in
     * types are considered.
     */
    fun isSmartcastPrimitive(classId: ClassId?): Boolean =
        // Support other primitives as well: KT-62246.
        classId == StandardClassIds.String

    fun safeForExhaustiveness(classId: ClassId): Boolean =
        classId == StandardClassIds.Enum || classId == StandardClassIds.Any
}