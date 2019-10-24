/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.cast

abstract class AbstractFirOverrideScope(val session: FirSession) : FirScope() {
    //base symbol as key, overridden as value
    val overrideByBase = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>?>()

    protected val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(a: ConeKotlinType, b: ConeKotlinType, substitution: ConeSubstitutor) =
        AbstractStrictEqualityTypeChecker.strictEqualTypes(context, substitution.substituteOrSelf(a), substitution.substituteOrSelf(b))

    private fun isEqualTypes(a: FirTypeRef, b: FirTypeRef, substitution: ConeSubstitutor) =
        isEqualTypes(a.cast<FirResolvedTypeRef>().type, b.cast<FirResolvedTypeRef>().type, substitution)

    protected open fun isOverriddenFunCheck(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        if (overrideCandidate.valueParameters.size != baseDeclaration.valueParameters.size) return false
        if (overrideCandidate.typeParameters.size != baseDeclaration.typeParameters.size) return false

        val types = baseDeclaration.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
        }
        val substitution = substitutorByMap(overrideCandidate.typeParameters.map { it.symbol }.zip(types).toMap())
        if (!overrideCandidate.typeParameters.zip(baseDeclaration.typeParameters).all { (a, b) ->
                a.bounds.size == b.bounds.size &&
                        a.bounds.zip(b.bounds).all { (aBound, bBound) -> isEqualTypes(aBound, bBound, substitution) }
            }
        ) return false
        if (!sameReceivers(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitution)) return false

        return overrideCandidate.valueParameters.zip(baseDeclaration.valueParameters).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitution)
        }
    }

    protected open fun isOverriddenPropertyCheck(
        overrideCandidate: FirCallableMemberDeclaration<*>, // NB: in Java it can be a function which overrides accessor
        baseDeclaration: FirProperty
    ): Boolean {
        // TODO: substitutor
        return overrideCandidate is FirProperty &&
                sameReceivers(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, ConeSubstitutor.Empty)
    }

    private fun sameReceivers(memberTypeRef: FirTypeRef?, selfTypeRef: FirTypeRef?, substitution: ConeSubstitutor): Boolean {
        return when {
            memberTypeRef != null && selfTypeRef != null -> isEqualTypes(memberTypeRef, selfTypeRef, substitution)
            else -> memberTypeRef == null && selfTypeRef == null
        }
    }

    private fun similarFunctionsOrBothProperties(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): Boolean {
        return when (overrideCandidate) {
            is FirSimpleFunction -> when (baseDeclaration) {
                is FirSimpleFunction -> isOverriddenFunCheck(overrideCandidate, baseDeclaration)
                is FirProperty -> isOverriddenPropertyCheck(overrideCandidate, baseDeclaration)
                else -> false
            }
            is FirConstructor -> false
            is FirProperty -> baseDeclaration is FirProperty && isOverriddenPropertyCheck(overrideCandidate, baseDeclaration)
            is FirField -> false
            else -> error("Unknown fir callable type: $overrideCandidate, $baseDeclaration")
        }
    }

    // Receiver is super-type function here
    protected open fun FirCallableSymbol<*>.getOverridden(overrideCandidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        if (overrideByBase.containsKey(this)) return overrideByBase[this]

        val baseDeclaration = (this as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
        val override = overrideCandidates.firstOrNull {
            val overrideCandidate = (it as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
            baseDeclaration.modality != Modality.FINAL && similarFunctionsOrBothProperties(overrideCandidate, baseDeclaration)
        } // TODO: two or more overrides for one fun?
        overrideByBase[this] = override
        return override
    }

}