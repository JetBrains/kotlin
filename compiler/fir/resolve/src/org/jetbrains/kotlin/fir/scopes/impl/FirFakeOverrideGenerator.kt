/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

object FirFakeOverrideGenerator {
    fun createFakeOverrideFunction(
        session: FirSession,
        baseFunction: FirSimpleFunction,
        baseSymbol: FirNamedFunctionSymbol,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        derivedClassId: ClassId? = null,
        isExpect: Boolean = baseFunction.isExpect
    ): FirNamedFunctionSymbol {
        val symbol = FirNamedFunctionSymbol(
            CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseFunction.name),
            isFakeOverride = true, overriddenSymbol = baseSymbol
        )
        createFakeOverrideFunction(
            symbol, session, baseFunction, newReceiverType, newReturnType, newParameterTypes, newTypeParameters, isExpect
        )
        return symbol
    }

    private fun createFakeOverrideFunction(
        fakeOverrideSymbol: FirFunctionSymbol<FirSimpleFunction>,
        session: FirSession,
        baseFunction: FirSimpleFunction,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameter>?,
        isExpect: Boolean = baseFunction.isExpect,
    ): FirSimpleFunction {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return createCopyForFirFunction(
            fakeOverrideSymbol,
            baseFunction,
            session,
            FirDeclarationOrigin.FakeOverride,
            isExpect,
            newParameterTypes,
            newTypeParameters,
            newReceiverType,
            newReturnType
        )
    }

    fun createCopyForFirFunction(
        newSymbol: FirFunctionSymbol<FirSimpleFunction>,
        baseFunction: FirSimpleFunction,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseFunction.isExpect,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
    ): FirSimpleFunction {
        return buildSimpleFunction {
            source = baseFunction.source
            this.session = session
            this.origin = origin
            name = baseFunction.name
            status = baseFunction.status.updatedStatus(isExpect, newModality, newVisibility)
            symbol = newSymbol
            resolvePhase = baseFunction.resolvePhase

            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session, baseFunction, newParameterTypes, newTypeParameters, newReceiverType, newReturnType
            ).filterIsInstance<FirTypeParameter>()
        }
    }

    fun createFakeOverrideConstructor(
        fakeOverrideSymbol: FirConstructorSymbol,
        session: FirSession,
        baseConstructor: FirConstructor,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        isExpect: Boolean
    ): FirConstructor {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return buildConstructor {
            source = baseConstructor.source
            this.session = session
            origin = FirDeclarationOrigin.FakeOverride
            receiverTypeRef = baseConstructor.receiverTypeRef?.withReplacedConeType(null)
            status = baseConstructor.status.updatedStatus(isExpect)
            symbol = fakeOverrideSymbol
            resolvePhase = baseConstructor.resolvePhase

            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session, baseConstructor, newParameterTypes, newTypeParameters, newReceiverType = null, newReturnType
            )
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
        session: FirSession,
        baseFunction: FirFunction<*>,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?
    ): List<FirTypeParameterRef> {
        return when {
            baseFunction.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(session, baseFunction, newParameterTypes, newReceiverType, newReturnType)
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    baseFunction, ConeSubstitutor.Empty
                )
                val copiedParameterTypes = baseFunction.valueParameters.map {
                    substitutor.substituteOrNull(it.returnTypeRef.coneType)
                }
                val (copiedReceiverType, copiedReturnType) = substituteReceiverAndReturnType(
                    baseFunction as FirCallableMemberDeclaration<*>, newReceiverType, newReturnType, substitutor
                )
                configureAnnotationsAndSignature(session, baseFunction, copiedParameterTypes, copiedReceiverType, copiedReturnType)
                copiedTypeParameters
            }
            else -> {
                configureAnnotationsAndSignature(session, baseFunction, newParameterTypes, newReceiverType, newReturnType)
                newTypeParameters
            }
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsAndSignature(
        session: FirSession,
        baseFunction: FirFunction<*>,
        newParameterTypes: List<ConeKotlinType?>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?
    ) {
        annotations += baseFunction.annotations
        returnTypeRef = replaceReturnTypeIfResolved(baseFunction, newReturnType)

        if (this is FirSimpleFunctionBuilder) {
            receiverTypeRef = baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType)
        }
        valueParameters += baseFunction.valueParameters.zip(
            newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
        ) { valueParameter, newType ->
            buildValueParameterCopy(valueParameter) {
                origin = FirDeclarationOrigin.FakeOverride
                returnTypeRef = valueParameter.returnTypeRef.withReplacedConeType(newType)
                symbol = FirVariableSymbol(valueParameter.symbol.callableId)
            }
        }
    }

    private fun replaceReturnTypeIfResolved(
        base: FirCallableDeclaration<*>,
        newReturnType: ConeKotlinType?
    ): FirTypeRef = if (base.returnTypeRef is FirResolvedTypeRef)
        base.returnTypeRef.withReplacedConeType(newReturnType)
    else
        base.returnTypeRef

    fun createFakeOverrideProperty(
        session: FirSession,
        baseProperty: FirProperty,
        baseSymbol: FirPropertySymbol,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        derivedClassId: ClassId? = null,
        isExpect: Boolean = baseProperty.isExpect
    ): FirPropertySymbol {
        val symbol = FirPropertySymbol(
            CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseProperty.name),
            isFakeOverride = true, overriddenSymbol = baseSymbol
        )
        createCopyForFirProperty(
            symbol, baseProperty, session, isExpect,
            newTypeParameters, newReceiverType, newReturnType
        )
        return symbol
    }

    fun createCopyForFirProperty(
        newSymbol: FirPropertySymbol,
        baseProperty: FirProperty,
        session: FirSession,
        isExpect: Boolean = baseProperty.isExpect,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
    ): FirProperty {
        return buildProperty {
            source = baseProperty.source
            this.session = session
            origin = FirDeclarationOrigin.FakeOverride
            name = baseProperty.name
            isVar = baseProperty.isVar
            this.symbol = newSymbol
            isLocal = false
            status = baseProperty.status.updatedStatus(isExpect, newModality, newVisibility)

            resolvePhase = baseProperty.resolvePhase
            typeParameters += configureAnnotationsTypeParametersAndSignature(
                baseProperty,
                newTypeParameters,
                newReceiverType,
                newReturnType
            )
        }
    }

    private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        baseProperty: FirProperty,
        newTypeParameters: List<FirTypeParameter>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType)
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    baseProperty, ConeSubstitutor.Empty
                )
                val (copiedReceiverType, copiedReturnType) = substituteReceiverAndReturnType(
                    baseProperty, newReceiverType, newReturnType, substitutor
                )
                configureAnnotationsAndSignature(baseProperty, copiedReceiverType, copiedReturnType)
                copiedTypeParameters.filterIsInstance<FirTypeParameter>()
            }
            else -> {
                configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType)
                newTypeParameters
            }
        }
    }

    private fun substituteReceiverAndReturnType(
        baseCallable: FirCallableMemberDeclaration<*>,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        substitutor: ConeSubstitutor
    ): Pair<ConeKotlinType?, ConeKotlinType?> {
        val copiedReceiverType = newReceiverType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.receiverTypeRef?.let {
            substitutor.substituteOrNull(it.coneType)
        }
        val copiedReturnType = newReturnType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.returnTypeRef.let {
            substitutor.substituteOrNull(it.coneType)
        }
        return copiedReceiverType to copiedReturnType
    }

    private fun FirPropertyBuilder.configureAnnotationsAndSignature(
        baseProperty: FirProperty,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?
    ) {
        annotations += baseProperty.annotations
        returnTypeRef = replaceReturnTypeIfResolved(baseProperty, newReturnType)
        receiverTypeRef = baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType)
    }

    fun createFakeOverrideField(
        session: FirSession,
        baseField: FirField,
        baseSymbol: FirFieldSymbol,
        newReturnType: ConeKotlinType?,
        derivedClassId: ClassId?
    ): FirFieldSymbol {
        val symbol = FirFieldSymbol(
            CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseField.name)
        )
        buildField {
            source = baseField.source
            this.session = session
            origin = FirDeclarationOrigin.FakeOverride
            resolvePhase = baseField.resolvePhase
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
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?
    ): FirAccessorSymbol {
        val functionSymbol = FirNamedFunctionSymbol(baseSymbol.accessorId)
        val function = createFakeOverrideFunction(
            functionSymbol,
            session,
            baseProperty.getter.delegate,
            newReceiverType = null,
            newReturnType,
            newParameterTypes,
            newTypeParameters = null
        )
        return buildSyntheticProperty {
            this.session = session
            name = baseProperty.name
            symbol = FirAccessorSymbol(baseSymbol.callableId, baseSymbol.accessorId)
            delegateGetter = function
        }.symbol
    }

    private fun FirDeclarationStatus.updatedStatus(
        isExpect: Boolean,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
    ): FirDeclarationStatus {
        return if (this.isExpect == isExpect && newModality == null && newVisibility == null) {
            this
        } else {
            require(this is FirDeclarationStatusImpl) { "Unexpected class ${this::class}" }
            this.resolved(newVisibility ?: visibility, newModality ?: modality!!).apply {
                this.isExpect = isExpect
            }
        }
    }

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        member: FirTypeParameterRefsOwner,
        substitutor: ConeSubstitutor,
        forceTypeParametersRecreation: Boolean = true
    ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { typeParameter ->
            if (typeParameter !is FirTypeParameter) return@map null
            FirTypeParameterBuilder().apply {
                source = typeParameter.source
                session = typeParameter.session
                origin = FirDeclarationOrigin.FakeOverride
                name = typeParameter.name
                symbol = FirTypeParameterSymbol()
                variance = typeParameter.variance
                isReified = typeParameter.isReified
                annotations += typeParameter.annotations
            }
        }

        val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).mapNotNull { (original, new) ->
            if (new != null)
                Pair(original.symbol, ConeTypeParameterTypeImpl(new.symbol.toLookupTag(), isNullable = false))
            else null
        }.toMap()

        val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters)

        var wereChangesInTypeParameters = forceTypeParametersRecreation
        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
            if (newTypeParameter == null) continue
            val original = oldTypeParameter as FirTypeParameter
            for (boundTypeRef in original.bounds) {
                val typeForBound = boundTypeRef.coneType
                val substitutedBound = substitutor.substituteOrNull(typeForBound)
                if (substitutedBound != null) {
                    wereChangesInTypeParameters = true
                }
                newTypeParameter.bounds +=
                    buildResolvedTypeRef {
                        source = boundTypeRef.source
                        type = additionalSubstitutor.substituteOrSelf(substitutedBound ?: typeForBound)
                    }
            }
        }

        if (!wereChangesInTypeParameters) return Pair(member.typeParameters, substitutor)
        return Pair(
            newTypeParameters.mapIndexed { index, builder -> builder?.build() ?: member.typeParameters[index] },
            ChainedSubstitutor(substitutor, additionalSubstitutor)
        )
    }
}
