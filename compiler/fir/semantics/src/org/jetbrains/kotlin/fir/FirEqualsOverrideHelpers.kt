/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.collectSymbolsForType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * The entries are ordered from least trustworthy to most trustworthy.
 * `A` is more trustworthy than `B` iff we can rely on `A` in more cases compared to `B`.
 */
enum class EqualsOverrideContract {
    UNKNOWN,
    TRUSTED_FOR_EXHAUSTIVENESS,

    /**
     * You can think of it as:
     * ```
     * contract {
     *     returns(true) implies (other is SelfType)
     *     returns(false) implies (other !is SelfLiteralType)
     * }
     * ```
     */
    SAFE_FOR_SMART_CAST,
}

fun computeEqualsOverrideContract(
    type: ConeKotlinType,
    session: FirSession,
    scopeSession: ScopeSession,
    trustExpectClasses: Boolean,
): EqualsOverrideContract {
    return computeEqualsOverrideContract(
        symbolsForType = collectSymbolsForType(type, session),
        session = session,
        scopeSession = scopeSession,
        visitedSymbols = mutableSetOf(),
        trustExpectClasses = trustExpectClasses,
    )
}

fun computeEqualsOverrideContract(
    symbolsForType: List<FirClassSymbol<*>>,
    session: FirSession,
    scopeSession: ScopeSession,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    trustExpectClasses: Boolean,
): EqualsOverrideContract {
    val subtypesContract = symbolsForType
        .maxOfOrNull { it.computeEqualsOverrideContract(session, scopeSession, visitedSymbols, trustExpectClasses) }
        ?: EqualsOverrideContract.SAFE_FOR_SMART_CAST

    if (subtypesContract == EqualsOverrideContract.UNKNOWN) {
        return EqualsOverrideContract.UNKNOWN
    }

    val superTypes = lookupSuperTypes(
        symbolsForType,
        lookupInterfaces = false,
        deep = true,
        session,
        substituteTypes = false,
        visitedSymbols = visitedSymbols,
    )
    val superClassSymbols = superTypes.mapNotNull { it.fullyExpandedType(session).toRegularClassSymbol(session) }
    val supertypesContract = superClassSymbols.minOfOrNull { it.getDeclaredEqualsOverrideContract(session, scopeSession, trustExpectClasses) }
        ?: EqualsOverrideContract.SAFE_FOR_SMART_CAST

    return minOf(subtypesContract, supertypesContract)
}

private fun FirClassSymbol<*>.computeEqualsOverrideContract(
    session: FirSession,
    scopeSession: ScopeSession,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
    trustExpectClasses: Boolean,
): EqualsOverrideContract {
    fun FirClassSymbol<*>.computeInheritorsContract(): EqualsOverrideContract {
        if (this !is FirRegularClassSymbol) return EqualsOverrideContract.UNKNOWN

        val inheritors = fir.getSealedClassInheritors(session).map {
            it.toSymbol(session) as? FirClassSymbol<*> ?: return EqualsOverrideContract.UNKNOWN
        }

        // Note that `sealed class` variants may have additional supertypes
        return inheritors.minOfOrNull { computeEqualsOverrideContract(listOf(it), session, scopeSession, visitedSymbols, trustExpectClasses) }
            ?: EqualsOverrideContract.SAFE_FOR_SMART_CAST
    }

    return when {
        isFinal -> getDeclaredEqualsOverrideContract(session, scopeSession, trustExpectClasses)
        isSealed -> minOf(
            EqualsOverrideContract.TRUSTED_FOR_EXHAUSTIVENESS, // We could leave `SAFE_FOR_SMART_CAST`, but we choose to be conservative.
            getDeclaredEqualsOverrideContract(session, scopeSession, trustExpectClasses),
            computeInheritorsContract(),
        )
        else -> EqualsOverrideContract.UNKNOWN
    }
}

private fun FirClassSymbol<*>.getDeclaredEqualsOverrideContract(
    session: FirSession,
    scopeSession: ScopeSession,
    trustExpectClasses: Boolean,
): EqualsOverrideContract {
    if (resolvedStatus.isExpect && !trustExpectClasses) return EqualsOverrideContract.UNKNOWN
    if (isSmartcastPrimitive(classId)) return EqualsOverrideContract.SAFE_FOR_SMART_CAST
    when (classId) {
        StandardClassIds.Any -> return EqualsOverrideContract.SAFE_FOR_SMART_CAST
        // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
        StandardClassIds.Float, StandardClassIds.Double -> return EqualsOverrideContract.UNKNOWN
        // kotlin.Enum has `equals()`, but we know it's reasonable
        StandardClassIds.Enum -> return EqualsOverrideContract.SAFE_FOR_SMART_CAST
    }

    // When the class belongs to a different module, "equals" contract might be changed without re-compilation
    // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
    // when different modules belong to a single project, so they're totally safe (see KT-50534)

    val ownerTag = this.toLookupTag()
    val declaredEquals = this.unsubstitutedScope(
        session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS
    ).getFunctions(OperatorNameConventions.EQUALS).find {
        !it.isSubstitutionOrIntersectionOverride && it.isEquals(session) && ownerTag.isRealOwnerOf(it)
    }

    // If the symbol comes from a dependency, we decide to trust that it's sane.
    // This is to avoid upsetting users who use carefully verified classes from a library,
    // and because we can no longer check its origin.
    val isTrustedDependency = (isData || isInlineOrValue || classKind.isObject) && moduleData != session.moduleData

    return when {
        // Strictly speaking, the default `equals()` of non-`data` objects, while safe for smartcasts,
        // should not be trusted for exhaustiveness: if another instance of the same object pops up at
        // runtime, it will not be equal to the instance denoted by the fqName of the object.
        // But we choose not to emit any diagnostic, because sometimes "just adding `data` to the object"
        // is not always possible, and we believe the probability of getting a second instance is low.
        // Also, `object`s are an example of why `EqualsOverrideContract` is not really a total order.
        declaredEquals == null -> EqualsOverrideContract.SAFE_FOR_SMART_CAST
        // We could conclude `SAFE_FOR_SMART_CAST` from `isGenerated`, but we choose to be conservative.
        declaredEquals.isGenerated || isTrustedDependency -> EqualsOverrideContract.TRUSTED_FOR_EXHAUSTIVENESS
        else -> EqualsOverrideContract.UNKNOWN
    }
}

/**
 * Determines if type smart-casting to the specified [org.jetbrains.kotlin.name.ClassId] can be performed when values are
 * compared via equality. Because this is determined using the ClassId, only standard built-in
 * types are considered.
 */
fun isSmartcastPrimitive(classId: ClassId?): Boolean {
    return when (classId) {
        // Support other primitives as well: KT-62246.
        StandardClassIds.String,
            -> true

        else -> false
    }
}

private val FirNamedFunctionSymbol.isGenerated: Boolean get() = origin.generatedAnyMethod
