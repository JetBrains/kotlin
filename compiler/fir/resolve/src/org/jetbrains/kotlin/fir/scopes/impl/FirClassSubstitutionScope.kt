/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.FakeOverrideSubstitution
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
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

    private val substitutionOverrideFunctions = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()
    private val substitutionOverrideConstructors = mutableMapOf<FirConstructorSymbol, FirConstructorSymbol>()
    private val substitutionOverrideVariables = mutableMapOf<FirVariableSymbol<*>, FirVariableSymbol<*>>()

    private val newOwnerClassId = dispatchReceiverTypeForSubstitutedMembers.lookupTag.classId

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val function = substitutionOverrideFunctions.getOrPut(original) { createSubstitutionOverrideFunction(original) }
            processor(function)
        }

        return super.processFunctionsByName(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            functionSymbol, processor, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope, substitutionOverrideFunctions
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
                    val property = substitutionOverrideVariables.getOrPut(original) { createSubstitutionOverrideProperty(original) }
                    processor(property)
                }
                is FirFieldSymbol -> {
                    val field = substitutionOverrideVariables.getOrPut(original) { createSubstitutionOverrideField(original) }
                    processor(field)
                }
                is FirAccessorSymbol -> {
                    val accessor = substitutionOverrideVariables.getOrPut(original) { createSubstitutionOverrideAccessor(original) }
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
            substitutionOverrideVariables
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

    private fun createSubstitutionOverrideFunction(original: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(member)
        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReceiverType == null && newReturnType == null && newParameterTypes.all { it == null } &&
            newTypeParameters === member.typeParameters && fakeOverrideSubstitution == null
        ) {
            return original
        }

        /*
         * Member functions can't capture type parameters, so
         *   it's safe to cast newTypeParameters to List<FirTypeParameter>
         */
        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createSubstitutionOverrideFunction(
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

    private fun createSubstitutionOverrideConstructor(original: FirConstructorSymbol): FirConstructorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val constructor = original.fir

        val (newTypeParameters, _, newReturnType, newSubstitutor, fakeOverrideSubstitution) = createSubstitutedData(constructor)
        val newParameterTypes = constructor.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReturnType == null && newParameterTypes.all { it == null } && newTypeParameters === constructor.typeParameters) {
            return original
        }

        return FirFakeOverrideGenerator.createSubstitutionOverrideConstructor(
            FirConstructorSymbol(original.callableId),
            session, constructor, dispatchReceiverTypeForSubstitutedMembers,
            newReturnType, newParameterTypes, newTypeParameters, makeExpect, fakeOverrideSubstitution
        ).symbol
    }

    private fun createSubstitutionOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, _, fakeOverrideSubstitution) = createSubstitutedData(member)

        if (newReceiverType == null && newReturnType == null &&
            newTypeParameters === member.typeParameters && fakeOverrideSubstitution == null
        ) {
            return original
        }

        @Suppress("UNCHECKED_CAST")
        return FirFakeOverrideGenerator.createSubstitutionOverrideProperty(
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

    private fun createSubstitutedData(member: FirCallableDeclaration): SubstitutedData {
        val (newTypeParameters, substitutor) = FirFakeOverrideGenerator.createNewTypeParametersAndSubstitutor(
            session,
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

    private fun createSubstitutionOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        // TODO: do we have fields with implicit type?
        val newReturnType = returnType?.substitute() ?: return original

        return FirFakeOverrideGenerator.createSubstitutionOverrideField(session, member, original, newReturnType, newOwnerClassId)
    }

    private fun createSubstitutionOverrideAccessor(original: FirAccessorSymbol): FirAccessorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir as FirSyntheticProperty
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = member.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val fakeOverrideSubstitution = runIf(returnType == null) { FakeOverrideSubstitution(substitutor, original) }
        val newReturnType = returnType?.substitute()

        val newGetterParameterTypes = member.getter.valueParameters.map {
            it.returnTypeRef.coneType.substitute()
        }
        val newSetterParameterTypes = member.setter?.valueParameters?.map {
            it.returnTypeRef.coneType.substitute()
        }.orEmpty()

        if (newReturnType == null &&
            newGetterParameterTypes.all { it == null } &&
            newSetterParameterTypes.all { it == null }
        ) {
            return original
        }

        return FirFakeOverrideGenerator.createSubstitutionOverrideAccessor(
            session,
            member,
            original,
            dispatchReceiverTypeForSubstitutedMembers,
            newReturnType,
            newGetterParameterTypes,
            newSetterParameterTypes,
            fakeOverrideSubstitution
        )
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val constructor = substitutionOverrideConstructors.getOrPut(original) { createSubstitutionOverrideConstructor(original) }
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
