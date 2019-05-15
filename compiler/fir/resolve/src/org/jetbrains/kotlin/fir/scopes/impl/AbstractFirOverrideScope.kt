/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.cast

abstract class AbstractFirOverrideScope(val session: FirSession) : FirScope {
    //base symbol as key
    val overrides = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol?>()

    val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(a: ConeKotlinType, b: ConeKotlinType, substitution: ConeSubstitutor) =
        AbstractStrictEqualityTypeChecker.strictEqualTypes(context, substitution.substituteOrSelf(a), substitution.substituteOrSelf(b))

    private fun isEqualTypes(a: FirTypeRef, b: FirTypeRef, substitution: ConeSubstitutor) =
        isEqualTypes(a.cast<FirResolvedTypeRef>().type, b.cast<FirResolvedTypeRef>().type, substitution)

    private fun isOverriddenFunCheck(member: FirNamedFunction, self: FirNamedFunction): Boolean {
        if (member.valueParameters.size != self.valueParameters.size) return false
        if (member.typeParameters.size != self.typeParameters.size) return false

        val types = self.typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol, false)
        }
        val substitution = ConeSubstitutorByMap(member.typeParameters.map { it.symbol }.zip(types).toMap())
        if (!member.typeParameters.zip(self.typeParameters).all { (a, b) ->
                a.bounds.size == b.bounds.size &&
                        a.bounds.zip(b.bounds).all { (aBound, bBound) -> isEqualTypes(aBound, bBound, substitution) }
            }
        ) return false
        if (!sameReceivers(member.receiverTypeRef, self.receiverTypeRef, substitution)) return false

        return member.valueParameters.zip(self.valueParameters).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitution)
        }
    }

    private fun sameReceivers(memberTypeRef: FirTypeRef?, selfTypeRef: FirTypeRef?, substitution: ConeSubstitutor): Boolean {
        return when {
            memberTypeRef != null && selfTypeRef != null -> isEqualTypes(memberTypeRef, selfTypeRef, substitution)
            else -> memberTypeRef == null && selfTypeRef == null
        }
    }

    protected fun ConeCallableSymbol.isOverridden(seen: Set<ConeCallableSymbol>): ConeCallableSymbol? {
        if (overrides.containsKey(this)) return overrides[this]

        fun similarFunctionsOrBothProperties(declaration: FirCallableDeclaration, self: FirCallableDeclaration): Boolean {
            return when (declaration) {
                is FirNamedFunction -> self is FirNamedFunction && isOverriddenFunCheck(declaration, self)
                is FirConstructor -> false
                is FirProperty -> self is FirProperty && sameReceivers(
                    declaration.receiverTypeRef,
                    self.receiverTypeRef,
                    ConeSubstitutor.Empty // TODO
                )
                is FirField -> false
                else -> error("Unknown fir callable type: $declaration, $self")
            }
        }

        val self = (this as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration
        val overriding = seen.firstOrNull {
            val member = (it as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration
            self.modality != Modality.FINAL
                    && similarFunctionsOrBothProperties(member, self)
        } // TODO: two or more overrides for one fun?
        overrides[this] = overriding
        return overriding
    }

}