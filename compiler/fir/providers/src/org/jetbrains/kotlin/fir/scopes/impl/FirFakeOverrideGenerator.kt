/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FakeOverrideSubstitution
import org.jetbrains.kotlin.fir.scopes.fakeOverrideSubstitution
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
        symbolForSubstitutionOverride: FirNamedFunctionSymbol,
        baseFunction: FirSimpleFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseFunction.isExpect,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirNamedFunctionSymbol {
        createSubstitutionOverrideFunction(
            symbolForSubstitutionOverride, session, baseFunction, derivedClassLookupTag, newDispatchReceiverType, newReceiverType,
            newContextReceiverTypes, newReturnType, newParameterTypes, newTypeParameters, isExpect, fakeOverrideSubstitution
        )
        return symbolForSubstitutionOverride
    }

    fun createSymbolForSubstitutionOverride(baseSymbol: FirNamedFunctionSymbol, derivedClassId: ClassId? = null): FirNamedFunctionSymbol {
        return if (derivedClassId == null) {
            FirNamedFunctionSymbol(baseSymbol.callableId)
        } else {
            FirNamedFunctionSymbol(CallableId(derivedClassId, baseSymbol.callableId.callableName))
        }
    }

    private fun createSubstitutionOverrideFunction(
        fakeOverrideSymbol: FirNamedFunctionSymbol,
        session: FirSession,
        baseFunction: FirSimpleFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
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
            derivedClassLookupTag = derivedClassLookupTag,
            session,
            FirDeclarationOrigin.SubstitutionOverride,
            isExpect,
            newDispatchReceiverType,
            newParameterTypes,
            newTypeParameters,
            newReceiverType,
            newContextReceiverTypes,
            newReturnType,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        ).apply {
            originalForSubstitutionOverrideAttr = baseFunction
        }
    }

    fun createCopyForFirFunction(
        newSymbol: FirNamedFunctionSymbol,
        baseFunction: FirSimpleFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseFunction.isExpect,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
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
            status = baseFunction.status.copy(newVisibility, newModality, isExpect = isExpect)
            symbol = newSymbol
            resolvePhase = baseFunction.resolvePhase

            dispatchReceiverType = newDispatchReceiverType
            attributes = baseFunction.attributes.copy()
            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session, baseFunction, newParameterTypes, newTypeParameters,
                newReceiverType, newContextReceiverTypes, newReturnType, fakeOverrideSubstitution, newSymbol
            ).filterIsInstance<FirTypeParameter>()
            deprecationsProvider = baseFunction.deprecationsProvider
        }.apply {
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseFunction) }
        }
    }

    fun createCopyForFirConstructor(
        fakeOverrideSymbol: FirConstructorSymbol,
        session: FirSession,
        baseConstructor: FirConstructor,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        origin: FirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        isExpect: Boolean,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): FirConstructor {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return buildConstructor {
            annotations += baseConstructor.annotations
            moduleData = session.moduleData
            this.origin = origin
            receiverParameter = baseConstructor.receiverParameter?.let { receiverParameter ->
                buildReceiverParameterCopy(receiverParameter) {
                    typeRef = receiverParameter.typeRef.withReplacedConeType(null)
                }
            }

            status = baseConstructor.status.copy(isExpect = isExpect)
            symbol = fakeOverrideSymbol

            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session,
                baseConstructor,
                newParameterTypes,
                newTypeParameters,
                newReceiverType = null,
                newContextReceiverTypes,
                newReturnType,
                fakeOverrideSubstitution,
                fakeOverrideSymbol
            )

            dispatchReceiverType = newDispatchReceiverType

            resolvePhase = baseConstructor.resolvePhase
            source = baseConstructor.source
            attributes = baseConstructor.attributes.copy()
            deprecationsProvider = baseConstructor.deprecationsProvider
        }.apply {
            originalForSubstitutionOverrideAttr = baseConstructor
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseConstructor) }
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseFunction: FirFunction,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?,
        symbolForOverride: FirFunctionSymbol<*>,
    ): List<FirTypeParameterRef> {
        return when {
            baseFunction.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    newParameterTypes,
                    newReceiverType,
                    newContextReceiverTypes,
                    newReturnType,
                    fakeOverrideSubstitution
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseFunction, symbolForOverride, ConeSubstitutor.Empty
                )
                val copiedParameterTypes = baseFunction.valueParameters.map {
                    substitutor.substituteOrNull(it.returnTypeRef.coneType)
                }
                val symbol = baseFunction.symbol
                val (copiedReceiverType, copiedContextReceiverTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseFunction as FirCallableDeclaration, newReceiverType, newContextReceiverTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newFakeOverrideSubstitution) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to FakeOverrideSubstitution(substitutor, symbol)
                }
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    copiedParameterTypes,
                    copiedReceiverType,
                    copiedContextReceiverTypes,
                    copiedReturnType,
                    newFakeOverrideSubstitution
                )
                copiedTypeParameters
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    newParameterTypes,
                    newReceiverType,
                    newContextReceiverTypes,
                    newReturnType,
                    fakeOverrideSubstitution
                )
                newTypeParameters
            }
        }
    }

    private fun FirFunctionBuilder.configureAnnotationsAndSignature(
        baseFunction: FirFunction,
        fakeFunctionSymbol: FirFunctionSymbol<*>,
        newParameterTypes: List<ConeKotlinType?>?,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?,
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
            receiverParameter = baseFunction.receiverParameter?.let { receiverParameter ->
                buildReceiverParameterCopy(receiverParameter) {
                    typeRef = receiverParameter.typeRef.withReplacedConeType(newReceiverType)
                }
            }
        }
        valueParameters += baseFunction.valueParameters.zip(
            newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
        ) { valueParameter, newType ->
            buildValueParameterCopy(valueParameter) {
                origin = FirDeclarationOrigin.SubstitutionOverride
                returnTypeRef = valueParameter.returnTypeRef.withReplacedConeType(newType)
                symbol = FirValueParameterSymbol(valueParameter.name)
                containingFunctionSymbol = fakeFunctionSymbol
            }.apply {
                originalForSubstitutionOverrideAttr = valueParameter
            }
        }

        contextReceivers += baseFunction.contextReceivers.zip(
            newContextReceiverTypes ?: List(baseFunction.contextReceivers.size) { null }
        ) { contextReceiver, newType ->
            buildContextReceiverCopy(contextReceiver) {
                typeRef = contextReceiver.typeRef.withReplacedConeType(newType)
            }
        }
    }

    fun createSubstitutionOverrideProperty(
        session: FirSession,
        symbolForSubstitutionOverride: FirPropertySymbol,
        baseProperty: FirProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseProperty.isExpect,
        fakeOverrideSubstitution: FakeOverrideSubstitution? = null
    ): FirPropertySymbol {
        createCopyForFirProperty(
            symbolForSubstitutionOverride, baseProperty, derivedClassLookupTag, session, FirDeclarationOrigin.SubstitutionOverride,
            isExpect, newDispatchReceiverType, newTypeParameters, newReceiverType, newContextReceiverTypes, newReturnType,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        ).apply {
            originalForSubstitutionOverrideAttr = baseProperty
        }
        return symbolForSubstitutionOverride
    }

    fun createSymbolForSubstitutionOverride(baseSymbol: FirPropertySymbol, derivedClassId: ClassId? = null): FirPropertySymbol {
        return if (derivedClassId == null) {
            FirPropertySymbol(baseSymbol.callableId)
        } else {
            FirPropertySymbol(CallableId(derivedClassId, baseSymbol.callableId.callableName))
        }
    }

    fun createCopyForFirProperty(
        newSymbol: FirPropertySymbol,
        baseProperty: FirProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseProperty.isExpect,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newTypeParameters: List<FirTypeParameter>? = null,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
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
            status = baseProperty.status.copy(newVisibility, newModality, isExpect = isExpect)

            resolvePhase = baseProperty.resolvePhase
            dispatchReceiverType = newDispatchReceiverType
            attributes = baseProperty.attributes.copy()
            typeParameters += configureAnnotationsTypeParametersAndSignature(
                session,
                baseProperty,
                newTypeParameters,
                newReceiverType,
                newContextReceiverTypes,
                newReturnType,
                fakeOverrideSubstitution
            )
            deprecationsProvider = baseProperty.deprecationsProvider
        }.apply {
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
        }
    }

    private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseProperty: FirProperty,
        newTypeParameters: List<FirTypeParameter>?,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, fakeOverrideSubstitution
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseProperty, symbol, ConeSubstitutor.Empty
                )
                val (copiedReceiverType, copiedContextReceiverTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newFakeOverrideSubstitution) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to FakeOverrideSubstitution(substitutor, baseProperty.symbol)
                }
                configureAnnotationsAndSignature(
                    baseProperty, copiedReceiverType, copiedContextReceiverTypes, copiedReturnType, newFakeOverrideSubstitution
                )
                copiedTypeParameters.filterIsInstance<FirTypeParameter>()
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, fakeOverrideSubstitution
                )
                newTypeParameters
            }
        }
    }

    private fun substituteReceiverAndReturnType(
        baseCallable: FirCallableDeclaration,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        substitutor: ConeSubstitutor
    ): Triple<ConeKotlinType?, List<ConeKotlinType?>, Maybe<ConeKotlinType?>> {
        val copiedReceiverType = newReceiverType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.receiverParameter?.typeRef?.let {
            substitutor.substituteOrNull(it.coneType)
        }

        val copiedContextReceiverTypes = newContextReceiverTypes?.map {
            it?.type?.let(substitutor::substituteOrNull)
        } ?: baseCallable.contextReceivers.map {
            substitutor.substituteOrNull(it.typeRef.coneType)
        }

        val copiedReturnType = newReturnType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.returnTypeRef.let {
            val coneType = baseCallable.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return Triple(
                copiedReceiverType,
                copiedContextReceiverTypes,
                Maybe.Nothing,
            )
            substitutor.substituteOrNull(coneType)
        }
        return Triple(copiedReceiverType, copiedContextReceiverTypes, Maybe.Value(copiedReturnType))
    }

    private fun FirPropertyBuilder.configureAnnotationsAndSignature(
        baseProperty: FirProperty,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
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

        receiverParameter = baseProperty.receiverParameter?.let { receiverParameter ->
            buildReceiverParameterCopy(receiverParameter) {
                typeRef = receiverParameter.typeRef.withReplacedConeType(newReceiverType)
            }
        }

        contextReceivers += baseProperty.contextReceivers.zip(
            newContextReceiverTypes ?: List(baseProperty.contextReceivers.size) { null }
        ) { contextReceiver, newType ->
            buildContextReceiverCopy(contextReceiver) {
                typeRef = contextReceiver.typeRef.withReplacedConeType(newType)
            }
        }
    }

    fun createSubstitutionOverrideField(
        session: FirSession,
        baseField: FirField,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        newReturnType: ConeKotlinType?
    ): FirFieldSymbol {
        val symbol = FirFieldSymbol(CallableId(derivedClassLookupTag.classId, baseField.name))
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
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
        }
        return symbol
    }

    fun createSubstitutionOverrideSyntheticProperty(
        session: FirSession,
        baseProperty: FirSyntheticProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        baseSymbol: FirSyntheticPropertySymbol,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        newGetterParameterTypes: List<ConeKotlinType?>?,
        newSetterParameterTypes: List<ConeKotlinType?>?,
        fakeOverrideSubstitution: FakeOverrideSubstitution?
    ): FirSyntheticPropertySymbol {
        val getterSymbol = FirNamedFunctionSymbol(baseSymbol.getterId)
        val getter = createSubstitutionOverrideFunction(
            getterSymbol,
            session,
            baseProperty.getter.delegate,
            derivedClassLookupTag,
            newDispatchReceiverType,
            newReceiverType = null,
            newContextReceiverTypes,
            newReturnType,
            newGetterParameterTypes,
            newTypeParameters = null,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        )
        val setterSymbol = FirNamedFunctionSymbol(baseSymbol.getterId)
        val baseSetter = baseProperty.setter
        val setter = if (baseSetter == null) null else createSubstitutionOverrideFunction(
            setterSymbol,
            session,
            baseSetter.delegate,
            derivedClassLookupTag,
            newDispatchReceiverType,
            newReceiverType = null,
            newContextReceiverTypes,
            StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false),
            newSetterParameterTypes,
            newTypeParameters = null,
            fakeOverrideSubstitution = fakeOverrideSubstitution
        )
        return buildSyntheticProperty {
            moduleData = session.moduleData
            name = baseProperty.name
            symbol = baseSymbol.copy()
            delegateGetter = getter
            delegateSetter = setter
            status = baseProperty.status
            deprecationsProvider = getDeprecationsProviderFromAccessors(session, getter, setter)
        }.apply {
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
        }.symbol
    }

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: FirSession,
        member: FirTypeParameterRefsOwner,
        symbolForOverride: FirBasedSymbol<*>,
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
                containingDeclarationSymbol = symbolForOverride
            }
        }

        val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).associate { (original, new) ->
            Pair(original.symbol, ConeTypeParameterTypeImpl(new.symbol.toLookupTag(), isNullable = false))
        }

        val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters, useSiteSession)

        var wereChangesInTypeParameters = forceTypeParametersRecreation
        for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
            val original = oldTypeParameter.symbol.fir
            for (boundTypeRef in original.symbol.resolvedBounds) {
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

    private fun shouldOverrideSetContainingClass(baseDeclaration: FirCallableDeclaration): Boolean {
        return baseDeclaration is FirConstructor
                || baseDeclaration.isStatic
                || baseDeclaration.containingClassForStaticMemberAttr != null
    }

    private sealed class Maybe<out A> {
        class Value<out A>(val value: A) : Maybe<A>()
        object Nothing : Maybe<kotlin.Nothing>()
    }
}
