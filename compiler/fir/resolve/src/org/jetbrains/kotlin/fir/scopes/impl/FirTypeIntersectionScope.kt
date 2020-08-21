/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import java.util.*
import kotlin.collections.HashSet

class FirTypeIntersectionScope private constructor(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    private val scopes: List<FirTypeScope>,
) : AbstractFirOverrideScope(session, overrideChecker) {
    private val absentFunctions: MutableSet<Name> = mutableSetOf()
    private val absentProperties: MutableSet<Name> = mutableSetOf()
    private val absentClassifiers: MutableSet<Name> = mutableSetOf()

    private val typeContext = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = false, session)

    private val overriddenSymbols: MutableMap<FirCallableSymbol<*>, Collection<MemberWithBaseScope<out FirCallableSymbol<*>>>> = mutableMapOf()

    private val intersectionOverrides: MutableMap<FirCallableSymbol<*>, MemberWithBaseScope<out FirCallableSymbol<*>>> = mutableMapOf()

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        if (!processCallablesByName(name, processor, absentFunctions, FirScope::processFunctionsByName)) {
            super.processFunctionsByName(name, processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (!processCallablesByName(name, processor, absentProperties, FirScope::processPropertiesByName)) {
            super.processPropertiesByName(name, processor)
        }
    }

    private inline fun <D : FirCallableSymbol<*>> processCallablesByName(
        name: Name,
        noinline processor: (D) -> Unit,
        absentNames: MutableSet<Name>,
        processCallables: FirScope.(Name, (D) -> Unit) -> Unit
    ): Boolean {
        if (name in absentNames) {
            return false
        }

        val membersByScope = scopes.mapNotNull { scope ->
            val resultForScope = mutableListOf<MemberWithBaseScope<D>>()
            scope.processCallables(name) {
                if (it !is FirConstructorSymbol) {
                    resultForScope.add(MemberWithBaseScope(it, scope))
                }
            }
            resultForScope.takeIf { it.isNotEmpty() }
        }

        if (membersByScope.isEmpty()) {
            absentNames.add(name)
            return false
        }

        membersByScope.singleOrNull()?.let { members ->
            for (it in members) {
                overriddenSymbols[it.member] = listOf(it)
                processor(it.member)
            }

            return false
        }

        val allMembersWithScope = membersByScope.flatMapTo(LinkedList()) { it }

        while (allMembersWithScope.isNotEmpty()) {
            val extractedOverrides = extractAnyEquivalentSet(allMembersWithScope)
            while (extractedOverrides.isNotEmpty()) {
                val (maximal, group) = extractMostSpecificMember(extractedOverrides)
                val representative = group.singleOrNull() ?: intersectionOverrides.getOrPut(maximal.member) {
                    when (maximal.member) {
                        is FirNamedFunctionSymbol -> createIntersectionOverride(maximal.member)
                        is FirPropertySymbol -> createIntersectionOverride(maximal.member)
                        else -> throw IllegalStateException("Should not be here")
                    }.withScope(maximal.baseScope)
                }
                overriddenSymbols[representative.member] = group
                @Suppress("UNCHECKED_CAST")
                processor(representative.member as D)
            }
        }

        return true
    }

    private fun createIntersectionOverride(mostSpecific: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        val newSymbol =
            FirNamedFunctionSymbol(
                mostSpecific.callableId,
                mostSpecific.isFakeOverride,
                mostSpecific,
                isIntersectionOverride = true
            )
        val mostSpecificFunction = mostSpecific.fir
        createFunctionCopy(mostSpecific.fir, newSymbol).apply {
            resolvePhase = mostSpecificFunction.resolvePhase
            origin = FirDeclarationOrigin.IntersectionOverride
            typeParameters += mostSpecificFunction.typeParameters
            valueParameters += mostSpecificFunction.valueParameters
        }.build()
        return newSymbol
    }

    private fun createIntersectionOverride(mostSpecific: FirPropertySymbol): FirPropertySymbol {
        val newSymbol = FirPropertySymbol(mostSpecific.callableId, mostSpecific.isFakeOverride, mostSpecific, isIntersectionOverride = true)
        val mostSpecificProperty = mostSpecific.fir
        createPropertyCopy(mostSpecific.fir, newSymbol).apply {
            resolvePhase = mostSpecificProperty.resolvePhase
            origin = FirDeclarationOrigin.IntersectionOverride
            typeParameters += mostSpecificProperty.typeParameters
        }.build()
        return newSymbol
    }

    private inline fun <E, C : MutableCollection<E>> LinkedList<E>.extractToIf(target: C, crossinline predicate: (E) -> Boolean) =
        target.also { removeIf { predicate(it) && target.add(it) } }

    private fun <D : FirCallableSymbol<*>> extractAnyEquivalentSet(
        members: LinkedList<MemberWithBaseScope<D>>
    ): LinkedList<MemberWithBaseScope<D>> {
        val prototype = members.first()
        val prototypeFir = prototype.member.fir as FirCallableMemberDeclaration<*>
        return members.extractToIf(LinkedList<MemberWithBaseScope<D>>()) {
            it === prototype || similarFunctionsOrBothProperties(prototypeFir, it.member.fir as FirCallableMemberDeclaration<*>)
        }
    }

    private fun <D : FirCallableSymbol<*>> extractMostSpecificMember(
        members: LinkedList<MemberWithBaseScope<D>>
    ): Pair<MemberWithBaseScope<D>, List<MemberWithBaseScope<D>>> {
        val maximal = members.firstOrNull { candidate ->
            members.all { isMoreSpecificOrSame(candidate.member, it.member) || !isMoreSpecificOrSame(it.member, candidate.member) }
        } ?: throw AssertionError("isMoreSpecificOrSame does not define a valid <= operator: no maximal elements")
        val group = members.extractToIf(ArrayList<MemberWithBaseScope<D>>(2)) { isMoreSpecificOrSame(maximal.member, it.member) }
        return maximal to group
    }

    private fun isMoreSpecificOrSame(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): Boolean {
        if (a === b) return true

        val aFir = a.fir
        val bFir = b.fir
        if (aFir !is FirCallableMemberDeclaration<*> || bFir !is FirCallableMemberDeclaration<*>) return false

        val hasGreaterVisibility = (Visibilities.compare(aFir.status.visibility, bFir.status.visibility) ?: -1) >= 0
        if (!hasGreaterVisibility) return false

        val substitutor = buildSubstitutorForOverridesCheck(aFir, bFir) ?: return false
        // NB: these lines throw CCE in modularized tests when changed to just .coneType (FirImplicitTypeRef)
        val aReturnType = a.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.let(substitutor::substituteOrSelf) ?: return false
        val bReturnType = b.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false
        if (aReturnType is ConeFlexibleType && bReturnType !is ConeFlexibleType) return false

        if (aFir is FirSimpleFunction) {
            require(bFir is FirSimpleFunction) { "b is " + b.javaClass }
            val hasSupersetOfDefaults = aFir.valueParameters.withIndex().all { (i, param) ->
                param.defaultValue != null || bFir.valueParameters[i].defaultValue == null
            }
            return hasSupersetOfDefaults && isTypeMoreSpecificOrSame(aReturnType, bReturnType)
        }
        if (aFir is FirProperty) {
            require(bFir is FirProperty) { "b is " + b.javaClass }
            // TODO: if (!OverridingUtil.isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false
            return if (aFir.isVar && bFir.isVar) {
                AbstractTypeChecker.equalTypes(typeContext as AbstractTypeCheckerContext, aReturnType, bReturnType)
            } else { // both vals or var vs val: val can't be more specific then var
                !(!aFir.isVar && bFir.isVar) && isTypeMoreSpecificOrSame(aReturnType, bReturnType)
            }
        }
        throw IllegalArgumentException("Unexpected callable: " + a.javaClass)
    }

    private fun isTypeMoreSpecificOrSame(a: ConeKotlinType, b: ConeKotlinType): Boolean =
        AbstractTypeChecker.isSubtypeOf(typeContext as AbstractTypeCheckerContext, a, b)

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        if (name in absentClassifiers) {
            return
        }
        val accepted = HashSet<FirClassifierSymbol<*>>()
        val pending = mutableListOf<FirClassifierSymbol<*>>()
        var empty = true
        for (scope in scopes) {
            scope.processClassifiersByNameWithSubstitution(name) { symbol, substitution ->
                empty = false
                if (symbol !in accepted) {
                    pending += symbol
                    processor(symbol, substitution)
                }
            }
            accepted += pending
            pending.clear()
        }
        if (empty) {
            absentClassifiers += name
        }
        super.processClassifiersByNameWithSubstitution(name, processor)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S : FirCallableSymbol<*>> getDirectOverriddenSymbols(symbol: S): Collection<MemberWithBaseScope<S>> {
        val intersectionOverride = intersectionOverrides[symbol]
        val allDirectOverridden = overriddenSymbols[symbol].orEmpty() + intersectionOverride?.let {
            overriddenSymbols[it.member]
        }.orEmpty()
        return allDirectOverridden as Collection<MemberWithBaseScope<S>>
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenCallablesCallablesWithBaseScope(
            functionSymbol, processor,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenCallablesCallablesWithBaseScope(
            propertySymbol, processor,
            FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
        )

    private fun <D : FirCallableSymbol<*>> processDirectOverriddenCallablesCallablesWithBaseScope(
        callableSymbol: D,
        processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenInBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction
    ): ProcessorAction {
        for ((overridden, baseScope) in getDirectOverriddenSymbols(callableSymbol)) {
            if (overridden === callableSymbol) {
                if (!baseScope.processDirectOverriddenInBaseScope(callableSymbol, processor)) return ProcessorAction.STOP
            } else {
                if (!processor(overridden, baseScope)) return ProcessorAction.STOP
            }
        }

        return ProcessorAction.NEXT
    }

    override fun getCallableNames(): Set<Name> {
        return scopes.flatMapTo(mutableSetOf()) { it.getCallableNames() }
    }

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getClassifierNames() }
    }

    companion object {
        fun prepareIntersectionScope(
            session: FirSession,
            overrideChecker: FirOverrideChecker,
            scopes: List<FirTypeScope>
        ): FirTypeScope {
            scopes.singleOrNull()?.let { return it }
            return FirTypeIntersectionScope(session, overrideChecker, scopes)
        }
    }
}

private data class MemberWithBaseScope<D : FirCallableSymbol<*>>(val member: D, val baseScope: FirTypeScope)

private fun <D : FirCallableSymbol<*>> D.withScope(baseScope: FirTypeScope) = MemberWithBaseScope(this, baseScope)
