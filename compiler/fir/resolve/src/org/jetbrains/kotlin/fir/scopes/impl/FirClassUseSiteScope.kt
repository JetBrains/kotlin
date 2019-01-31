/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMember
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast

class FirClassUseSiteScope(
    session: FirSession,
    private val superTypesScope: FirScope,
    private val declaredMemberScope: FirClassDeclaredMemberScope,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    //base symbol as key
    val overrides = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol?>()

    @Suppress("UNUSED_PARAMETER")
    private fun isSubtypeOf(subType: ConeKotlinType, superType: ConeKotlinType): Boolean {
        // TODO: introduce normal sub-typing
        return true
    }

    private fun isSubtypeOf(subType: FirType, superType: FirType) =
        isSubtypeOf(subType.cast<FirResolvedType>().type, superType.cast<FirResolvedType>().type)

    @Suppress("UNUSED_PARAMETER")
    private fun isEqualTypes(a: ConeKotlinType, b: ConeKotlinType): Boolean {
        // TODO: introduce normal type comparison
        return true
    }

    private fun isEqualTypes(a: FirType, b: FirType) = isEqualTypes(a.cast<FirResolvedType>().type, b.cast<FirResolvedType>().type)

    private fun isOverriddenFunCheck(member: FirNamedFunction, self: FirNamedFunction): Boolean {
        return member.valueParameters.size == self.valueParameters.size &&
                member.valueParameters.zip(self.valueParameters).all { (memberParam, selfParam) ->
                    isEqualTypes(memberParam.returnType, selfParam.returnType)
                }
    }

    private fun ConeCallableSymbol.isOverridden(seen: Set<ConeCallableSymbol>): ConeCallableSymbol? {
        if (overrides.containsKey(this)) return overrides[this]

        fun sameReceivers(memberType: FirType?, selfType: FirType?): Boolean {
            return when {
                memberType != null && selfType != null -> isEqualTypes(memberType, selfType)
                else -> memberType == null && selfType == null
            }
        }

        fun similarFunctionsOrBothProperties(member: FirCallableMember, self: FirCallableMember): Boolean {
            return when (member) {
                is FirNamedFunction -> self is FirNamedFunction && isOverriddenFunCheck(member, self)
                is FirConstructor -> false
                is FirProperty -> self is FirProperty
                else -> error("Unknown fir callable type: $member, $self")
            }
        }

        val self = (this as AbstractFirBasedSymbol<*>).fir as FirCallableMember
        val overriding = seen.firstOrNull {
            val member = (it as AbstractFirBasedSymbol<*>).fir as FirCallableMember
            member.isOverride && self.modality != Modality.FINAL
                    && sameReceivers(member.receiverType, self.receiverType)
                    && isSubtypeOf(member.returnType, self.returnType)
                    && similarFunctionsOrBothProperties(member, self)
        } // TODO: two or more overrides for one fun?
        overrides[this] = overriding
        return overriding
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val seen = mutableSetOf<ConeCallableSymbol>()
        if (!declaredMemberScope.processFunctionsByName(name) {
                seen += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processFunctionsByName(name) {

            val overriddenBy = it.isOverridden(seen)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
        val seen = mutableSetOf<ConeCallableSymbol>()
        if (!declaredMemberScope.processPropertiesByName(name) {
                seen += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processPropertiesByName(name) {

            val overriddenBy = it.isOverridden(seen)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
        }
    }
}


