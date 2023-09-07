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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

typealias MembersByScope<D> = List<Pair<FirTypeScope, List<D>>>

class FirTypeIntersectionScopeContext(
    val session: FirSession,
    private val overrideChecker: FirOverrideChecker,
    val scopes: List<FirTypeScope>,
    private val dispatchReceiverType: ConeSimpleKotlinType,
    private val forClassUseSiteScope: Boolean,
) {
    private val overrideService = session.overrideService

    private val dispatchClassSymbol: FirRegularClassSymbol? = dispatchReceiverType.toRegularClassSymbol(session)
    private val isReceiverClassExpect = dispatchClassSymbol?.isExpect == true

    val intersectionOverrides: FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ResultOfIntersection.NonTrivial<*>> =
        session.intersectionOverrideStorage.cacheByScope.getValue(dispatchReceiverType)

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
            val context: FirTypeIntersectionScopeContext,
            val mostSpecific: List<MemberWithBaseScope<D>>,
            overriddenMembers: List<MemberWithBaseScope<D>>,
            containingScope: FirTypeScope?
        ) : ResultOfIntersection<D>(overriddenMembers, containingScope) {
            override val chosenSymbol: D by lazy {
                @Suppress("UNCHECKED_CAST")
                context.intersectionOverrides.getValue(keySymbol, this).member as D
            }

            val keySymbol: D
                get() = mostSpecific.first().member
        }
    }

    fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        for ((symbol, substitution) in collectClassifiers(name)) {
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
        return collectIntersectionResultsForCallables(name, FirScope::processFunctionsByName)
    }

    inline fun <D : FirCallableSymbol<*>> collectMembersGroupedByScope(
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

    inline fun <D : FirCallableSymbol<*>> collectIntersectionResultsForCallables(
        name: Name,
        processCallables: FirScope.(Name, (D) -> Unit) -> Unit
    ): List<ResultOfIntersection<D>> {
        return convertGroupedCallablesToIntersectionResults(collectMembersGroupedByScope(name, processCallables))
    }

    fun <D : FirCallableSymbol<*>> convertGroupedCallablesToIntersectionResults(
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
            val groupWithInvisible =
                overrideService.extractBothWaysOverridable(allMembersWithScope.maxByVisibility(), allMembersWithScope, overrideChecker)
            val group = groupWithInvisible.filter { it.isVisible() }.ifEmpty { groupWithInvisible }
            val nonSubsumed = if (forClassUseSiteScope) group.nonSubsumed() else group
            val mostSpecific = overrideService.selectMostSpecificMembers(nonSubsumed, ReturnTypeCalculatorForFullBodyResolve.Default)
            val nonTrivial = if (forClassUseSiteScope) {
                // Create a non-trivial intersection override when the base methods come from different scopes,
                // even if one of them is more specific than the others, i.e. when there is more than one method that is not subsumed.
                // This is necessary for proper reporting of MANY_{IMPL,INTERFACES}_MEMBER_NOT_IMPLEMENTED diagnostics.
                //
                // It is also possible to have the opposite case (> 1 most specific member, but all members are from
                // the same base scope), but this means there are different instantiations of the same base class,
                // which should generally result in INCONSISTENT_TYPE_PARAMETER_VALUES errors.
                nonSubsumed.size > 1 &&
                        nonSubsumed.mapTo(mutableSetOf()) { it.member.fir.unwrapSubstitutionOverrides().symbol }.size > 1
            } else {
                // Create a non-trivial intersection override when return types should be intersected.
                mostSpecific.size > 1
            }
            if (nonTrivial) {
                // Only add non-subsumed members to list of overridden in intersection override.
                result += ResultOfIntersection.NonTrivial(this, mostSpecific, overriddenMembers = nonSubsumed, containingScope = null)
            } else {
                val (member, containingScope) = mostSpecific.first()
                result += ResultOfIntersection.SingleMember(member, group, containingScope)
            }
        }

        if (allMembersWithScope.isNotEmpty()) {
            val (single, containingScope) = allMembersWithScope.single()
            result += ResultOfIntersection.SingleMember(single, allMembersWithScope.toList(), containingScope)
        }

        return result
    }

    private fun MemberWithBaseScope<*>.isVisible(): Boolean {
        // Checking for private is not enough because package-private declarations can be hidden, too, if they're in a different package.
        val dispatchClassSymbol = dispatchClassSymbol ?: return true

        return session.visibilityChecker.isVisibleForOverriding(
            dispatchClassSymbol.moduleData,
            dispatchClassSymbol.classId.packageFqName,
            member.fir
        )
    }

    fun <D : FirCallableSymbol<*>> createIntersectionOverride(
        mostSpecific: List<MemberWithBaseScope<D>>,
        extractedOverrides: List<MemberWithBaseScope<D>>,
    ): MemberWithBaseScope<FirCallableSymbol<*>> {
        val newModality = chooseIntersectionOverrideModality(extractedOverrides)
        val newVisibility = chooseIntersectionVisibility(extractedOverrides)
        val mostSpecificSymbols = mostSpecific.map { it.member }
        val extractedOverridesSymbols = extractedOverrides.map { it.member }
        val key = mostSpecific.first()
        return when (key.member) {
            is FirNamedFunctionSymbol ->
                createIntersectionOverrideFunction(mostSpecificSymbols, extractedOverridesSymbols, newModality, newVisibility)

            is FirPropertySymbol ->
                createIntersectionOverrideProperty(mostSpecificSymbols, extractedOverridesSymbols, newModality, newVisibility)

            is FirFieldSymbol -> {
                if (forClassUseSiteScope) error("Can not create intersection override in class scope for field ${key.member}")
                createIntersectionOverrideField(mostSpecificSymbols, extractedOverridesSymbols, newModality, newVisibility)
            }

            else -> error("Unsupported symbol type for creating intersection overrides: ${key.member}")
        }.withScope(key.baseScope)
    }

    /**
     * A callable declaration D [subsumes](https://kotlinlang.org/spec/inheritance.html#matching-and-subsumption-of-declarations)
     * a callable declaration B if D overrides B.
     */
    private fun <S : FirCallableSymbol<*>> List<MemberWithBaseScope<S>>.nonSubsumed(): List<MemberWithBaseScope<S>> {
        val baseMembers = mutableSetOf<FirCallableSymbol<*>>()
        for ((member, scope) in this) {
            val unwrapped = member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>()
            val addIfDifferent = { it: FirCallableSymbol<*> ->
                val symbol = it.unwrapSubstitutionOverrides()
                if (symbol != unwrapped) {
                    baseMembers += symbol
                }
                ProcessorAction.NEXT
            }
            if (member is FirNamedFunctionSymbol) {
                scope.processOverriddenFunctions(member, addIfDifferent)
            } else if (member is FirPropertySymbol) {
                scope.processOverriddenProperties(member, addIfDifferent)
            }
        }
        return filter { it.member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() !in baseMembers }
    }

    private fun <S : FirCallableSymbol<*>> Collection<MemberWithBaseScope<S>>.maxByVisibility(): MemberWithBaseScope<S> {
        var member: MemberWithBaseScope<S>? = null
        for (candidate in this) {
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

    private fun <D : FirCallableSymbol<*>> collectRealOverridden(
        symbol: D,
        scope: FirTypeScope,
        result: MutableCollection<MemberWithBaseScope<D>>,
        // There's no guarantee that directOverridden(symbol) is strictly different
        // It might be the same instance that when being requested with a different/new scope would return next level of overridden
        visited: MutableSet<Pair<FirTypeScope, D>>,
        processDirectOverridden: FirTypeScope.(D, (D, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    ) {
        if (!visited.add(scope to symbol)) return
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

    private fun createIntersectionOverrideFunction(
        mostSpecific: Collection<FirCallableSymbol<*>>,
        overrides: Collection<FirCallableSymbol<*>>,
        newModality: Modality?,
        newVisibility: Visibility,
    ): FirNamedFunctionSymbol {
        val key = mostSpecific.first() as FirNamedFunctionSymbol
        val keyFir = key.fir
        val callableId = CallableId(
            dispatchReceiverType.classId ?: keyFir.dispatchReceiverClassLookupTagOrNull()?.classId!!,
            keyFir.name
        )
        val newSymbol = FirIntersectionOverrideFunctionSymbol(callableId, overrides)
        FirFakeOverrideGenerator.createCopyForFirFunction(
            newSymbol, keyFir, derivedClassLookupTag = null, session,
            FirDeclarationOrigin.IntersectionOverride,
            isExpect = isReceiverClassExpect || keyFir.isExpect,
            newModality = newModality,
            newVisibility = newVisibility,
            newDispatchReceiverType = dispatchReceiverType,
            newReturnType = if (!forClassUseSiteScope) intersectReturnTypes(mostSpecific) else null,
        ).apply {
            originalForIntersectionOverrideAttr = keyFir
        }
        return newSymbol
    }

    private fun createIntersectionOverrideProperty(
        mostSpecific: Collection<FirCallableSymbol<*>>,
        overrides: Collection<FirCallableSymbol<*>>,
        newModality: Modality?,
        newVisibility: Visibility,
    ): FirPropertySymbol {
        return createIntersectionOverrideVariable<FirPropertySymbol, _>(
            mostSpecific,
            overrides,
            ::FirIntersectionOverridePropertySymbol,
        ) { symbol, fir, returnType ->
            FirFakeOverrideGenerator.createCopyForFirProperty(
                symbol, fir, derivedClassLookupTag = null, session,
                FirDeclarationOrigin.IntersectionOverride,
                isExpect = isReceiverClassExpect || fir.isExpect,
                newModality = newModality,
                newVisibility = newVisibility,
                newDispatchReceiverType = dispatchReceiverType,
                // If any of the properties are vars and the types are not equal, these declarations are conflicting
                // anyway and their uses should result in an overload resolution error.
                newReturnType = returnType
            )
        }
    }

    private fun createIntersectionOverrideField(
        mostSpecific: Collection<FirCallableSymbol<*>>,
        overrides: Collection<FirCallableSymbol<*>>,
        newModality: Modality?,
        newVisibility: Visibility,
    ): FirFieldSymbol {
        return createIntersectionOverrideVariable<FirFieldSymbol, _>(
            mostSpecific,
            overrides,
            ::FirIntersectionOverrideFieldSymbol
        ) { symbol, fir, returnType ->
            FirFakeOverrideGenerator.createCopyForFirField(
                symbol, fir, derivedClassLookupTag = null, session,
                FirDeclarationOrigin.IntersectionOverride,
                isExpect = isReceiverClassExpect || fir.isExpect,
                newModality = newModality,
                newVisibility = newVisibility,
                newDispatchReceiverType = dispatchReceiverType,
                // If any of the properties are vars and the types are not equal, these declarations are conflicting
                // anyway and their uses should result in an overload resolution error.
                newReturnType = returnType
            )
        }
    }

    private inline fun <reified S : FirVariableSymbol<F>, F : FirVariable> createIntersectionOverrideVariable(
        mostSpecific: Collection<FirCallableSymbol<*>>,
        overrides: Collection<FirCallableSymbol<*>>,
        createIntersectionOverrideSymbol: (CallableId, Collection<FirCallableSymbol<*>>) -> S,
        createCopy: (S, F, returnType: ConeKotlinType?) -> F
    ): S {
        val key = mostSpecific.first() as S
        val keyFir = key.fir
        val callableId = CallableId(dispatchReceiverType.classId ?: keyFir.dispatchReceiverClassLookupTagOrNull()?.classId!!, keyFir.name)
        val newSymbol = createIntersectionOverrideSymbol(callableId, overrides)
        val newReturnType = runIf(!forClassUseSiteScope && mostSpecific.none { (it as FirVariableSymbol<*>).fir.isVar }) {
            intersectReturnTypes(mostSpecific)
        }
        createCopy(newSymbol, keyFir, newReturnType).apply {
            originalForIntersectionOverrideAttr = keyFir
        }
        return newSymbol
    }

    private fun intersectReturnTypes(overrides: Collection<FirCallableSymbol<*>>): ConeKotlinType? {
        val key = overrides.first()
        // Remap type parameters to the first declaration's:
        //   (fun <A, B> foo(): B) & (fun <C, D> foo(): D?) -> (fun <A, B> foo(): B & B?)
        val substituted = overrides.mapNotNull {
            val returnType = it.fir.returnTypeRef.coneTypeSafe<ConeKotlinType>()
            if (it == key) return@mapNotNull returnType
            val substitutor = buildSubstitutorForOverridesCheck(it.fir, key.fir, session) ?: return@mapNotNull null
            returnType?.let(substitutor::substituteOrSelf)
        }
        return if (substituted.isNotEmpty()) session.typeContext.intersectTypes(substituted) else null
    }
}

private fun <D : FirCallableSymbol<*>> D.withScope(baseScope: FirTypeScope) = MemberWithBaseScope(this, baseScope)

typealias FirIntersectionOverrideCache =
        FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ResultOfIntersection.NonTrivial<*>>

class FirIntersectionOverrideStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val cacheByScope: FirCache<ConeKotlinType, FirIntersectionOverrideCache, Nothing?> =
        cachesFactory.createCache { _ ->
            cachesFactory.createCache { _, result ->
                result.context.createIntersectionOverride(result.mostSpecific, result.overriddenMembers)
            }
        }
}

private val FirSession.intersectionOverrideStorage: FirIntersectionOverrideStorage by FirSession.sessionComponentAccessor()

@OptIn(ExperimentalContracts::class)
fun <D : FirCallableSymbol<*>> ResultOfIntersection<D>.isIntersectionOverride(): Boolean {
    contract {
        returns(true) implies (this@isIntersectionOverride is ResultOfIntersection.NonTrivial<D>)
    }
    return this is ResultOfIntersection.NonTrivial
}
