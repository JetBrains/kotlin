/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirIntersectionOverrideStorage.ContextForIntersectionOverrideConstruction
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

typealias MembersByScope<D> = List<Pair<FirTypeScope, List<D>>>

class FirTypeIntersectionScopeContext(
    val session: FirSession,
    private val overrideChecker: FirOverrideChecker,
    val scopes: List<FirTypeScope>,
    private val dispatchReceiverType: ConeSimpleKotlinType,
) {
    private val overrideService = session.overrideService

    val intersectionOverrides: FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ContextForIntersectionOverrideConstruction<*>> =
        session.intersectionOverrideStorage.cacheByScope.getValue(dispatchReceiverType).intersectionOverrides

    sealed class ResultOfIntersection<D : FirCallableSymbol<*>>(
        val overriddenMembers: List<MemberWithBaseScope<D>>,
        val containingScope: FirTypeScope?
    ) {
        abstract val chosenSymbol: D

        class SingleMember<D : FirCallableSymbol<*>>(
            override val chosenSymbol: D,
            overriddenMembers: List<MemberWithBaseScope<D>>,
            containingScope: FirTypeScope?
        ) : ResultOfIntersection<D>(overriddenMembers, containingScope) {
            constructor(
                chosenSymbol: D,
                overriddenMember: MemberWithBaseScope<D>
            ) : this(chosenSymbol, listOf(overriddenMember), overriddenMember.baseScope)
        }

        class NonTrivial<D : FirCallableSymbol<*>>(
            private val intersectionOverridesCache: FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ContextForIntersectionOverrideConstruction<*>>,
            private val context: ContextForIntersectionOverrideConstruction<D>,
            overriddenMembers: List<MemberWithBaseScope<D>>,
            containingScope: FirTypeScope?
        ) : ResultOfIntersection<D>(overriddenMembers, containingScope) {
            override val chosenSymbol: D by lazy {
                @Suppress("UNCHECKED_CAST")
                intersectionOverridesCache.getValue(
                    context.mostSpecific,
                    context
                ).member as D
            }

            val mostSpecific: D
                get() = context.mostSpecific
        }
    }

    fun processClassifiersByNameWithSubstitution(
        name: Name,
        absentClassifierNames: MutableSet<Name>,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        if (name in absentClassifierNames) return
        val classifiers = collectClassifiers(name)
        if (classifiers.isEmpty()) {
            absentClassifierNames += name
            return
        }
        for ((symbol, substitution) in classifiers) {
            processor(symbol, substitution)
        }
    }

    private fun collectClassifiers(name: Name): List<Pair<FirClassifierSymbol<*>, ConeSubstitutor>> {
        val accepted = HashSet<FirClassifierSymbol<*>>()
        val pending = mutableListOf<FirClassifierSymbol<*>>()
        val result = mutableListOf<Pair<FirClassifierSymbol<*>, ConeSubstitutor>>()
        for (scope in scopes) {
            scope.processClassifiersByNameWithSubstitution(name) { symbol, substitution ->
                if (symbol !in accepted) {
                    pending += symbol
                    result += symbol to substitution
                }
            }
            accepted += pending
            pending.clear()
        }
        return result
    }

    fun collectFunctions(name: Name): List<ResultOfIntersection<FirNamedFunctionSymbol>> {
        return collectCallables(name, FirScope::processFunctionsByName)
    }

    @OptIn(PrivateForInline::class)
    inline fun <D : FirCallableSymbol<*>> collectMembersByScope(
        name: Name,
        processCallables: FirScope.(Name, (D) -> Unit) -> Unit
    ): MembersByScope<D> {
        return scopes.mapNotNull { scope ->
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
    }

    @OptIn(PrivateForInline::class)
    inline fun <D : FirCallableSymbol<*>> collectCallables(
        name: Name,
        processCallables: FirScope.(Name, (D) -> Unit) -> Unit
    ): List<ResultOfIntersection<D>> {
        return collectCallablesImpl(collectMembersByScope(name, processCallables))
    }

    fun <D : FirCallableSymbol<*>> collectCallablesImpl(
        membersByScope: List<Pair<FirTypeScope, List<D>>>
    ): List<ResultOfIntersection<D>> {
        if (membersByScope.isEmpty()) {
            return emptyList()
        }

        membersByScope.singleOrNull()?.let { (scope, members) ->
            return members.map { ResultOfIntersection.SingleMember(it, MemberWithBaseScope(it, scope)) }
        }

        val allMembersWithScope = membersByScope.flatMapTo(linkedSetOf()) { (scope, members) ->
            members.map { MemberWithBaseScope(it, scope) }
        }

        val result = mutableListOf<ResultOfIntersection<D>>()

        while (allMembersWithScope.size > 1) {
            val maxByVisibility = findMemberWithMaxVisibility(allMembersWithScope)
            val extractBothWaysWithPrivate = overrideService.extractBothWaysOverridable(maxByVisibility, allMembersWithScope, overrideChecker)
            val extractedOverrides = extractBothWaysWithPrivate.filterNotTo(mutableListOf()) {
                Visibilities.isPrivate((it.member.fir as FirMemberDeclaration).visibility)
            }.takeIf { it.isNotEmpty() } ?: extractBothWaysWithPrivate
            val baseMembersForIntersection = extractedOverrides.calcBaseMembersForIntersectionOverride()
            if (baseMembersForIntersection.size > 1) {
                val (mostSpecific, scopeForMostSpecific) = overrideService.selectMostSpecificMember(
                    baseMembersForIntersection,
                    ReturnTypeCalculatorForFullBodyResolve
                )
                val intersectionOverrideContext = ContextForIntersectionOverrideConstruction(
                    mostSpecific,
                    this,
                    extractedOverrides,
                    scopeForMostSpecific
                )
                result += ResultOfIntersection.NonTrivial(
                    intersectionOverrides,
                    intersectionOverrideContext,
                    extractedOverrides,
                    containingScope = null
                )
            } else {
                val (mostSpecific, containingScope) = baseMembersForIntersection.single()
                result += ResultOfIntersection.SingleMember(mostSpecific, extractedOverrides, containingScope)
            }
        }

        if (allMembersWithScope.isNotEmpty()) {
            val (single, containingScope) = allMembersWithScope.single()
            result += ResultOfIntersection.SingleMember(single, allMembersWithScope.toList(), containingScope)
        }

        return result
    }

    fun <D : FirCallableSymbol<*>> createIntersectionOverride(
        extractedOverrides: List<MemberWithBaseScope<D>>,
        mostSpecific: D,
        scopeForMostSpecific: FirTypeScope
    ): MemberWithBaseScope<FirCallableSymbol<*>> {
        val newModality = chooseIntersectionOverrideModality(extractedOverrides)
        val newVisibility = chooseIntersectionVisibility(extractedOverrides)
        val extractedOverridesSymbols = extractedOverrides.map { it.member }
        return when (mostSpecific) {
            is FirNamedFunctionSymbol -> createIntersectionOverride(mostSpecific, extractedOverridesSymbols, newModality, newVisibility)
            is FirPropertySymbol -> createIntersectionOverride(mostSpecific, extractedOverridesSymbols, newModality, newVisibility)
            else -> throw IllegalStateException("Should not be here")
        }.withScope(scopeForMostSpecific)
    }

    private fun <S : FirCallableSymbol<*>> List<MemberWithBaseScope<S>>.calcBaseMembersForIntersectionOverride(): List<MemberWithBaseScope<S>> {
        if (size == 1) return this
        val unwrappedMemberSet = mutableSetOf<MemberWithBaseScope<S>>()
        for ((member, scope) in this) {
            @Suppress("UNCHECKED_CAST")
            unwrappedMemberSet += MemberWithBaseScope(member.fir.unwrapSubstitutionOverrides().symbol as S, scope)
        }
        // If in fact extracted overrides are the same symbols,
        // we should just take most specific member without creating intersection
        // A typical sample here is inheritance of the same class in different places of hierarchy
        if (unwrappedMemberSet.size == 1) {
            return listOf(overrideService.selectMostSpecificMember(this, ReturnTypeCalculatorForFullBodyResolve))
        }

        val baseMembers = mutableSetOf<S>()
        for ((member, scope) in this) {
            @Suppress("UNCHECKED_CAST")
            if (member is FirNamedFunctionSymbol) {
                scope.processOverriddenFunctions(member) {
                    val symbol = it.fir.unwrapSubstitutionOverrides().symbol
                    if (symbol != member.fir.unwrapSubstitutionOverrides().symbol) {
                        baseMembers += symbol as S
                    }
                    ProcessorAction.NEXT
                }
            } else if (member is FirPropertySymbol) {
                scope.processOverriddenProperties(member) {
                    val symbol = it.fir.unwrapSubstitutionOverrides().symbol
                    if (symbol != member.fir.unwrapSubstitutionOverrides().symbol) {
                        baseMembers += symbol as S
                    }
                    ProcessorAction.NEXT
                }
            }
        }

        val result = this.toMutableList()
        result.removeIf { (member, _) -> member.fir.unwrapSubstitutionOverrides().symbol in baseMembers }
        return result
    }

    private fun <D : FirCallableSymbol<*>> findMemberWithMaxVisibility(members: Collection<MemberWithBaseScope<D>>): MemberWithBaseScope<D> {
        assert(members.isNotEmpty())

        var member: MemberWithBaseScope<D>? = null
        for (candidate in members) {
            if (member == null) {
                member = candidate
                continue
            }

            val result = Visibilities.compare(
                member.member.fir.status.visibility,
                candidate.member.fir.status.visibility
            )
            if (result != null && result < 0) {
                member = candidate
            }
        }
        return member!!
    }

    private fun <D : FirCallableSymbol<*>> chooseIntersectionOverrideModality(
        extractedOverridden: Collection<MemberWithBaseScope<D>>
    ): Modality? {
        var hasOpen = false
        var hasAbstract = false

        for ((member) in extractedOverridden) {
            when ((member.fir as FirMemberDeclaration).modality) {
                Modality.FINAL -> return Modality.FINAL
                Modality.SEALED -> {
                    // Members should not be sealed. But, that will be reported as WRONG_MODIFIER_TARGET, and here we shouldn't raise an
                    // internal error. Instead, let the intersection override have the default modality: null.
                    return null
                }
                Modality.OPEN -> {
                    hasOpen = true
                }
                Modality.ABSTRACT -> {
                    hasAbstract = true
                }
                null -> {
                }
            }
        }

        if (hasAbstract && !hasOpen) return Modality.ABSTRACT
        if (!hasAbstract && hasOpen) return Modality.OPEN

        @Suppress("UNCHECKED_CAST")
        val processDirectOverridden: ProcessOverriddenWithBaseScope<D> = when (extractedOverridden.first().member) {
            is FirNamedFunctionSymbol -> FirTypeScope::processDirectOverriddenFunctionsWithBaseScope as ProcessOverriddenWithBaseScope<D>
            is FirPropertySymbol -> FirTypeScope::processDirectOverriddenPropertiesWithBaseScope as ProcessOverriddenWithBaseScope<D>
            else -> error("Unexpected callable kind: ${extractedOverridden.first().member}")
        }

        val realOverridden = extractedOverridden.flatMap { realOverridden(it.member, it.baseScope, processDirectOverridden) }
        val filteredOverridden = filterOutOverridden(realOverridden, processDirectOverridden)

        return filteredOverridden.minOf { (it.member.fir as FirMemberDeclaration).modality ?: Modality.ABSTRACT }
    }

    private fun <D : FirCallableSymbol<*>> realOverridden(
        symbol: D,
        scope: FirTypeScope,
        processDirectOverridden: ProcessOverriddenWithBaseScope<D>,
    ): Collection<MemberWithBaseScope<D>> {
        val result = mutableSetOf<MemberWithBaseScope<D>>()

        collectRealOverridden(symbol, scope, result, mutableSetOf(), processDirectOverridden)

        return result
    }

    private inline fun <reified D : FirCallableDeclaration> D.unwrapSubstitutionOverrides(): D {
        var current = this

        do {
            val next = current.originalForSubstitutionOverride ?: return current
            current = next
        } while (true)
    }

    private fun <D : FirCallableSymbol<*>> collectRealOverridden(
        symbol: D,
        scope: FirTypeScope,
        result: MutableCollection<MemberWithBaseScope<D>>,
        visited: MutableSet<D>,
        processDirectOverridden: FirTypeScope.(D, (D, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    ) {
        if (!visited.add(symbol)) return
        if (!symbol.fir.origin.fromSupertypes) {
            result.add(MemberWithBaseScope(symbol, scope))
            return
        }

        scope.processDirectOverridden(symbol) { overridden, baseScope ->
            collectRealOverridden(overridden, baseScope, result, visited, processDirectOverridden)
            ProcessorAction.NEXT
        }
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
        overrides: Collection<FirCallableSymbol<*>>,
        newModality: Modality?,
        newVisibility: Visibility,
    ): FirNamedFunctionSymbol {

        val newSymbol =
            FirIntersectionOverrideFunctionSymbol(
                CallableId(
                    dispatchReceiverType.classId ?: mostSpecific.dispatchReceiverClassOrNull()?.classId!!,
                    mostSpecific.fir.name
                ),
                overrides
            )
        val mostSpecificFunction = mostSpecific.fir
        FirFakeOverrideGenerator.createCopyForFirFunction(
            newSymbol,
            mostSpecificFunction, session, FirDeclarationOrigin.IntersectionOverride,
            mostSpecificFunction.isExpect,
            newDispatchReceiverType = dispatchReceiverType,
            newModality = newModality,
            newVisibility = newVisibility,
        ).apply {
            originalForIntersectionOverrideAttr = mostSpecific.fir
        }
        return newSymbol
    }

    private fun createIntersectionOverride(
        mostSpecific: FirPropertySymbol,
        overrides: Collection<FirCallableSymbol<*>>,
        newModality: Modality?,
        newVisibility: Visibility,
    ): FirPropertySymbol {
        val callableId = CallableId(
            dispatchReceiverType.classId ?: mostSpecific.dispatchReceiverClassOrNull()?.classId!!,
            mostSpecific.fir.name
        )
        val newSymbol = FirIntersectionOverridePropertySymbol(callableId, overrides)
        val mostSpecificProperty = mostSpecific.fir
        FirFakeOverrideGenerator.createCopyForFirProperty(
            newSymbol, mostSpecificProperty, session, FirDeclarationOrigin.IntersectionOverride,
            newModality = newModality,
            newVisibility = newVisibility,
            newDispatchReceiverType = dispatchReceiverType,
        ).apply {
            originalForIntersectionOverrideAttr = mostSpecific.fir
        }
        return newSymbol
    }
}

private fun <D : FirCallableSymbol<*>> D.withScope(baseScope: FirTypeScope) = MemberWithBaseScope(this, baseScope)

class FirIntersectionOverrideStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    class CacheForScope(cachesFactory: FirCachesFactory) {
        val intersectionOverrides: FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ContextForIntersectionOverrideConstruction<*>> =
            cachesFactory.createCache { mostSpecific, context ->
                val (_, intersectionScope, extractedOverrides, scopeForMostSpecific) = context
                intersectionScope.createIntersectionOverride(extractedOverrides, mostSpecific, scopeForMostSpecific)
            }
    }

    data class ContextForIntersectionOverrideConstruction<D : FirCallableSymbol<*>>(
        val mostSpecific: D,
        val intersectionContext: FirTypeIntersectionScopeContext,
        val extractedOverrides: List<MemberWithBaseScope<D>>,
        val scopeForMostSpecific: FirTypeScope
    )

    val cacheByScope: FirCache<ConeKotlinType, CacheForScope, Nothing?> =
        cachesFactory.createCache { _ -> CacheForScope(cachesFactory) }
}

private val FirSession.intersectionOverrideStorage: FirIntersectionOverrideStorage by FirSession.sessionComponentAccessor()

@OptIn(ExperimentalContracts::class)
fun <D : FirCallableSymbol<*>> ResultOfIntersection<D>.isIntersectionOverride(): Boolean {
    contract {
        returns(true) implies (this@isIntersectionOverride is ResultOfIntersection.NonTrivial<D>)
    }
    return this is ResultOfIntersection.NonTrivial
}
