/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.FakeOverrideSubstitution
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirTypeScope,
    private val substitutor: ConeSubstitutor,
    private val dispatchReceiverTypeForSubstitutedMembers: ConeClassLikeType,
    private val skipPrivateMembers: Boolean,
    private val makeExpect: Boolean = false
) : FirTypeScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideConstructors = mutableMapOf<FirConstructorSymbol, FirConstructorSymbol>()
    private val fakeOverrideVariables = mutableMapOf<FirVariableSymbol<*>, FirVariableSymbol<*>>()

    private val newOwnerClassId = dispatchReceiverTypeForSubstitutedMembers.lookupTag.classId

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }

        return super.processFunctionsByName(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            functionSymbol, processor, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope, fakeOverrideFunctions
        )

    private inline fun <reified D : FirCallableSymbol<*>> processDirectOverriddenWithBaseScope(
        callableSymbol: D,
        noinline processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction,
        fakeOverridesMap: Map<out FirCallableSymbol<*>, FirCallableSymbol<*>>
    ): ProcessorAction {
        val original = callableSymbol.originalForSubstitutionOverride?.takeIf { it in fakeOverridesMap }
            ?: return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(callableSymbol, processor)

        if (!processor(original, useSiteMemberScope)) return ProcessorAction.STOP

        return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(original, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            when (original) {
                is FirPropertySymbol -> {
                    val property = fakeOverrideVariables.getOrPut(original) { createFakeOverrideProperty(original) }
                    processor(property)
                }
                is FirFieldSymbol -> {
                    val field = fakeOverrideVariables.getOrPut(original) { createFakeOverrideField(original) }
                    processor(field)
                }
                is FirAccessorSymbol -> {
                    val accessor = fakeOverrideVariables.getOrPut(original) { createFakeOverrideAccessor(original) }
                    processor(accessor)
                }
                else -> {
                    processor(original)
                }
            }
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            propertySymbol, processor, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
            fakeOverrideVariables
        )

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

    private fun createFakeOverrideFunction(original: FirFunctionSymbol<*>): FirFunctionSymbol<*> {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = when (original) {
            is FirNamedFunctionSymbol -> original.fir
            is FirConstructorSymbol -> return original
            else -> throw AssertionError("Should not be here")
        }
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(member)
        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReceiverType == null && newReturnType == null && newParameterTypes.all { it == null } &&
            newTypeParameters === member.typeParameters && fakeOverrideSubstitution == null) {
            return original
        }

        /*
         * Member functions can't capture type parameters, so
         *   it's safe to cast newTypeParameters to List<FirTypeParameter>
         */
        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createFakeOverrideFunction(
            session,
            member,
            original,
            dispatchReceiverTypeForSubstitutedMembers,
            newReceiverType,
            newReturnType,
            newParameterTypes,
            newTypeParameters as List<FirTypeParameter>,
            newOwnerClassId,
            makeExpect,
            fakeOverrideSubstitution
        )
    }

    private fun createFakeOverrideConstructor(original: FirConstructorSymbol): FirConstructorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val constructor = original.fir

        val (newTypeParameters, _, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(constructor)
        val newParameterTypes = constructor.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReturnType == null && newParameterTypes.all { it == null } && newTypeParameters === constructor.typeParameters) {
            return original
        }
        return FirFakeOverrideGenerator.createFakeOverrideConstructor(
            FirConstructorSymbol(original.callableId, overriddenSymbol = original),
            session, constructor, dispatchReceiverTypeForSubstitutedMembers,
            newReturnType, newParameterTypes, newTypeParameters, makeExpect, fakeOverrideSubstitution
        ).symbol
    }

    private fun createFakeOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, _, fakeOverrideSubstitution) = createSubstitutedData(member)
        if (newReceiverType == null &&
            newReturnType == null && newTypeParameters === member.typeParameters
        ) {
            return original
        }

        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createFakeOverrideProperty(
            session,
            member,
            original,
            dispatchReceiverTypeForSubstitutedMembers,
            newReceiverType,
            newReturnType,
            newTypeParameters as List<FirTypeParameter>,
            newOwnerClassId,
            makeExpect,
            fakeOverrideSubstitution
        )
    }

    private data class SubstitutedData(
        val typeParameters: List<FirTypeParameterRef>,
        val receiverType: ConeKotlinType?,
        val returnType: ConeKotlinType?,
        val substitutor: ConeSubstitutor,
        val fakeOverrideSubstitution: FakeOverrideSubstitution?
    )

    private fun createSubstitutedData(member: FirCallableMemberDeclaration<*>): SubstitutedData {
        val (newTypeParameters, substitutor) = FirFakeOverrideGenerator.createNewTypeParametersAndSubstitutor(
            member as FirTypeParameterRefsOwner,
            substitutor,
            forceTypeParametersRecreation = dispatchReceiverTypeForSubstitutedMembers.lookupTag != member.dispatchReceiverClassOrNull()
        )

        val receiverType = member.receiverTypeRef?.coneType
        val newReceiverType = receiverType?.substitute(substitutor)

        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val fakeOverrideSubstitution = runIf(returnType == null) { FakeOverrideSubstitution(substitutor, member.symbol) }
        val newReturnType = returnType?.substitute(substitutor)
        return SubstitutedData(newTypeParameters, newReceiverType, newReturnType, substitutor, fakeOverrideSubstitution)
    }

    private fun createFakeOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        // TODO: do we have fields with implicit type?
        val newReturnType = returnType?.substitute() ?: return original

        return FirFakeOverrideGenerator.createFakeOverrideField(session, member, original, newReturnType, newOwnerClassId)
    }

    private fun createFakeOverrideAccessor(original: FirAccessorSymbol): FirAccessorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir as FirSyntheticProperty
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val fakeOverrideSubstitution = runIf(returnType == null) { FakeOverrideSubstitution(substitutor, original) }
        val newReturnType = returnType?.substitute()

        val newParameterTypes = member.getter.valueParameters.map {
            it.returnTypeRef.coneType.substitute()
        }

        if (newReturnType == null && newParameterTypes.all { it == null }) {
            return original
        }

        return FirFakeOverrideGenerator.createFakeOverrideAccessor(
            session,
            member,
            original,
            dispatchReceiverTypeForSubstitutedMembers,
            newReturnType,
            newParameterTypes,
            fakeOverrideSubstitution
        )
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val constructor = fakeOverrideConstructors.getOrPut(original) { createFakeOverrideConstructor(original) }
            processor(constructor)
        }
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }
}
