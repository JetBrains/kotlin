/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState

object OverrideCheckerUtils {

    /**
     * Types from substitution overrides may not be compatible with the types before substitution due to variance.
     * For example, there is `Enum<E>::getDeclaringClass()` that returns `Class<E>`, and we may create a SO
     * `MyEnum::getDeclaringClass()` returning `Class<MyEnum>`, but `Class<T>` is invariant w.r.t. `T`.
     * Or we can also create a substitution override for a final function.
     * Since substitution overrides are allowed to be incorrect overrides, they are skipped below.
     *
     * Other kinds of fake overrides may also be incorrect, but not to that extent, so we can
     * check them more granularly. See the relevant comments.
     */
    val checkedOrigins: Set<FirDeclarationOrigin> = [
        FirDeclarationOrigin.Source,
        FirDeclarationOrigin.Synthetic.DataClassMember,
        FirDeclarationOrigin.Delegated,
        FirDeclarationOrigin.IntersectionOverride,
    ]

    /**
     * @return the overridden symbol with a conflicting return type, if any
     */
    context(context: CheckerContext)
    fun checkReturnType(
        member: FirCallableSymbol<*>,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerState: TypeCheckerState,
    ): FirCallableSymbol<*>? {
        val overridingReturnType = member.resolvedReturnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (overridingReturnType is ConeErrorType) {
            return null
        }

        val bounds = overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it).coneType }

        for (it in bounds.indices) {
            val overriddenDeclaration = overriddenSymbols[it]

            val overriddenReturnType = bounds[it].substituteAllTypeParameters(member, overriddenDeclaration)

            val isReturnTypeOkForOverride =
                if (overriddenDeclaration is FirPropertySymbol && overriddenDeclaration.isVar)
                    AbstractTypeChecker.equalTypes(typeCheckerState, overridingReturnType, overriddenReturnType)
                else
                    AbstractTypeChecker.isSubtypeOf(typeCheckerState, overridingReturnType, overriddenReturnType)

            if (!isReturnTypeOkForOverride) {
                return overriddenDeclaration
            }
        }

        return null
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.substituteAllTypeParameters(
        overrideDeclaration: FirCallableSymbol<*>,
        baseDeclaration: FirCallableSymbol<*>
    ): ConeKotlinType {
        val overrideTypeParameters = overrideDeclaration.typeParameterSymbols
        if (overrideTypeParameters.isEmpty()) {
            return this
        }

        val baseTypeParameters = baseDeclaration.typeParameterSymbols

        val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val size = minOf(overrideTypeParameters.size, baseTypeParameters.size)

        for (it in 0 until size) {
            val to = overrideTypeParameters[it]
            val from = baseTypeParameters[it]

            map[from] = to.toConeType()
        }

        return substitutorByMap(map, context.session).substituteOrSelf(this)
    }
}
