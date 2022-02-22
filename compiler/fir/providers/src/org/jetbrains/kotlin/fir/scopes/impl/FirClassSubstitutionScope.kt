/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSessionKey
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.FakeOverrideSubstitution
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirTypeScope,
    key: ScopeSessionKey<*, *>,
    private val substitutor: ConeSubstitutor,
    private val dispatchReceiverTypeForSubstitutedMembers: ConeClassLikeType,
    private val skipPrivateMembers: Boolean,
    private val makeExpect: Boolean = false
) : FirTypeScope() {
    companion object {
        private val FirVariableSymbol<*>.isOverridable: Boolean
            get() = when (this) {
                is FirPropertySymbol,
                is FirFieldSymbol,
                is FirSyntheticPropertySymbol -> true
                else -> false
            }
    }

    private val substitutionOverrideCache = session.substitutionOverrideStorage.substitutionOverrideCacheByScope.getValue(key, null)
    private val newOwnerClassId = dispatchReceiverTypeForSubstitutedMembers.lookupTag.classId

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val function = substitutionOverrideCache.overridesForFunctions.getValue(original, this)
            processor(function)
        }

        return super.processFunctionsByName(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            functionSymbol,
            processor,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope,
        ) { it in substitutionOverrideCache.overridesForFunctions }

    private inline fun <reified D : FirCallableSymbol<*>> processDirectOverriddenWithBaseScope(
        callableSymbol: D,
        noinline processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction,
        originalInCache: (D) -> Boolean
    ): ProcessorAction {
        val original = callableSymbol.originalForSubstitutionOverride?.takeIf { originalInCache(it) }
            ?: return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(callableSymbol, processor)

        if (original != callableSymbol) {
            if (!processor(original, useSiteMemberScope)) return ProcessorAction.STOP
        }

        return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(original, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            val symbol = if (original.isOverridable) {
                substitutionOverrideCache.overridesForVariables.getValue(original, this)
            } else {
                original
            }
            processor(symbol)
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            propertySymbol, processor, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
        ) { it in substitutionOverrideCache.overridesForVariables }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            processor(symbol, substitutor.chain(this.substitutor))
        }
    }

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun ConeKotlinType.substitute(substitutor: ConeSubstitutor): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun ConeSimpleKotlinType.substitute(substitutor: ConeSubstitutor): ConeSimpleKotlinType? {
        return substitutor.substituteOrNull(this) as ConeSimpleKotlinType?
    }

    fun createSubstitutionOverrideFunction(original: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val symbolForOverride = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(original, newOwnerClassId)

        val (newTypeParameters, newDispatchReceiverType, newReceiverType, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(
            member, symbolForOverride
        )
        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReceiverType == null &&
            newReturnType == null &&
            newParameterTypes.all { it == null } &&
            newTypeParameters === member.typeParameters &&
            fakeOverrideSubstitution == null
        ) {
            if (original.dispatchReceiverType?.substitute(substitutor) != null) {
                return FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
                    session,
                    symbolForOverride,
                    member,
                    newDispatchReceiverType ?: dispatchReceiverTypeForSubstitutedMembers,
                    isExpect = makeExpect,
                )
            }
            return original
        }

        /*
         * Member functions can't capture type parameters, so
         *   it's safe to cast newTypeParameters to List<FirTypeParameter>
         */
        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
            session,
            symbolForOverride,
            member,
            newDispatchReceiverType ?: dispatchReceiverTypeForSubstitutedMembers,
            newReceiverType,
            newReturnType,
            newParameterTypes,
            newTypeParameters as List<FirTypeParameter>,
            makeExpect,
            fakeOverrideSubstitution
        )
    }

    fun createSubstitutionOverrideConstructor(original: FirConstructorSymbol): FirConstructorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val constructor = original.fir

        val symbolForOverride = FirConstructorSymbol(original.callableId)
        val (newTypeParameters, _, _, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(constructor, symbolForOverride)

        // If constructor has a dispatch receiver, it should be an inner class' constructor.
        // It means that we need to substitute its dispatcher as every other type,
        // instead of using dispatchReceiverTypeForSubstitutedMembers
        val newDispatchReceiverType = original.dispatchReceiverType?.substitute(substitutor)

        val newParameterTypes = constructor.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReturnType == null && newParameterTypes.all { it == null } && newTypeParameters === constructor.typeParameters) {
            return original
        }

        return FirFakeOverrideGenerator.createCopyForFirConstructor(
            symbolForOverride,
            session,
            constructor,
            FirDeclarationOrigin.SubstitutionOverride,
            newDispatchReceiverType,
            newReturnType,
            newParameterTypes,
            newTypeParameters,
            makeExpect,
            fakeOverrideSubstitution
        ).symbol
    }

    fun createSubstitutionOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val symbolForOverride = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(original, newOwnerClassId)

        val (newTypeParameters, newDispatchReceiverType, newReceiverType, newReturnType, _, fakeOverrideSubstitution) = createSubstitutedData(
            member, symbolForOverride
        )

        if (newReceiverType == null &&
            newReturnType == null &&
            newTypeParameters === member.typeParameters &&
            fakeOverrideSubstitution == null
        ) {
            if (original.dispatchReceiverType?.substitute(substitutor) != null) {
                return FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
                    session,
                    symbolForOverride,
                    member,
                    newDispatchReceiverType ?: dispatchReceiverTypeForSubstitutedMembers,
                    isExpect = makeExpect,
                )
            }
            return original
        }

        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
            session,
            symbolForOverride,
            member,
            newDispatchReceiverType ?: dispatchReceiverTypeForSubstitutedMembers,
            newReceiverType,
            newReturnType,
            newTypeParameters as List<FirTypeParameter>,
            makeExpect,
            fakeOverrideSubstitution
        )
    }

    private data class SubstitutedData(
        val typeParameters: List<FirTypeParameterRef>,
        val dispatchReceiverType: ConeSimpleKotlinType?,
        val receiverType: ConeKotlinType?,
        val returnType: ConeKotlinType?,
        val substitutor: ConeSubstitutor,
        val fakeOverrideSubstitution: FakeOverrideSubstitution?
    )

    private fun createSubstitutedData(member: FirCallableDeclaration, symbolForOverride: FirBasedSymbol<*>): SubstitutedData {
        member.ensureResolved(FirResolvePhase.TYPES)
        val (newTypeParameters, substitutor) = FirFakeOverrideGenerator.createNewTypeParametersAndSubstitutor(
            session,
            member as FirTypeParameterRefsOwner,
            symbolForOverride,
            substitutor,
            forceTypeParametersRecreation = dispatchReceiverTypeForSubstitutedMembers.lookupTag != member.dispatchReceiverClassOrNull()
        )

        val receiverType = member.receiverTypeRef?.coneType
        val newReceiverType = receiverType?.substitute(substitutor)

        val newDispatchReceiverType = dispatchReceiverTypeForSubstitutedMembers.substitute(substitutor)

        member.ensureResolved(FirResolvePhase.STATUS)
        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val fakeOverrideSubstitution = runIf(returnType == null) { FakeOverrideSubstitution(substitutor, member.symbol) }
        val newReturnType = returnType?.substitute(substitutor)
        return SubstitutedData(
            newTypeParameters,
            newDispatchReceiverType,
            newReceiverType,
            newReturnType,
            substitutor,
            fakeOverrideSubstitution
        )
    }

    fun createSubstitutionOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        member.symbol.ensureResolved(FirResolvePhase.STATUS)
        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        // TODO: do we have fields with implicit type?
        val newReturnType = returnType?.substitute() ?: return original

        return FirFakeOverrideGenerator.createSubstitutionOverrideField(session, member, original, newReturnType, newOwnerClassId)
    }

    fun createSubstitutionOverrideSyntheticProperty(original: FirSyntheticPropertySymbol): FirSyntheticPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir as FirSyntheticProperty
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        member.symbol.ensureResolved(FirResolvePhase.STATUS)
        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val fakeOverrideSubstitution = runIf(returnType == null) { FakeOverrideSubstitution(substitutor, original) }
        val newReturnType = returnType?.substitute()

        val newGetterParameterTypes = member.getter.valueParameters.map {
            it.returnTypeRef.coneType.substitute()
        }
        val newSetterParameterTypes = member.setter?.valueParameters?.map {
            it.returnTypeRef.coneType.substitute()
        }.orEmpty()

        if (original.dispatchReceiverType?.substitute(substitutor) == null &&
            newReturnType == null &&
            newGetterParameterTypes.all { it == null } &&
            newSetterParameterTypes.all { it == null }
        ) {
            return original
        }

        return FirFakeOverrideGenerator.createSubstitutionOverrideSyntheticProperty(
            session,
            member,
            original,
            substitutor.substituteOrSelf(dispatchReceiverTypeForSubstitutedMembers) as ConeSimpleKotlinType?,
            newReturnType,
            newGetterParameterTypes,
            newSetterParameterTypes,
            fakeOverrideSubstitution
        )
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val constructor = substitutionOverrideCache.overridesForConstructors.getValue(original, this)
            processor(constructor)
        }
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }

    override fun toString(): String {
        return "Substitution scope for [$useSiteMemberScope] for type $dispatchReceiverTypeForSubstitutedMembers"
    }
}

class FirSubstitutionOverrideStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val substitutionOverrideCacheByScope: FirCache<ScopeSessionKey<*, *>, SubstitutionOverrideCache, Nothing?> =
        cachesFactory.createCache { _ -> SubstitutionOverrideCache(session.firCachesFactory) }

    class SubstitutionOverrideCache(cachesFactory: FirCachesFactory) {
        val overridesForFunctions: FirCache<FirNamedFunctionSymbol, FirNamedFunctionSymbol, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope -> scope.createSubstitutionOverrideFunction(original) }
        val overridesForConstructors: FirCache<FirConstructorSymbol, FirConstructorSymbol, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope -> scope.createSubstitutionOverrideConstructor(original) }
        val overridesForVariables: FirCache<FirVariableSymbol<*>, FirVariableSymbol<*>, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope ->
                when (original) {
                    is FirPropertySymbol -> scope.createSubstitutionOverrideProperty(original)
                    is FirFieldSymbol -> scope.createSubstitutionOverrideField(original)
                    is FirSyntheticPropertySymbol -> scope.createSubstitutionOverrideSyntheticProperty(original)
                    else -> error("symbol $original is not overridable")
                }
            }
    }
}

private val FirSession.substitutionOverrideStorage: FirSubstitutionOverrideStorage by FirSession.sessionComponentAccessor()
