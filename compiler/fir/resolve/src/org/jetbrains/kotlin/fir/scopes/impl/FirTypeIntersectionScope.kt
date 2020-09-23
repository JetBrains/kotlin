/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import java.util.*
import kotlin.collections.HashSet

class FirTypeIntersectionScope private constructor(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    private val scopes: List<FirTypeScope>,
    private val derivedClassId: ClassId?,
) : AbstractFirOverrideScope(session, overrideChecker) {
    private val absentFunctions: MutableSet<Name> = mutableSetOf()
    private val absentProperties: MutableSet<Name> = mutableSetOf()
    private val absentClassifiers: MutableSet<Name> = mutableSetOf()

    private val typeContext = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = false, session)

    private val overriddenSymbols: MutableMap<FirCallableSymbol<*>, Collection<MemberWithBaseScope<out FirCallableSymbol<*>>>> =
        mutableMapOf()

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
            val resultForScope = mutableListOf<D>()
            scope.processCallables(name) {
                if (it !is FirConstructorSymbol) {
                    resultForScope.add(it)
                }
            }

            resultForScope.takeIf { it.isNotEmpty() }?.let {
                scope to it
            }
        }

        if (membersByScope.isEmpty()) {
            absentNames.add(name)
            return false
        }

        membersByScope.singleOrNull()?.let { (scope, members) ->
            for (member in members) {
                overriddenSymbols[member] = listOf(MemberWithBaseScope(member, scope))
                processor(member)
            }

            return false
        }

        val allMembersWithScope = membersByScope.flatMapTo(mutableListOf()) { (scope, members) ->
            members.map { MemberWithBaseScope(it, scope) }
        }

        while (allMembersWithScope.isNotEmpty()) {
            val maxByVisibility = findMemberWithMaxVisibility(allMembersWithScope)
            val extractBothWaysWithPrivate = extractBothWaysOverridable(maxByVisibility, allMembersWithScope)
            val extractedOverrides = extractBothWaysWithPrivate.filterNot {
                Visibilities.isPrivate((it.member.fir as FirMemberDeclaration).visibility)
            }.takeIf { it.isNotEmpty() } ?: extractBothWaysWithPrivate

            val (mostSpecific, scopeForMostSpecific) = selectMostSpecificMember(extractedOverrides)
            if (extractedOverrides.size > 1) {
                val intersectionOverride = intersectionOverrides.getOrPut(mostSpecific) {
                    val newModality = chooseIntersectionOverrideModality(extractedOverrides)
                    val newVisibility = chooseIntersectionVisibility(extractedOverrides)
                    @Suppress("UNCHECKED_CAST")
                    when (mostSpecific) {
                        is FirNamedFunctionSymbol -> {
                            createIntersectionOverride(mostSpecific, newModality, newVisibility)
                        }
                        is FirPropertySymbol -> {
                            createIntersectionOverride(mostSpecific, newModality, newVisibility)
                        }
                        else -> {
                            throw IllegalStateException("Should not be here")
                        }
                    }.withScope(scopeForMostSpecific)
                }
                overriddenSymbols[intersectionOverride.member] = extractedOverrides
                @Suppress("UNCHECKED_CAST")
                processor(intersectionOverride.member as D)
            } else {
                overriddenSymbols[mostSpecific] = extractedOverrides
                processor(mostSpecific)
            }
        }

        return true
    }

    private fun <D : FirCallableSymbol<*>> chooseIntersectionOverrideModality(
        extractedOverrides: Collection<MemberWithBaseScope<D>>
    ): Modality {
        return extractedOverrides.minOf { (it.member.fir as FirMemberDeclaration).modality ?: Modality.ABSTRACT }
    }

    private fun <D : FirCallableSymbol<*>> chooseIntersectionVisibility(
        extractedOverrides: Collection<MemberWithBaseScope<D>>
    ): Visibility {
        var maxVisibility: Visibility = Visibilities.Private
        for ((override) in extractedOverrides) {
            val visibility = (override.fir as FirMemberDeclaration).visibility
            // TODO: There is more complex logic at org.jetbrains.kotlin.resolve.OverridingUtil.resolveUnknownVisibilityForMember
            // TODO: and org.jetbrains.kotlin.resolve.OverridingUtil.findMaxVisibility
            val compare = Visibilities.compare(visibility, maxVisibility) ?: return Visibilities.DEFAULT_VISIBILITY
            if (compare > 0) {
                maxVisibility = visibility
            }
        }
        return maxVisibility
    }

    private fun createIntersectionOverride(
        mostSpecific: FirNamedFunctionSymbol,
        newModality: Modality,
        newVisibility: Visibility,
    ): FirNamedFunctionSymbol {
        val newSymbol =
            FirNamedFunctionSymbol(
                CallableId(derivedClassId ?: mostSpecific.callableId.classId!!, mostSpecific.fir.name),
                mostSpecific.isFakeOverride,
                mostSpecific,
                isIntersectionOverride = true
            )
        val mostSpecificFunction = mostSpecific.fir
        FirClassSubstitutionScope.createCopyForFirFunction(
            newSymbol,
            mostSpecificFunction, session, FirDeclarationOrigin.IntersectionOverride,
            mostSpecificFunction.isExpect,
            newModality = newModality,
            newVisibility = newVisibility,
        )
        return newSymbol
    }

    private fun createIntersectionOverride(
        mostSpecific: FirPropertySymbol,
        newModality: Modality,
        newVisibility: Visibility,
    ): FirPropertySymbol {
        val newSymbol = FirPropertySymbol(mostSpecific.callableId, mostSpecific.isFakeOverride, mostSpecific, isIntersectionOverride = true)
        val mostSpecificProperty = mostSpecific.fir
        FirClassSubstitutionScope.createCopyForFirProperty(
            newSymbol, mostSpecificProperty, mostSpecificProperty.session,
            newModality = newModality,
            newVisibility = newVisibility,
        )
        return newSymbol
    }

    private fun <D : FirCallableSymbol<*>> selectMostSpecificMember(overridables: Collection<MemberWithBaseScope<D>>): MemberWithBaseScope<D> {
        require(overridables.isNotEmpty()) { "Should have at least one overridable symbol" }
        if (overridables.size == 1) {
            return overridables.first()
        }

        val candidates: MutableCollection<MemberWithBaseScope<D>> = ArrayList(2)
        var transitivelyMostSpecific: MemberWithBaseScope<D> = overridables.first()

        for (candidate in overridables) {
            if (overridables.all { isMoreSpecific(candidate.member, it.member) }) {
                candidates.add(candidate)
            }

            if (isMoreSpecific(candidate.member, transitivelyMostSpecific.member) &&
                !isMoreSpecific(transitivelyMostSpecific.member, candidate.member)
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
        b: FirCallableSymbol<*>
    ): Boolean {
        val aFir = a.fir
        val bFir = b.fir
        if (aFir !is FirCallableMemberDeclaration<*> || bFir !is FirCallableMemberDeclaration<*>) return false

        val substitutor = buildSubstitutorForOverridesCheck(aFir, bFir) ?: return false
        // NB: these lines throw CCE in modularized tests when changed to just .coneType (FirImplicitTypeRef)
        val aReturnType = a.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.let(substitutor::substituteOrSelf) ?: return false
        val bReturnType = b.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false

        if (aFir is FirSimpleFunction) {
            require(bFir is FirSimpleFunction) { "b is " + b.javaClass }
            return isTypeMoreSpecific(aReturnType, bReturnType)
        }
        if (aFir is FirProperty) {
            require(bFir is FirProperty) { "b is " + b.javaClass }
            // TODO: if (!OverridingUtil.isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false
            return if (aFir.isVar && bFir.isVar) {
                AbstractTypeChecker.equalTypes(typeContext as AbstractTypeCheckerContext, aReturnType, bReturnType)
            } else { // both vals or var vs val: val can't be more specific then var
                !(!aFir.isVar && bFir.isVar) && isTypeMoreSpecific(aReturnType, bReturnType)
            }
        }
        throw IllegalArgumentException("Unexpected callable: " + a.javaClass)
    }

    private fun isTypeMoreSpecific(a: ConeKotlinType, b: ConeKotlinType): Boolean =
        AbstractTypeChecker.isSubtypeOf(typeContext as AbstractTypeCheckerContext, a, b)

    private fun <D : FirCallableSymbol<*>> findMemberWithMaxVisibility(members: Collection<MemberWithBaseScope<D>>): MemberWithBaseScope<D> {
        assert(members.isNotEmpty())

        var member: MemberWithBaseScope<D>? = null
        for (candidate in members) {
            if (member == null) {
                member = candidate
                continue
            }

            val result = Visibilities.compare(
                (member.member.fir as FirCallableMemberDeclaration<*>).status.visibility,
                (candidate.member.fir as FirCallableMemberDeclaration<*>).status.visibility
            )
            if (result != null && result < 0) {
                member = candidate
            }
        }
        return member!!
    }

    private fun <D : FirCallableSymbol<*>> extractBothWaysOverridable(
        overrider: MemberWithBaseScope<D>,
        members: MutableCollection<MemberWithBaseScope<D>>
    ): Collection<MemberWithBaseScope<D>> {
        val result = mutableListOf<MemberWithBaseScope<D>>().apply { add(overrider) }

        val iterator = members.iterator()

        val overrideCandidate = overrider.member.fir as FirCallableMemberDeclaration<*>
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == overrider) {
                iterator.remove()
                continue
            }

            if (similarFunctionsOrBothProperties(overrideCandidate, next.member.fir as FirCallableMemberDeclaration<*>)) {
                result.add(next)
                iterator.remove()
            }
        }

        return result
    }

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
            scopes: List<FirTypeScope>,
            derivedClassId: ClassId? = null,
        ): FirTypeScope {
            scopes.singleOrNull()?.let { return it }
            return FirTypeIntersectionScope(session, overrideChecker, scopes, derivedClassId)
        }
    }
}

private class MemberWithBaseScope<D : FirCallableSymbol<*>>(val member: D, private val baseScope: FirTypeScope) {
    operator fun component1() = member
    operator fun component2() = baseScope
}

private fun <D : FirCallableSymbol<*>> D.withScope(baseScope: FirTypeScope) = MemberWithBaseScope(this, baseScope)
