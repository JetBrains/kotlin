/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.CallableCopySubstitution
import org.jetbrains.kotlin.fir.scopes.callableCopySubstitutionForTypeUpdater
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirFakeOverrideGenerator {
    fun createSubstitutionOverrideFunction(
        session: FirSession,
        symbolForSubstitutionOverride: FirNamedFunctionSymbol,
        baseFunction: FirSimpleFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseFunction.isExpect,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null
    ): FirNamedFunctionSymbol {
        createSubstitutionOverrideFunction(
            symbolForSubstitutionOverride, session, baseFunction, derivedClassLookupTag,
            newDispatchReceiverType, newReceiverType, newContextReceiverTypes,
            newReturnType, newParameterTypes, newTypeParameters,
            isExpect, callableCopySubstitutionForTypeUpdater, origin,
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
    ): FirSimpleFunction {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return createCopyForFirFunction(
            fakeOverrideSymbol,
            baseFunction,
            derivedClassLookupTag = derivedClassLookupTag,
            session,
            origin,
            isExpect,
            newDispatchReceiverType,
            newParameterTypes,
            newTypeParameters,
            newReceiverType,
            newContextReceiverTypes,
            newReturnType,
            callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null
    ): FirSimpleFunction {
        checkStatusIsResolved(baseFunction)

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
                newReceiverType, newContextReceiverTypes, newReturnType, callableCopySubstitutionForTypeUpdater, newSymbol
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?
    ): FirConstructor {
        checkStatusIsResolved(baseConstructor)

        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        return buildConstructor {
            annotations += baseConstructor.annotations
            moduleData = session.nullableModuleData ?: baseConstructor.moduleData
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
                callableCopySubstitutionForTypeUpdater,
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?,
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
                    callableCopySubstitutionForTypeUpdater,
                    origin,
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseFunction, symbolForOverride, ConeSubstitutor.Empty, origin
                )
                val copiedParameterTypes = baseFunction.valueParameters.map {
                    substitutor.substituteOrNull(it.returnTypeRef.coneType)
                }
                val symbol = baseFunction.symbol
                val (copiedReceiverType, copiedContextReceiverTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseFunction as FirCallableDeclaration, newReceiverType, newContextReceiverTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newCallableCopySubstitutionForTypeUpdater) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to CallableCopySubstitution(substitutor, symbol)
                }
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    copiedParameterTypes,
                    copiedReceiverType,
                    copiedContextReceiverTypes,
                    copiedReturnType,
                    newCallableCopySubstitutionForTypeUpdater,
                    origin,
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
                    callableCopySubstitutionForTypeUpdater,
                    origin,
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?,
        origin: FirDeclarationOrigin,
    ) {
        annotations += baseFunction.annotations

        @Suppress("NAME_SHADOWING")
        val callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
            ?: runIf(baseFunction.returnTypeRef is FirImplicitTypeRef) {
                CallableCopySubstitution(ConeSubstitutor.Empty, baseFunction.symbol)
            }

        if (callableCopySubstitutionForTypeUpdater != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
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
                this.origin = origin
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
        origin: FirDeclarationOrigin.SubstitutionOverride,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseProperty.isExpect,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null
    ): FirPropertySymbol {
        createCopyForFirProperty(
            symbolForSubstitutionOverride, baseProperty, derivedClassLookupTag, session, origin,
            isExpect, newDispatchReceiverType, newTypeParameters, newReceiverType, newContextReceiverTypes, newReturnType,
            callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
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
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null
    ): FirProperty {
        checkStatusIsResolved(baseProperty)

        return buildProperty {
            source = baseProperty.source
            moduleData = session.nullableModuleData ?: baseProperty.moduleData
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
                callableCopySubstitutionForTypeUpdater
            )
            deprecationsProvider = baseProperty.deprecationsProvider

            getter = baseProperty.getter?.buildCopyIfNeeded(
                moduleData = session.nullableModuleData ?: baseProperty.moduleData,
                origin = origin,
                propertyReturnTypeRef = this@buildProperty.returnTypeRef,
                propertySymbol = newSymbol,
                dispatchReceiverType = dispatchReceiverType,
                derivedClassLookupTag = derivedClassLookupTag,
                baseProperty = baseProperty,
            )

            setter = baseProperty.setter?.buildCopyIfNeeded(
                moduleData = session.nullableModuleData ?: baseProperty.moduleData,
                origin = origin,
                propertyReturnTypeRef = this@buildProperty.returnTypeRef,
                propertySymbol = newSymbol,
                dispatchReceiverType = dispatchReceiverType,
                derivedClassLookupTag = derivedClassLookupTag,
                baseProperty = baseProperty,
            )
        }.apply {
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
        }
    }

    private fun FirPropertyAccessor.buildCopyIfNeeded(
        moduleData: FirModuleData,
        origin: FirDeclarationOrigin,
        propertyReturnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        dispatchReceiverType: ConeSimpleKotlinType?,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        baseProperty: FirProperty,
    ) = when {
        annotations.isNotEmpty() || visibility != baseProperty.visibility -> buildCopy(
            moduleData,
            origin,
            propertyReturnTypeRef,
            propertySymbol,
            dispatchReceiverType,
            derivedClassLookupTag,
            baseProperty,
        )
        else -> null
    }

    private fun FirPropertyAccessor.buildCopy(
        moduleData: FirModuleData,
        origin: FirDeclarationOrigin,
        propertyReturnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        dispatchReceiverType: ConeSimpleKotlinType?,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        baseProperty: FirProperty,
    ) = when (this) {
        is FirDefaultPropertyGetter -> FirDefaultPropertyGetter(
            source = source,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = propertyReturnTypeRef,
            visibility = visibility,
            propertySymbol = propertySymbol,
            modality = modality ?: Modality.FINAL,
            effectiveVisibility = effectiveVisibility,
            resolvePhase = resolvePhase,
        ).apply {
            replaceAnnotations(annotations)
        }
        is FirDefaultPropertySetter -> FirDefaultPropertySetter(
            source = source,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = propertyReturnTypeRef,
            visibility = visibility,
            propertySymbol = propertySymbol,
            modality = modality ?: Modality.FINAL,
            effectiveVisibility = effectiveVisibility,
            resolvePhase = resolvePhase,
        ).apply {
            replaceAnnotations(annotations)
        }
        else -> buildPropertyAccessorCopy(this) {
            this.symbol = FirPropertyAccessorSymbol()
            this.moduleData = moduleData
            this.origin = origin
            this.propertySymbol = propertySymbol
            this.dispatchReceiverType = dispatchReceiverType
            this.body = null
        }.also {
            if (it.isSetter) {
                val newParameter = buildValueParameterCopy(it.valueParameters.first()) {
                    this.symbol = FirValueParameterSymbol(symbol.name)
                    this.returnTypeRef = propertyReturnTypeRef
                }
                it.replaceValueParameters(listOf(newParameter))
            } else {
                it.replaceReturnTypeRef(propertyReturnTypeRef)
            }
        }
    }.also { accessor ->
        when (accessor.origin) {
            FirDeclarationOrigin.IntersectionOverride -> accessor.originalForIntersectionOverrideAttr = this
            is FirDeclarationOrigin.SubstitutionOverride -> accessor.originalForSubstitutionOverrideAttr = this
            else -> {}
        }

        accessor.containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf {
            accessor is FirDefaultPropertyAccessor || shouldOverrideSetContainingClass(baseProperty)
        }
    }

    fun createCopyForFirField(
        newSymbol: FirFieldSymbol,
        baseField: FirField,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: FirSession,
        origin: FirDeclarationOrigin,
        isExpect: Boolean = baseField.isExpect,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        newReceiverType: ConeKotlinType? = null,
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null
    ): FirField {
        return buildField {
            source = baseField.source
            moduleData = session.nullableModuleData ?: baseField.moduleData
            this.origin = origin
            name = baseField.name
            isVar = baseField.isVar
            this.symbol = newSymbol
            status = baseField.status.copy(newVisibility, newModality, isExpect = isExpect)

            resolvePhase = baseField.resolvePhase
            dispatchReceiverType = newDispatchReceiverType
            attributes = baseField.attributes.copy()
            configureAnnotationsAndSignature(
                baseField, newReceiverType, newContextReceiverTypes, newReturnType,
                callableCopySubstitutionForTypeUpdater, updateReceiver = false
            )
            deprecationsProvider = baseField.deprecationsProvider
        }.apply {
            containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
        }
    }

    private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseProperty: FirProperty,
        newTypeParameters: List<FirTypeParameter>?,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, callableCopySubstitutionForTypeUpdater
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseProperty, symbol, ConeSubstitutor.Empty, origin,
                )
                val (copiedReceiverType, copiedContextReceiverTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newCallableCopySubstitutionForTypeUpdater) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to CallableCopySubstitution(substitutor, baseProperty.symbol)
                }
                configureAnnotationsAndSignature(
                    baseProperty, copiedReceiverType, copiedContextReceiverTypes,
                    copiedReturnType, newCallableCopySubstitutionForTypeUpdater
                )
                copiedTypeParameters.filterIsInstance<FirTypeParameter>()
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, callableCopySubstitutionForTypeUpdater
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

    private fun FirVariableBuilder.configureAnnotationsAndSignature(
        baseVariable: FirVariable,
        newReceiverType: ConeKotlinType?,
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution?,
        updateReceiver: Boolean = true
    ) {
        annotations += baseVariable.annotations

        @Suppress("NAME_SHADOWING")
        val callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
            ?: runIf(baseVariable.returnTypeRef is FirImplicitTypeRef) {
                CallableCopySubstitution(ConeSubstitutor.Empty, baseVariable.symbol)
            }

        if (callableCopySubstitutionForTypeUpdater != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.callableCopySubstitutionForTypeUpdater = callableCopySubstitutionForTypeUpdater
        } else {
            returnTypeRef = baseVariable.returnTypeRef.withReplacedReturnType(newReturnType)
        }

        if (updateReceiver) {
            receiverParameter = baseVariable.receiverParameter?.let { receiverParameter ->
                buildReceiverParameterCopy(receiverParameter) {
                    typeRef = receiverParameter.typeRef.withReplacedConeType(newReceiverType)
                }
            }
        }

        contextReceivers += baseVariable.contextReceivers.zip(
            newContextReceiverTypes ?: List(baseVariable.contextReceivers.size) { null }
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
        newReturnType: ConeKotlinType?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
    ): FirFieldSymbol {
        val symbol = FirFieldSymbol(CallableId(derivedClassLookupTag.classId, baseField.name))
        buildField {
            moduleData = session.nullableModuleData ?: baseField.moduleData
            this.symbol = symbol
            this.origin = origin
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

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: FirSession,
        member: FirTypeParameterRefsOwner,
        symbolForOverride: FirBasedSymbol<*>,
        substitutor: ConeSubstitutor,
        origin: FirDeclarationOrigin,
        forceTypeParametersRecreation: Boolean = true
    ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { typeParameterRef ->
            val typeParameter = typeParameterRef.symbol.fir
            FirTypeParameterBuilder().apply {
                source = typeParameter.source
                moduleData = typeParameter.moduleData
                this.origin = origin
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

    private fun checkStatusIsResolved(member: FirCallableDeclaration) {
        check(member.status is FirResolvedDeclarationStatus) {
            "Status should be resolved for a declaration to create it fake override, " +
                    "otherwise the status of the fake override will never be resolved." +
                    "The status was unresolved for ${member::class.java.simpleName}"
        }
    }
}
