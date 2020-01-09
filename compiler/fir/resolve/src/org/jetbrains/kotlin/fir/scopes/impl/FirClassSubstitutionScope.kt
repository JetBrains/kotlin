/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirScope,
    scopeSession: ScopeSession,
    private val substitutor: ConeSubstitutor
) : FirScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideProperties = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()
    private val fakeOverrideFields = mutableMapOf<FirFieldSymbol, FirFieldSymbol>()
    private val fakeOverrideAccessors = mutableMapOf<FirAccessorSymbol, FirAccessorSymbol>()

    constructor(
        session: FirSession, useSiteMemberScope: FirScope, scopeSession: ScopeSession,
        substitution: Map<FirTypeParameterSymbol, ConeKotlinType>
    ) : this(session, useSiteMemberScope, scopeSession, substitutorByMap(substitution))

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
        return useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            when (original) {
                is FirPropertySymbol -> {
                    val property = fakeOverrideProperties.getOrPut(original) { createFakeOverrideProperty(original) }
                    processor(property)
                }
                is FirFieldSymbol -> {
                    val field = fakeOverrideFields.getOrPut(original) { createFakeOverrideField(original) }
                    processor(field)
                }
                is FirAccessorSymbol -> {
                    val accessor = fakeOverrideAccessors.getOrPut(original) { createFakeOverrideAccessor(original) }
                    processor(accessor)
                }
                else -> {
                    processor(original)
                }
            }
        }
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> Unit) {
        useSiteMemberScope.processClassifiersByName(name, processor)
    }

    private val typeCalculator by lazy { ReturnTypeCalculatorWithJump(session, scopeSession) }

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun ConeKotlinType.substitute(substitutor: ConeSubstitutor): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun createFakeOverrideFunction(original: FirFunctionSymbol<*>): FirFunctionSymbol<*> {
        val member = when (original) {
            is FirNamedFunctionSymbol -> original.fir
            is FirConstructorSymbol -> return original
            else -> throw AssertionError("Should not be here")
        }

        val (newTypeParameters, newSubstitutor) = createNewTypeParametersAndSubstitutor(member)

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>()
        val newReceiverType = receiverType?.substitute(newSubstitutor)

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute(newSubstitutor)

        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().substitute(newSubstitutor)
        }

        if (newReceiverType == null && newReturnType == null && newParameterTypes.all { it == null } &&
            newTypeParameters === member.typeParameters) {
            return original
        }

        return createFakeOverrideFunction(
            session, member, original, newReceiverType, newReturnType, newParameterTypes, newTypeParameters
        )
    }

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    private fun createNewTypeParametersAndSubstitutor(
        member: FirSimpleFunction
    ): Pair<List<FirTypeParameter>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { originalParameter ->
            FirTypeParameterImpl(
                originalParameter.source, originalParameter.session, originalParameter.name,
                FirTypeParameterSymbol(), originalParameter.variance, originalParameter.isReified
            ).apply {
                annotations += originalParameter.annotations
            }
        }

        val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).map {
            Pair(it.first.symbol, ConeTypeParameterTypeImpl(it.second.symbol.toLookupTag(), isNullable = false))
        }.toMap()

        val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters)

        var wereChangesInTypeParameters = false
        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
            for (boundTypeRef in oldTypeParameter.bounds) {
                val typeForBound = boundTypeRef.coneTypeUnsafe<ConeKotlinType>()
                val substitutedBound = typeForBound.substitute()
                if (substitutedBound != null) {
                    wereChangesInTypeParameters = true
                }

                newTypeParameter.bounds +=
                    FirResolvedTypeRefImpl(
                        boundTypeRef.source, additionalSubstitutor.substituteOrSelf(substitutedBound ?: typeForBound)
                    )
            }
        }

        // TODO: Uncomment when problem from org.jetbrains.kotlin.fir.Fir2IrTextTestGenerated.Declarations.Parameters.testDelegatedMembers is gone
        // The problem is that Fir2Ir thinks that type parameters in fake override are the same as for original
        // While common Ir contracts expect them to be different
        // if (!wereChangesInTypeParameters) return Pair(member.typeParameters, substitutor)

        return Pair(newTypeParameters, ChainedSubstitutor(substitutor, additionalSubstitutor))
    }

    private fun createFakeOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        val member = original.fir

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>()
        val newReceiverType = receiverType?.substitute()

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        if (newReceiverType == null && newReturnType == null) {
            return original
        }

        return createFakeOverrideProperty(session, member, original, newReceiverType, newReturnType)
    }

    private fun createFakeOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        val member = original.fir

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute() ?: return original

        return createFakeOverrideField(session, member, original, newReturnType)
    }

    private fun createFakeOverrideAccessor(original: FirAccessorSymbol): FirAccessorSymbol {
        val member = original.fir

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().substitute()
        }

        if (newReturnType == null && newParameterTypes.all { it == null }) {
            return original
        }

        return createFakeOverrideAccessor(session, member, original, newReturnType, newParameterTypes)
    }

    companion object {
        private fun createFakeOverrideFunction(
            fakeOverrideSymbol: FirFunctionSymbol<FirSimpleFunction>,
            session: FirSession,
            baseFunction: FirSimpleFunction,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null
        ): FirSimpleFunction {
            return with(baseFunction) {
                // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
                // As second alternative, we can invent some light-weight kind of FirRegularClass
                FirSimpleFunctionImpl(
                    source,
                    session,
                    baseFunction.returnTypeRef.withReplacedConeType(newReturnType),
                    baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    name,
                    baseFunction.status,
                    fakeOverrideSymbol
                ).apply {
                    resolvePhase = baseFunction.resolvePhase
                    valueParameters += baseFunction.valueParameters.zip(
                        newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
                    ) { valueParameter, newType ->
                        with(valueParameter) {
                            FirValueParameterImpl(
                                source,
                                session,
                                this.returnTypeRef.withReplacedConeType(newType),
                                name,
                                FirVariableSymbol(valueParameter.symbol.callableId),
                                defaultValue,
                                isCrossinline,
                                isNoinline,
                                isVararg
                            )
                        }
                    }

                    // TODO: Fix the hack for org.jetbrains.kotlin.fir.backend.Fir2IrVisitor.addFakeOverrides
                    // We might have added baseFunction.typeParameters in case new ones are null
                    // But it fails at org.jetbrains.kotlin.ir.AbstractIrTextTestCase.IrVerifier.elementsAreUniqueChecker
                    // because it shares the same declarations of type parameters between two different two functions
                    if (newTypeParameters != null) {
                        typeParameters += newTypeParameters
                    }
                }
            }
        }

        fun createFakeOverrideFunction(
            session: FirSession,
            baseFunction: FirSimpleFunction,
            baseSymbol: FirNamedFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null
        ): FirNamedFunctionSymbol {
            val symbol = FirNamedFunctionSymbol(baseSymbol.callableId, true, baseSymbol)
            createFakeOverrideFunction(
                symbol, session, baseFunction, newReceiverType, newReturnType, newParameterTypes, newTypeParameters
            )
            return symbol
        }

        fun createFakeOverrideProperty(
            session: FirSession,
            baseProperty: FirProperty,
            baseSymbol: FirPropertySymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ): FirPropertySymbol {
            val symbol = FirPropertySymbol(baseSymbol.callableId, true, baseSymbol)
            with(baseProperty) {
                FirPropertyImpl(
                    source,
                    session,
                    baseProperty.returnTypeRef.withReplacedConeType(newReturnType),
                    baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType),
                    name,
                    null,
                    null,
                    isVar,
                    symbol,
                    false,
                    baseProperty.status
                ).apply {
                    resolvePhase = baseProperty.resolvePhase
                }
            }
            return symbol
        }

        fun createFakeOverrideField(
            session: FirSession,
            baseField: FirField,
            baseSymbol: FirFieldSymbol,
            newReturnType: ConeKotlinType? = null
        ): FirFieldSymbol {
            val symbol = FirFieldSymbol(baseSymbol.callableId)
            with(baseField) {
                FirFieldImpl(
                    source, session,
                    baseField.returnTypeRef.withReplacedConeType(newReturnType),
                    name, symbol, isVar, baseField.status
                ).apply {
                    resolvePhase = baseField.resolvePhase
                }
            }
            return symbol
        }

        fun createFakeOverrideAccessor(
            session: FirSession,
            baseFunction: FirSimpleFunction,
            baseSymbol: FirAccessorSymbol,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirAccessorSymbol {
            val symbol = FirAccessorSymbol(baseSymbol.callableId, baseSymbol.accessorId)
            createFakeOverrideFunction(symbol, session, baseFunction, null, newReturnType, newParameterTypes)
            return symbol
        }
    }
}


fun FirTypeRef.withReplacedConeType(newType: ConeKotlinType?): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return FirResolvedTypeRefImpl(source, newType).apply {
        annotations += this@withReplacedConeType.annotations
    }

}
