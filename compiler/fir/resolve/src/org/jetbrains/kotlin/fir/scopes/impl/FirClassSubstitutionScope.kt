/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirScope,
    scopeSession: ScopeSession,
    private val substitutor: ConeSubstitutor,
    private val skipPrivateMembers: Boolean,
    private val derivedClassId: ClassId? = null
) : FirScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideProperties = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()
    private val fakeOverrideFields = mutableMapOf<FirFieldSymbol, FirFieldSymbol>()
    private val fakeOverrideAccessors = mutableMapOf<FirAccessorSymbol, FirAccessorSymbol>()

    constructor(
        session: FirSession, useSiteMemberScope: FirScope, scopeSession: ScopeSession,
        substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
        skipPrivateMembers: Boolean, derivedClassId: ClassId? = null
    ) : this(session, useSiteMemberScope, scopeSession, substitutorByMap(substitution), skipPrivateMembers, derivedClassId)

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->

            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }


        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
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

    private val typeCalculator =
        (scopeSession.returnTypeCalculator as ReturnTypeCalculator?) ?: ReturnTypeCalculatorForFullBodyResolve()

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
        if (skipPrivateMembers && member.visibility == Visibilities.PRIVATE) return original

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
            session, member, original, newReceiverType, newReturnType, newParameterTypes, newTypeParameters, derivedClassId
        )
    }

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    private fun createNewTypeParametersAndSubstitutor(
        member: FirSimpleFunction
    ): Pair<List<FirTypeParameter>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { originalParameter ->
            FirTypeParameterBuilder().apply {
                source = originalParameter.source
                session = originalParameter.session
                name = originalParameter.name
                symbol = FirTypeParameterSymbol()
                variance = originalParameter.variance
                isReified = originalParameter.isReified
                annotations += originalParameter.annotations
            }
        }

        val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).map {
            Pair(it.first.symbol, ConeTypeParameterTypeImpl(it.second.symbol.toLookupTag(), isNullable = false))
        }.toMap()

        val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters)

        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
            for (boundTypeRef in oldTypeParameter.bounds) {
                val typeForBound = boundTypeRef.coneTypeUnsafe<ConeKotlinType>()
                val substitutedBound = typeForBound.substitute()
                newTypeParameter.bounds +=
                    buildResolvedTypeRef {
                        source = boundTypeRef.source
                        type = additionalSubstitutor.substituteOrSelf(substitutedBound ?: typeForBound)
                    }
            }
        }

        // TODO: Uncomment when problem from org.jetbrains.kotlin.fir.Fir2IrTextTestGenerated.Declarations.Parameters.testDelegatedMembers is gone
        // The problem is that Fir2Ir thinks that type parameters in fake override are the same as for original
        // While common Ir contracts expect them to be different
        // if (!wereChangesInTypeParameters) return Pair(member.typeParameters, substitutor)

        return Pair(newTypeParameters.map { it.build() }, ChainedSubstitutor(substitutor, additionalSubstitutor))
    }

    private fun createFakeOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.PRIVATE) return original

        val receiverType = member.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>()
        val newReceiverType = receiverType?.substitute()

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        if (newReceiverType == null && newReturnType == null) {
            return original
        }

        return createFakeOverrideProperty(session, member, original, newReceiverType, newReturnType, derivedClassId)
    }

    private fun createFakeOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.PRIVATE) return original

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute() ?: return original

        return createFakeOverrideField(session, member, original, newReturnType, derivedClassId)
    }

    private fun createFakeOverrideAccessor(original: FirAccessorSymbol): FirAccessorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir as FirSyntheticProperty
        if (skipPrivateMembers && member.visibility == Visibilities.PRIVATE) return original

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.getter.valueParameters.map {
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
            // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
            // As second alternative, we can invent some light-weight kind of FirRegularClass
            return buildSimpleFunction {
                source = baseFunction.source
                this.session = session
                returnTypeRef = baseFunction.returnTypeRef.withReplacedReturnType(newReturnType)
                receiverTypeRef = baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType)
                name = baseFunction.name
                status = baseFunction.status
                symbol = fakeOverrideSymbol
                annotations += baseFunction.annotations
                resolvePhase = baseFunction.resolvePhase
                valueParameters += baseFunction.valueParameters.zip(
                    newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
                ) { valueParameter, newType ->
                    buildValueParameter {
                        source = valueParameter.source
                        this.session = session
                        returnTypeRef = valueParameter.returnTypeRef.withReplacedConeType(newType)
                        name = valueParameter.name
                        symbol = FirVariableSymbol(valueParameter.symbol.callableId)
                        defaultValue = valueParameter.defaultValue
                        isCrossinline = valueParameter.isCrossinline
                        isNoinline = valueParameter.isNoinline
                        isVararg = valueParameter.isVararg
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

        fun createFakeOverrideFunction(
            session: FirSession,
            baseFunction: FirSimpleFunction,
            baseSymbol: FirNamedFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null,
            derivedClassId: ClassId? = null
        ): FirNamedFunctionSymbol {
            val symbol = FirNamedFunctionSymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseFunction.name),
                isFakeOverride = true, overriddenSymbol = baseSymbol
            )
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
            newReturnType: ConeKotlinType? = null,
            derivedClassId: ClassId? = null
        ): FirPropertySymbol {
            val symbol = FirPropertySymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseProperty.name),
                isFakeOverride = true, overriddenSymbol = baseSymbol
            )
            buildProperty {
                source = baseProperty.source
                this.session = session
                returnTypeRef = baseProperty.returnTypeRef.withReplacedReturnType(newReturnType)
                receiverTypeRef = baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType)
                name = baseProperty.name
                isVar = baseProperty.isVar
                this.symbol = symbol
                isLocal = false
                status = baseProperty.status
                resolvePhase = baseProperty.resolvePhase
                annotations += baseProperty.annotations
            }
            return symbol
        }

        fun createFakeOverrideField(
            session: FirSession,
            baseField: FirField,
            baseSymbol: FirFieldSymbol,
            newReturnType: ConeKotlinType? = null,
            derivedClassId: ClassId? = null
        ): FirFieldSymbol {
            val symbol = FirFieldSymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseField.name)
            )
            buildField {
                source = baseField.source
                this.session = session
                returnTypeRef = baseField.returnTypeRef.withReplacedConeType(newReturnType)
                name = baseField.name
                this.symbol = symbol
                isVar = baseField.isVar
                status = baseField.status
                resolvePhase = baseField.resolvePhase
                annotations += baseField.annotations
            }
            return symbol
        }

        fun createFakeOverrideAccessor(
            session: FirSession,
            baseProperty: FirSyntheticProperty,
            baseSymbol: FirAccessorSymbol,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirAccessorSymbol {
            val functionSymbol = FirNamedFunctionSymbol(baseSymbol.accessorId)
            val function = createFakeOverrideFunction(
                functionSymbol, session, baseProperty.getter.delegate, null, newReturnType, newParameterTypes
            )
            return buildSyntheticProperty {
                this.session = session
                name = baseProperty.name
                symbol = FirAccessorSymbol(baseSymbol.callableId, baseSymbol.accessorId)
                delegateGetter = function
            }.symbol
        }
    }
}

// Unlike other cases, return types may be implicit, i.e. unresolved
// But in that cases newType should also be `null`
fun FirTypeRef.withReplacedReturnType(newType: ConeKotlinType?): FirTypeRef {
    require(this is FirResolvedTypeRef || newType == null)
    if (newType == null) return this

    return buildResolvedTypeRef {
        source = this@withReplacedReturnType.source
        type = newType
        annotations += this@withReplacedReturnType.annotations
    }
}

fun FirTypeRef.withReplacedConeType(newType: ConeKotlinType?): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return buildResolvedTypeRef {
        source = this@withReplacedConeType.source
        type = newType
        annotations += this@withReplacedConeType.annotations
    }
}
