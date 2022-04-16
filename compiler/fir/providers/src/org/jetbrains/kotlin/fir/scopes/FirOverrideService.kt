/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.buildSubstitutorForOverridesCheck
import org.jetbrains.kotlin.fir.scopes.impl.similarFunctionsOrBothProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

class FirOverrideService(val session: FirSession) : FirSessionComponent {
    fun <D : FirCallableSymbol<*>> selectMostSpecificInEachOverridableGroup(
        members: Collection<MemberWithBaseScope<D>>,
        overrideChecker: FirOverrideChecker,
        returnTypeCalculator: ReturnTypeCalculator
    ): Collection<MemberWithBaseScope<D>> {
        if (members.size <= 1) return members
        val queue = LinkedList(members)
        val result = SmartSet.create<MemberWithBaseScope<D>>()

        while (queue.isNotEmpty()) {
            val nextHandle = queue.first()

            val conflictedHandles = SmartSet.create<MemberWithBaseScope<D>>()

            val overridableGroup = extractBothWaysOverridable(nextHandle, queue, overrideChecker)

            if (overridableGroup.size == 1 && conflictedHandles.isEmpty()) {
                result.add(overridableGroup.single())
                continue
            }

            val mostSpecific = selectMostSpecificMember(overridableGroup, returnTypeCalculator)

            overridableGroup.filterNotTo(conflictedHandles) {
                isMoreSpecific(mostSpecific.member, it.member, returnTypeCalculator)
            }

            if (conflictedHandles.isNotEmpty()) {
                result.addAll(conflictedHandles)
            }

            result.add(mostSpecific)
        }
        return result
    }

    fun <D : FirCallableSymbol<*>> extractBothWaysOverridable(
        overrider: MemberWithBaseScope<D>,
        members: MutableCollection<MemberWithBaseScope<D>>,
        overrideChecker: FirOverrideChecker
    ): MutableList<MemberWithBaseScope<D>> {
        val result = mutableListOf<MemberWithBaseScope<D>>().apply { add(overrider) }

        val iterator = members.iterator()

        val overrideCandidate = overrider.member.fir
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == overrider) {
                iterator.remove()
                continue
            }

            if (overrideChecker.similarFunctionsOrBothProperties(overrideCandidate, next.member.fir)) {
                result.add(next)
                iterator.remove()
            }
        }

        return result
    }

    fun <D : FirCallableSymbol<*>> selectMostSpecificMember(
        overridables: Collection<MemberWithBaseScope<D>>,
        returnTypeCalculator: ReturnTypeCalculator
    ): MemberWithBaseScope<D> {
        require(overridables.isNotEmpty()) { "Should have at least one overridable symbol" }
        if (overridables.size == 1) {
            return overridables.first()
        }

        val candidates: MutableCollection<MemberWithBaseScope<D>> = ArrayList(2)
        var transitivelyMostSpecific: MemberWithBaseScope<D> = overridables.first()

        for (candidate in overridables) {
            if (overridables.all { isMoreSpecific(candidate.member, it.member, returnTypeCalculator) }) {
                candidates.add(candidate)
            }

            if (isMoreSpecific(candidate.member, transitivelyMostSpecific.member, returnTypeCalculator) &&
                !isMoreSpecific(transitivelyMostSpecific.member, candidate.member, returnTypeCalculator)
            ) {
                transitivelyMostSpecific = candidate
            }
        }

        return when {
            candidates.isEmpty() -> transitivelyMostSpecific
            candidates.size == 1 -> candidates.first()
            else -> {
                candidates.firstOrNull {
                    val type = it.member.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                    type != null && type !is ConeFlexibleType
                }?.let { return it }
                candidates.first()
            }
        }
    }

    private fun isMoreSpecific(
        a: FirCallableSymbol<*>,
        b: FirCallableSymbol<*>,
        returnTypeCalculator: ReturnTypeCalculator
    ): Boolean {
        val aFir = a.fir
        val bFir = b.fir

        if (!isVisibilityMoreSpecific(aFir.visibility, bFir.visibility)) return false

        val substitutor = buildSubstitutorForOverridesCheck(aFir, bFir, session) ?: return false
        // NB: these lines throw CCE in modularized tests when changed to just .coneType (FirImplicitTypeRef)
        val aReturnType = returnTypeCalculator.tryCalculateReturnTypeOrNull(a.fir)?.type?.let(substitutor::substituteOrSelf) ?: return false
        val bReturnType = returnTypeCalculator.tryCalculateReturnTypeOrNull(b.fir)?.type ?: return false

        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        if (aFir is FirSimpleFunction) {
            require(bFir is FirSimpleFunction) { "b is " + b.javaClass }
            return isTypeMoreSpecific(aReturnType, bReturnType, typeCheckerState)
        }
        if (aFir is FirProperty) {
            require(bFir is FirProperty) { "b is " + b.javaClass }

            if (!isAccessorMoreSpecific(aFir.setter, bFir.setter)) return false

            return if (aFir.isVar && bFir.isVar) {
                AbstractTypeChecker.equalTypes(typeCheckerState, aReturnType, bReturnType)
            } else { // both vals or var vs val: val can't be more specific then var
                !(!aFir.isVar && bFir.isVar) && isTypeMoreSpecific(aReturnType, bReturnType, typeCheckerState)
            }
        }
        throw IllegalArgumentException("Unexpected callable: " + a.javaClass)
    }

    private fun isTypeMoreSpecific(a: ConeKotlinType, b: ConeKotlinType, typeCheckerState: TypeCheckerState): Boolean =
        AbstractTypeChecker.isSubtypeOf(typeCheckerState, a, b)

    private fun isAccessorMoreSpecific(a: FirPropertyAccessor?, b: FirPropertyAccessor?): Boolean {
        if (a == null || b == null) return true
        return isVisibilityMoreSpecific(a.visibility, b.visibility)
    }

    private fun isVisibilityMoreSpecific(a: Visibility, b: Visibility): Boolean {
        val result = Visibilities.compare(a, b)
        return result == null || result >= 0
    }
}

val FirSession.overrideService: FirOverrideService by FirSession.sessionComponentAccessor()
