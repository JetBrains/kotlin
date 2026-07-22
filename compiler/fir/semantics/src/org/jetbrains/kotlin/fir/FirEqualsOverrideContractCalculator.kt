/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.fir.EqualsOverrideContract.*
import org.jetbrains.kotlin.fir.declarations.utils.equalityBoundType
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability

/**
 * The entries are ordered from least trustworthy to most trustworthy.
 * `A` is more trustworthy than `B` iff the set of cases where we can rely on `A`
 * is a superset of that of `B`.
 *
 * The values determine how sure we are that implementation of `equals` which is called
 * for a given class in LHS is "primitive", where primitive means that `returns(true)` implies
 * that runtime types of LHS and RHS are the same. For example, final classes that don't override
 * `equals` from `Any` have primitive `equals`.
 *
 * Note that for primitve `equals` we can deduce both
 *  - `returns(true) implies (RHS is staticTypeOf(LHS))`
 *  - `returns(true) implies (LHS is staticTypeOf(RHS))`
 */
enum class EqualsOverrideContract {
    UNKNOWN,
    TRUSTED_FOR_EXHAUSTIVENESS,
    SAFE_FOR_SMART_CAST,
}

class FirEqualsOverrideContractCalculator(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
) : SessionAndScopeSessionHolder {

    fun computeFor(type: ConeKotlinType): EqualsOverrideContract {
        // the best contract over intersection components: one successful component is enough
        return collectSymbolsForType(type, session).maxOfOrNull { it.computeMinContractInSealedSubtree(mutableSetOf()) } ?: UNKNOWN
    }

    private fun FirClassSymbol<*>.computeMinContractInSealedSubtree(
        symbolsAlreadyCheckedForExpect: MutableSet<FirClassifierSymbol<*>>
    ): EqualsOverrideContract {
        return when {
            isFinal -> computeOwnContract(symbolsAlreadyCheckedForExpect)
            isSealed -> {
                val ownContract = computeOwnContract(symbolsAlreadyCheckedForExpect).also { if (it == UNKNOWN) return it }
                if (this !is FirRegularClassSymbol) return UNKNOWN

                val sealedInheritorsContract =
                    fir.getSealedClassInheritors(session).map {
                        it.toSymbol() as? FirClassSymbol<*> ?: return UNKNOWN
                    }.minOfOrNull { inheritor ->
                        inheritor.computeMinContractInSealedSubtree(symbolsAlreadyCheckedForExpect).also { if (it == UNKNOWN) return it }
                    }

                minOf(
                    TRUSTED_FOR_EXHAUSTIVENESS, // We could leave `SAFE_FOR_SMART_CAST`, but we choose to be conservative.
                    ownContract,
                    sealedInheritorsContract ?: SAFE_FOR_SMART_CAST,
                )
            }
            else -> UNKNOWN
        }
    }

    // Several notes:
    //
    // Strictly speaking, the default `equals()` of non-`data` objects, while safe for smartcasts,
    // should not be trusted for exhaustiveness: if another instance of the same object pops up at
    // runtime, it will not be equal to the instance denoted by the fqName of the object.
    // But we choose not to emit any diagnostic, because sometimes "just adding `data` to the object"
    // is not always possible, and we believe the probability of getting a second instance is low.
    // Also, `object`s are an example of why `EqualsOverrideContract` is not really a total order.
    //
    // When the class belongs to a different module, "equals" contract might be changed without re-compilation
    // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
    // when different modules belong to a single project, so they're totally safe (see KT-50534)
    private fun FirClassSymbol<*>.computeOwnContract(
        symbolsAlreadyCheckedForExpect: MutableSet<FirClassifierSymbol<*>>,
    ): EqualsOverrideContract {
        if (hasExpectSupertype(symbolsAlreadyCheckedForExpect)) return UNKNOWN
        if (isSmartcastPrimitive(classId)) return SAFE_FOR_SMART_CAST
        when (classId) {
            // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
            StandardClassIds.Float, StandardClassIds.Double -> return UNKNOWN
        }

        fun processor(function: FirNamedFunctionSymbol): EqualsOverrideContract {
            if (function.isSubstitutionOrIntersectionOverride) {
                return if (function is FirIntersectionCallableSymbol) {
                    function.intersections
                        .filterIsInstance<FirNamedFunctionSymbol>()
                        .minOfOrNull { processor(it) } ?: SAFE_FOR_SMART_CAST
                } else {
                    processor(function.unwrapSubstitutionOverrides())
                }
            }

            val declaringClassSymbol = function.getContainingClassSymbol() ?: return SAFE_FOR_SMART_CAST
            // kotlin.Enum has `equals()`, but we know it's reasonable
            if (declaringClassSymbol.classId == StandardClassIds.Any || declaringClassSymbol.classId == StandardClassIds.Enum) return SAFE_FOR_SMART_CAST
            if (declaringClassSymbol !is FirClassSymbol) return UNKNOWN

            return if (declaringClassSymbol.isInterface) {
                SAFE_FOR_SMART_CAST
            } else if (function.isGenerated || declaringClassSymbol.isTrustedDependency) {
                // 1. If the symbol comes from a dependency, we decide to trust that it's sane.
                // This is to avoid upsetting users who use carefully verified classes from a library,
                // and because we can no longer check its origin.
                // 2. We could conclude `SAFE_FOR_SMART_CAST` from `isGenerated`, but we choose to be conservative.
                TRUSTED_FOR_EXHAUSTIVENESS
            } else {
                UNKNOWN
            }
        }

        var minContract = SAFE_FOR_SMART_CAST

        unsubstitutedScope(withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS)
            .processFunctionsByName(OperatorNameConventions.EQUALS) {
                if (it.isEquals(session)) {
                    minContract = minOf(minContract, processor(it))
                }
            }

        return minContract
    }

    private fun FirClassSymbol<*>.hasExpectSupertype(symbolsAlreadyCheckedForExpect: MutableSet<FirClassifierSymbol<*>>): Boolean {
        val superTypes = lookupSuperTypes(
            listOf(this),
            lookupInterfaces = false,
            deep = true,
            session,
            substituteTypes = false,
            visitedSymbols = symbolsAlreadyCheckedForExpect,
        )
        symbolsAlreadyCheckedForExpect.add(this)
        if (isExpect) return true

        for (superType in superTypes) {
            if (superType.toClassSymbol()?.isExpect == true) return true
        }

        return false
    }

    private val FirNamedFunctionSymbol.isGenerated: Boolean get() = origin.generatedAnyMethod

    private val FirClassSymbol<*>.isTrustedDependency: Boolean
        get() = (isData || isInlineOrValue || classKind.isObject) && moduleData != session.moduleData

    fun computeTypeForEqualityBoundBasedContract(type: ConeKotlinType): ConeKotlinType? {
        if (LanguageFeature.StrictEquals.isDisabled()) return null

        var result: ConeKotlinType? = null
        type.scope(CallableCopyTypeCalculator.DoNothing, requiredMembersPhase = FirResolvePhase.STATUS)?.processFunctionsByName(
            OperatorNameConventions.EQUALS,
        ) { function ->
            // should only be a single such function
            if (function.isEquals(session)) {
                function.valueParameterSymbols.singleOrNull()?.equalityBoundType?.let {
                    result = it
                }
            }
        }
        // currently, flexible LHS results in nullable RHS
        return result?.withNullability(type.canBeNull(), session.typeContext)
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
