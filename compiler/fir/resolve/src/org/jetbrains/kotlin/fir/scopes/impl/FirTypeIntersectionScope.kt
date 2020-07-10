/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
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

    private val overriddenSymbols: MutableMap<FirCallableSymbol<*>, Collection<FirCallableSymbol<*>>> = mutableMapOf()

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

            resultForScope.takeIf { it.isNotEmpty() }
        }

        if (membersByScope.isEmpty()) {
            absentNames.add(name)
            return false
        }

        membersByScope.singleOrNull()?.let { members ->
            for (member in members) {
                overriddenSymbols[member] = listOf(member)
                processor(member)
            }

            return false
        }

        val allMembers = membersByScope.flattenTo(LinkedList())

        while (allMembers.isNotEmpty()) {
            val maxByVisibility = findMemberWithMaxVisibility(allMembers)
            val extractedOverrides = extractBothWaysOverridable(maxByVisibility, allMembers)

            val mostSpecific = selectMostSpecificMember(extractedOverrides)
            overriddenSymbols[mostSpecific] = extractedOverrides
            processor(mostSpecific)
        }

        return true
    }

    private fun <D : FirCallableSymbol<*>> selectMostSpecificMember(overridables: Collection<D>): D {
        require(overridables.isNotEmpty()) { "Should have at least one overridable symbol" }
        if (overridables.size == 1) {
            return overridables.first()
        }

        val candidates: MutableCollection<D> = ArrayList(2)
        var transitivelyMostSpecific: D = overridables.first()

        for (candidate in overridables) {
            if (overridables.all { isMoreSpecific(candidate, it) }) {
                candidates.add(candidate)
            }

            if (isMoreSpecific(candidate, transitivelyMostSpecific) && !isMoreSpecific(transitivelyMostSpecific, candidate)) {
                transitivelyMostSpecific = candidate
            }
        }

        return when {
            candidates.isEmpty() -> transitivelyMostSpecific
            candidates.size == 1 -> candidates.first()
            else -> {
                candidates.firstOrNull {
                    val type = it.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>()
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

    private fun <D : FirCallableSymbol<*>> findMemberWithMaxVisibility(members: Collection<D>): D {
        assert(members.isNotEmpty())

        var member: D? = null
        for (candidate in members) {
            if (member == null) {
                member = candidate
                continue
            }

            val result = Visibilities.compare(
                (member.fir as FirCallableMemberDeclaration<*>).status.visibility,
                (candidate.fir as FirCallableMemberDeclaration<*>).status.visibility
            )
            if (result != null && result < 0) {
                member = candidate
            }
        }
        return member!!
    }

    private fun <D : FirCallableSymbol<*>> extractBothWaysOverridable(
        overrider: D,
        members: MutableCollection<D>
    ): Collection<D> {
        val result = mutableListOf<D>().apply { add(overrider) }

        val iterator = members.iterator()

        val overrideCandidate = overrider.fir as FirCallableMemberDeclaration<*>
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == overrider) {
                iterator.remove()
                continue
            }

            if (similarFunctionsOrBothProperties(overrideCandidate, next.fir as FirCallableMemberDeclaration<*>)) {
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

    override fun processOverriddenFunctionsWithDepth(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
    ): ProcessorAction {
        @Suppress("UNCHECKED_CAST")
        val directOverriddenSymbols =
            overriddenSymbols[functionSymbol] as Collection<FirFunctionSymbol<*>>?
                ?: return ProcessorAction.NEXT

        for (directOverridden in directOverriddenSymbols) {
            // TODO: Preserve the scope where directOverridden came from
            for (scope in scopes) {
                if (!processor(directOverridden, 0)) return ProcessorAction.STOP
                if (!scope.processOverriddenFunctionsWithDepth(directOverridden) { symbol, depth ->
                        processor(symbol, depth)
                    }
                ) return ProcessorAction.STOP
            }
        }

        return ProcessorAction.NEXT
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
