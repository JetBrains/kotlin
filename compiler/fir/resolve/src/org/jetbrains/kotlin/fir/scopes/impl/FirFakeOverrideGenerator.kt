/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FakeOverrideSubstitution
import org.jetbrains.kotlin.fir.scopes.fakeOverrideSubstitution
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirFakeOverrideGenerator {
    fun createSubstitutionOverrideFunction(
        session: FirSession,
        baseFunction: FirSimpleFunction,
        baseSymbol: FirNamedFunctionSymbol,
        newDispatchReceiverType: ConeKotlinType?,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        derivedClassId: ClassId? = null,
        isExpect: Boolean = baseFunction.isExpect,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirNamedFunctionSymbol {
        val symbol = if (derivedClassId == null) {
            FirNamedFunctionSymbol(baseSymbol.callableId)
        } else {
            FirNamedFunctionSymbol(CallableId(derivedClassId, baseFunction.name))
        }
        createSubstitutionOverrideFunction(
            symbol, session, baseFunction, newDispatchReceiverType, newReceiverType, newReturnType,
            newParameterTypes, newTypeParameters, isExpect, fakeOverrideSubstitution
        )
        return symbol
    }

    private fun createSubstitutionOverrideFunction(
        fakeOverrideSymbol: FirNamedFunctionSymbol,
        session: FirSession,
        baseFunction: FirSimpleFunction,
        newDispatchReceiverType: ConeKotlinType?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameter>?,
        isExpect: Boolean = baseFunction.isExpect,
        fakeOverrideSubstitution: FakeOverrideSubstitution?,
    ): FirSimpleFunction {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return createCopyForFirFunction(
            fakeOverrideSymbol,
            baseFunction,
            session,
            FirDeclarationOrigin.SubstitutionOverride,
            isExpect,
            newDispatchReceiverType,
            newParameterTypes,
            newTypeParameters,
            newReceiverType,
            newReturnType,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        ).apply {
            originalForSubstitutionOverrideAttr = baseFunction
        }
    }

    fun createCopyForFirFunction(
        newSymbol: FirNamedFunctionSymbol,
        baseFunction: FirSimpleFunction,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseFunction.isExpect,
        newDispatchReceiverType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirSimpleFunction {
        return buildSimpleFunction {
            source = baseFunction.source
            moduleData = session.nullableModuleData ?: baseFunction.moduleData
            this.origin = origin
            name = baseFunction.name
            status = baseFunction.status.copy(isExpect, newModality, newVisibility)
            symbol = newSymbol
            resolvePhase = baseFunction.resolvePhase

            dispatchReceiverType = newDispatchReceiverType
            attributes = baseFunction.attributes.copy()
            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session, baseFunction, newParameterTypes, newTypeParameters, newReceiverType, newReturnType, fakeOverrideSubstitution
            ).filterIsInstance<FirTypeParameter>()
            deprecation = baseFunction.deprecation
        }
    }

    fun createSubstitutionOverrideConstructor(
        fakeOverrideSymbol: FirConstructorSymbol,
        session: FirSession,
        baseConstructor: FirConstructor,
        newDispatchReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        isExpect: Boolean,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): FirConstructor {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return buildConstructor {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.SubstitutionOverride
            receiverTypeRef = baseConstructor.receiverTypeRef?.withReplacedConeType(null)
            status = baseConstructor.status.copy(isExpect)
            symbol = fakeOverrideSymbol

            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session,
                baseConstructor,
                newParameterTypes,
                newTypeParameters,
                newReceiverType = null,
                newReturnType,
                fakeOverrideSubstitution
            )

            dispatchReceiverType = newDispatchReceiverType

            resolvePhase = baseConstructor.resolvePhase
            source = baseConstructor.source
            attributes = baseConstructor.attributes.copy()
            deprecation = baseConstructor.deprecation
        }.apply {
            originalForSubstitutionOverrideAttr = baseConstructor
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseFunction: FirFunction,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): List<FirTypeParameterRef> {
        return when {
            baseFunction.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    newParameterTypes,
                    newReceiverType,
                    newReturnType,
                    fakeOverrideSubstitution
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseFunction, ConeSubstitutor.Empty
                )
                val copiedParameterTypes = baseFunction.valueParameters.map {
                    substitutor.substituteOrNull(it.returnTypeRef.coneType)
                }
                val symbol = baseFunction.symbol
                val (copiedReceiverType, possibleReturnType) = substituteReceiverAndReturnType(
                    baseFunction as FirCallableDeclaration, newReceiverType, newReturnType, substitutor
                )
                val (copiedReturnType, newFakeOverrideSubstitution) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to FakeOverrideSubstitution(substitutor, symbol)
                }
                configureAnnotationsAndSignature(
                    baseFunction,
                    copiedParameterTypes,
                    copiedReceiverType,
                    copiedReturnType,
                    newFakeOverrideSubstitution
                )
                copiedTypeParameters
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    newParameterTypes,
                    newReceiverType,
                    newReturnType,
                    fakeOverrideSubstitution
                )
                newTypeParameters
            }
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsAndSignature(
        baseFunction: FirFunction,
        newParameterTypes: List<ConeKotlinType?>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ) {
        annotations += baseFunction.annotations

        @Suppress("NAME_SHADOWING")
        val fakeOverrideSubstitution = fakeOverrideSubstitution ?: runIf(baseFunction.returnTypeRef is FirImplicitTypeRef) {
            FakeOverrideSubstitution(ConeSubstitutor.Empty, baseFunction.symbol)
        }

        if (fakeOverrideSubstitution != null) {
            returnTypeRef = buildImplicitTypeRef()
            attributes.fakeOverrideSubstitution = fakeOverrideSubstitution
        } else {
            returnTypeRef = baseFunction.returnTypeRef.withReplacedReturnType(newReturnType)
        }

        if (this is FirSimpleFunctionBuilder) {
            receiverTypeRef = baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType)
        }
        valueParameters += baseFunction.valueParameters.zip(
            newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
        ) { valueParameter, newType ->
            buildValueParameterCopy(valueParameter) {
                origin = FirDeclarationOrigin.SubstitutionOverride
                returnTypeRef = valueParameter.returnTypeRef.withReplacedConeType(newType)
                symbol = FirValueParameterSymbol(valueParameter.name)
            }
        }
    }

    fun createSubstitutionOverrideProperty(
        session: FirSession,
        baseProperty: FirProperty,
        baseSymbol: FirPropertySymbol,
        newDispatchReceiverType: ConeKotlinType?,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        derivedClassId: ClassId? = null,
        isExpect: Boolean = baseProperty.isExpect,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirPropertySymbol {
        val symbol = if (derivedClassId == null) {
            FirPropertySymbol(baseSymbol.callableId)
        } else {
            FirPropertySymbol(CallableId(derivedClassId, baseProperty.name))
        }
        createCopyForFirProperty(
            symbol, baseProperty, session, FirDeclarationOrigin.SubstitutionOverride, isExpect,
            newDispatchReceiverType, newTypeParameters, newReceiverType, newReturnType,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        ).apply {
            originalForSubstitutionOverrideAttr = baseProperty
        }
        return symbol
    }

    fun createCopyForFirProperty(
        newSymbol: FirPropertySymbol,
        baseProperty: FirProperty,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseProperty.isExpect,
        newDispatchReceiverType: ConeKotlinType?,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirProperty {
        return buildProperty {
            source = baseProperty.source
            moduleData = session.moduleData
            this.origin = origin
            name = baseProperty.name
            isVar = baseProperty.isVar
            this.symbol = newSymbol
            isLocal = false
            status = baseProperty.status.copy(isExpect, newModality, newVisibility)

            resolvePhase = baseProperty.resolvePhase
            dispatchReceiverType = newDispatchReceiverType
            attributes = baseProperty.attributes.copy()
            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session,
                baseProperty,
                newTypeParameters,
                newReceiverType,
                newReturnType,
                fakeOverrideSubstitution
            )
            deprecation = baseProperty.deprecation
        }
    }

    private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseProperty: FirProperty,
        newTypeParameters: List<FirTypeParameter>?,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType, fakeOverrideSubstitution)
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseProperty, ConeSubstitutor.Empty
                )
                val (copiedReceiverType, possibleReturnType) = substituteReceiverAndReturnType(
                    baseProperty, newReceiverType, newReturnType, substitutor
                )
                val (copiedReturnType, newFakeOverrideSubstitution) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to FakeOverrideSubstitution(substitutor, baseProperty.symbol)
                }
                configureAnnotationsAndSignature(baseProperty, copiedReceiverType, copiedReturnType, newFakeOverrideSubstitution)
                copiedTypeParameters.filterIsInstance<FirTypeParameter>()
            }
            else -> {
                configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType, fakeOverrideSubstitution)
                newTypeParameters
            }
        }
    }

    private fun substituteReceiverAndReturnType(
        baseCallable: FirCallableDeclaration,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        substitutor: ConeSubstitutor
    ): Pair<ConeKotlinType?, Maybe<ConeKotlinType?>> {
        val copiedReceiverType = newReceiverType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.receiverTypeRef?.let {
            substitutor.substituteOrNull(it.coneType)
        }

        val copiedReturnType = newReturnType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.returnTypeRef.let {
            val coneType = baseCallable.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return copiedReceiverType to Maybe.Nothing
            substitutor.substituteOrNull(coneType)
        }
        return copiedReceiverType to Maybe.Value(copiedReturnType)
    }

    private fun FirPropertyBuilder.configureAnnotationsAndSignature(
        baseProperty: FirProperty,
        newReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ) {
        annotations += baseProperty.annotations

        @Suppress("NAME_SHADOWING")
        val fakeOverrideSubstitution = fakeOverrideSubstitution ?: runIf(baseProperty.returnTypeRef is FirImplicitTypeRef) {
            FakeOverrideSubstitution(ConeSubstitutor.Empty, baseProperty.symbol)
        }
        if (fakeOverrideSubstitution != null) {
            returnTypeRef = buildImplicitTypeRef()
            attributes.fakeOverrideSubstitution = fakeOverrideSubstitution
        } else {
            returnTypeRef = baseProperty.returnTypeRef.withReplacedReturnType(newReturnType)
        }
        receiverTypeRef = baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType)
    }

    fun createSubstitutionOverrideField(
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
            moduleData = session.moduleData
            this.symbol = symbol
            origin = FirDeclarationOrigin.SubstitutionOverride
            returnTypeRef = baseField.returnTypeRef.withReplacedConeType(newReturnType)

            source = baseField.source
            resolvePhase = baseField.resolvePhase
            name = baseField.name
            isVar = baseField.isVar
            status = baseField.status
            resolvePhase = baseField.resolvePhase
            annotations += baseField.annotations
            attributes = baseField.attributes.copy()
            dispatchReceiverType = baseField.dispatchReceiverType
        }.apply {
            originalForSubstitutionOverrideAttr = baseField
        }
        return symbol
    }

    fun createSubstitutionOverrideAccessor(
        session: FirSession,
        baseProperty: FirSyntheticProperty,
        baseSymbol: FirAccessorSymbol,
        newDispatchReceiverType: ConeKotlinType?,
        newReturnType: ConeKotlinType?,
        newGetterParameterTypes: List<ConeKotlinType?>?,
        newSetterParameterTypes: List<ConeKotlinType?>?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): FirAccessorSymbol {
        val getterSymbol = FirNamedFunctionSymbol(baseSymbol.accessorId)
        val getter = createSubstitutionOverrideFunction(
            getterSymbol,
            session,
            baseProperty.getter.delegate,
            newDispatchReceiverType,
            newReceiverType = null,
            newReturnType,
            newGetterParameterTypes,
            newTypeParameters = null,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        )
        val setterSymbol = FirNamedFunctionSymbol(baseSymbol.accessorId)
        val baseSetter = baseProperty.setter
        val setter = if (baseSetter == null) null else createSubstitutionOverrideFunction(
            setterSymbol,
            session,
            baseSetter.delegate,
            newDispatchReceiverType,
            newReceiverType = null,
            StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false),
            newSetterParameterTypes,
            newTypeParameters = null,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        )
        return buildSyntheticProperty {
            moduleData = session.moduleData
            name = baseProperty.name
            symbol = FirAccessorSymbol(baseSymbol.callableId, baseSymbol.accessorId)
            delegateGetter = getter
            delegateSetter = setter
            status = baseProperty.status
            deprecation = getDeprecationsFromAccessors(getter, setter, session.languageVersionSettings.apiVersion)
        }.symbol
    }

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: FirSession,
        member: FirTypeParameterRefsOwner,
        substitutor: ConeSubstitutor,
        forceTypeParametersRecreation: Boolean = true
    ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { typeParameterRef ->
            val typeParameter = typeParameterRef.symbol.fir
            FirTypeParameterBuilder().apply {
                source = typeParameter.source
                moduleData = typeParameter.moduleData
                origin = FirDeclarationOrigin.SubstitutionOverride
                resolvePhase = FirResolvePhase.DECLARATIONS
                name = typeParameter.name
                symbol = FirTypeParameterSymbol()
                variance = typeParameter.variance
                isReified = typeParameter.isReified
                annotations += typeParameter.annotations
            }
        }

        val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).associate { (original, new) ->
            Pair(original.symbol, ConeTypeParameterTypeImpl(new.symbol.toLookupTag(), isNullable = false))
        }

        val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters, useSiteSession)

        var wereChangesInTypeParameters = forceTypeParametersRecreation
        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
            val original = oldTypeParameter.symbol.fir
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
            newTypeParameters.map(FirTypeParameterBuilder::build),
            ChainedSubstitutor(substitutor, additionalSubstitutor)
        )
    }

    private sealed class Maybe<out A> {
        class Value<out A>(val value: A) : Maybe<A>()
        object Nothing : Maybe<kotlin.Nothing>()
    }
}
