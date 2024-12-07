/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.DeferredCallableCopyReturnType
import org.jetbrains.kotlin.fir.scopes.deferredCallableCopyReturnType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

object FirFakeOverrideGenerator {
    fun createSubstitutionOverrideFunction(
        session: FirSession,
        symbolForSubstitutionOverride: FirNamedFunctionSymbol,
        baseFunction: FirSimpleFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
        newReceiverType: ConeKotlinType? = null,
        newContextParameterTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseFunction.isExpect,
        callableCopySubstitutionForTypeUpdater: DeferredCallableCopyReturnType? = null,
    ): FirNamedFunctionSymbol {
        createSubstitutionOverrideFunction(
            symbolForSubstitutionOverride, session, baseFunction, derivedClassLookupTag,
            newDispatchReceiverType, newReceiverType, newContextParameterTypes,
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
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameter>?,
        isExpect: Boolean = baseFunction.isExpect,
        callableCopySubstitutionForTypeUpdater: DeferredCallableCopyReturnType?,
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
            newContextParameterTypes,
            newReturnType,
            deferredReturnTypeCalculation = callableCopySubstitutionForTypeUpdater,
            markAsOverride = true
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
        newTypeParameters: List<FirTypeParameterRef>? = null,
        newReceiverType: ConeKotlinType? = null,
        newContextParameterTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
        newSource: KtSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source ?: baseFunction.source,
        markAsOverride: Boolean,
    ): FirSimpleFunction = buildSimpleFunction {
        source = newSource
        moduleData = session.nullableModuleData ?: baseFunction.moduleData
        this.origin = origin
        name = baseFunction.name
        status = baseFunction.status.copy(
            newVisibility,
            newModality,
            isExpect = isExpect,
            isOverride = if (markAsOverride) true else baseFunction.status.isOverride
        )
        symbol = newSymbol
        resolvePhase = origin.resolvePhaseForCopy

        dispatchReceiverType = newDispatchReceiverType
        attributes = baseFunction.attributes.copy()
        typeParameters += configureAnnotationsTypeParametersAndSignature(
            session, baseFunction, newParameterTypes, newTypeParameters,
            newReceiverType, newContextParameterTypes, newReturnType, deferredReturnTypeCalculation, newSymbol,
            copyDefaultValues = false,
        ).filterIsInstance<FirTypeParameter>()
        deprecationsProvider = baseFunction.deprecationsProvider
    }.apply {
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseFunction) }
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
        newContextParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        isExpect: Boolean,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
        newSource: KtSourceElement? = null,
    ): FirConstructor = buildConstructor {
        // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
        // As second alternative, we can invent some light-weight kind of FirRegularClass
        annotations += baseConstructor.annotations
        source = newSource ?: derivedClassLookupTag?.toSymbol(session)?.source ?: baseConstructor.source
        moduleData = session.nullableModuleData ?: baseConstructor.moduleData
        this.origin = origin
        receiverParameter = baseConstructor.receiverParameter?.let { receiverParameter ->
            buildReceiverParameterCopy(receiverParameter) {
                typeRef = receiverParameter.typeRef.withReplacedConeType(null)
                symbol = FirReceiverParameterSymbol()
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
            newContextParameterTypes,
            newReturnType,
            deferredReturnTypeCalculation,
            fakeOverrideSymbol,
            // Copying default values here is important, because constructors don't
            // override anything and we rely on this fact when mapping arguments
            // during resolution.
            // See: FirDefaultParametersResolver.declaresDefaultValue()
            // See: testData/diagnostics/linked/declarations/classifier-declaration/class-declaration/constructor-declaration/p-5/pos/3.4.kt
            copyDefaultValues = true,
        )

        dispatchReceiverType = newDispatchReceiverType

        resolvePhase = origin.resolvePhaseForCopy
        attributes = baseConstructor.attributes.copy()
        deprecationsProvider = baseConstructor.deprecationsProvider
    }.apply {
        originalForSubstitutionOverrideAttr = baseConstructor
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseConstructor) }
    }

    private fun FirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseFunction: FirFunction,
        newParameterTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        newReceiverType: ConeKotlinType?,
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
        symbolForOverride: FirFunctionSymbol<*>,
        copyDefaultValues: Boolean = false,
    ): List<FirTypeParameterRef> {
        return when {
            baseFunction.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    newParameterTypes,
                    newReceiverType,
                    newContextParameterTypes,
                    newReturnType,
                    deferredReturnTypeCalculation,
                    origin,
                    copyDefaultValues,
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
                val (copiedReceiverType, copiedContextParameterTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseFunction as FirCallableDeclaration, newReceiverType, newContextParameterTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newCallableCopySubstitutionForTypeUpdater) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to DeferredReturnTypeOfSubstitution(substitutor, symbol)
                }
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    copiedParameterTypes,
                    copiedReceiverType,
                    copiedContextParameterTypes,
                    copiedReturnType,
                    newCallableCopySubstitutionForTypeUpdater,
                    origin,
                    copyDefaultValues,
                )
                copiedTypeParameters
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseFunction,
                    symbolForOverride,
                    newParameterTypes,
                    newReceiverType,
                    newContextParameterTypes,
                    newReturnType,
                    deferredReturnTypeCalculation,
                    origin,
                    copyDefaultValues,
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
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredTypeCalculation: DeferredCallableCopyReturnType?,
        origin: FirDeclarationOrigin,
        copyDefaultValues: Boolean = false,
    ) {
        checkStatusIsResolved(baseFunction)
        annotations += baseFunction.annotations

        @Suppress("NAME_SHADOWING")
        val deferredTypeCalculation = deferredTypeCalculation
            ?: runIf(baseFunction.returnTypeRef is FirImplicitTypeRef) {
                DeferredReturnTypeOfSubstitution(ConeSubstitutor.Empty, baseFunction.symbol)
            }

        if (deferredTypeCalculation != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.deferredCallableCopyReturnType = deferredTypeCalculation
        } else {
            returnTypeRef = baseFunction.returnTypeRef.withReplacedReturnType(newReturnType)
        }

        if (this is FirSimpleFunctionBuilder) {
            receiverParameter = baseFunction.receiverParameter?.let { receiverParameter ->
                buildReceiverParameterCopy(receiverParameter) {
                    typeRef = receiverParameter.typeRef.withReplacedConeType(newReceiverType)
                    symbol = FirReceiverParameterSymbol()
                }
            }
        }

        valueParameters += baseFunction.valueParameters.zip(
            newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
        ) { valueParameter, newType ->
            buildCopyForValueParameter(
                valueParameter,
                valueParameter.returnTypeRef.withReplacedConeType(newType),
                origin,
                fakeFunctionSymbol,
                this@configureAnnotationsAndSignature.source ?: valueParameter.source,
                copyDefaultValues,
            )
        }

        contextParameters += baseFunction.contextParameters.zip(
            newContextParameterTypes ?: List(baseFunction.contextParameters.size) { null }
        ) { contextParameter, newType ->
            buildValueParameterCopy(contextParameter) {
                symbol = FirValueParameterSymbol(name)
                returnTypeRef = contextParameter.returnTypeRef.withReplacedConeType(newType)
            }
        }
    }

    private fun buildCopyForValueParameter(
        original: FirValueParameter,
        returnTypeRef: FirTypeRef,
        origin: FirDeclarationOrigin,
        containingDeclarationSymbol: FirFunctionSymbol<*>,
        source: KtSourceElement?,
        copyDefaultValues: Boolean = true,
    ): FirValueParameter = buildValueParameterCopy(original) {
        this.origin = origin
        this.source = source
        this.returnTypeRef = returnTypeRef
        symbol = FirValueParameterSymbol(original.name)
        this.containingDeclarationSymbol = containingDeclarationSymbol
        defaultValue = defaultValue
            ?.takeIf { copyDefaultValues }
            ?.let {
                buildExpressionStub {
                    coneTypeOrNull = returnTypeRef.coneTypeOrNull
                }
            }

        resolvePhase = origin.resolvePhaseForCopy
    }

    fun createSubstitutionOverrideProperty(
        session: FirSession,
        symbolForSubstitutionOverride: FirPropertySymbol,
        baseProperty: FirProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
        newReceiverType: ConeKotlinType? = null,
        newContextParameterTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseProperty.isExpect,
        callableCopySubstitutionForTypeUpdater: DeferredCallableCopyReturnType? = null,
    ): FirPropertySymbol {
        createCopyForFirProperty(
            symbolForSubstitutionOverride, baseProperty, derivedClassLookupTag, session, origin,
            isExpect, newDispatchReceiverType, newTypeParameters, newReceiverType, newContextParameterTypes, newReturnType,
            deferredReturnTypeCalculation = callableCopySubstitutionForTypeUpdater
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
        newContextParameterTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        newSetterVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
        newSource: KtSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source,
    ): FirProperty = buildProperty {
        source = newSource ?: baseProperty.source
        moduleData = session.nullableModuleData ?: baseProperty.moduleData
        this.origin = origin
        name = baseProperty.name
        isVar = baseProperty.isVar
        this.symbol = newSymbol
        isLocal = false
        status = baseProperty.status.copy(newVisibility, newModality, isExpect = isExpect, isOverride = true)

        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseProperty.attributes.copy()
        typeParameters += configureAnnotationsTypeParametersAndSignature(
            session,
            baseProperty,
            newTypeParameters,
            newReceiverType,
            newContextParameterTypes,
            newReturnType,
            deferredReturnTypeCalculation
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
            newSource = newSource ?: baseProperty.getter?.source,
        )

        setter = baseProperty.setter?.let { setter ->
            setter.buildCopyIfNeeded(
                moduleData = session.nullableModuleData ?: baseProperty.moduleData,
                origin = origin,
                propertyReturnTypeRef = this@buildProperty.returnTypeRef,
                propertySymbol = newSymbol,
                dispatchReceiverType = dispatchReceiverType,
                derivedClassLookupTag = derivedClassLookupTag,
                baseProperty = baseProperty,
                newSource = newSource ?: baseProperty.setter?.source,
                newSetterVisibility ?: setter.visibility,
            )
        }
    }.apply {
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
    }

    private fun FirPropertyAccessor.buildCopyIfNeeded(
        moduleData: FirModuleData,
        origin: FirDeclarationOrigin,
        propertyReturnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        dispatchReceiverType: ConeSimpleKotlinType?,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        baseProperty: FirProperty,
        newSource: KtSourceElement? = source,
        newVisibility: Visibility = visibility,
    ) = when {
        annotations.isNotEmpty() || newVisibility != baseProperty.visibility ||
                origin == FirDeclarationOrigin.Delegated || origin is FirDeclarationOrigin.SubstitutionOverride -> buildCopy(
            moduleData,
            origin,
            propertyReturnTypeRef,
            propertySymbol,
            dispatchReceiverType,
            derivedClassLookupTag,
            baseProperty,
            newSource,
            newVisibility,
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
        newSource: KtSourceElement? = source,
        newVisibility: Visibility = visibility,
    ) = when (this) {
        is FirDefaultPropertyGetter -> FirDefaultPropertyGetter(
            source = newSource,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = propertyReturnTypeRef,
            visibility = newVisibility,
            propertySymbol = propertySymbol,
            modality = modality ?: Modality.FINAL,
            effectiveVisibility = effectiveVisibility,
            resolvePhase = origin.resolvePhaseForCopy,
            isOverride = true,
            attributes = attributes.copy(),
        ).apply {
            replaceAnnotations(this@buildCopy.annotations)
        }
        is FirDefaultPropertySetter -> FirDefaultPropertySetter(
            source = newSource,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = propertyReturnTypeRef,
            visibility = newVisibility,
            propertySymbol = propertySymbol,
            modality = modality ?: Modality.FINAL,
            effectiveVisibility = effectiveVisibility,
            resolvePhase = origin.resolvePhaseForCopy,
            parameterSource = valueParameters.first().source,
            isOverride = true,
            attributes = attributes.copy(),
        ).apply {
            replaceAnnotations(this@buildCopy.annotations)
        }
        else -> buildPropertyAccessorCopy(this) {
            this.source = newSource
            this.symbol = FirPropertyAccessorSymbol()
            this.moduleData = moduleData
            this.origin = origin
            this.propertySymbol = propertySymbol
            this.dispatchReceiverType = dispatchReceiverType
            this.body = null
            resolvePhase = origin.resolvePhaseForCopy
            this.status = status.copy(visibility = newVisibility)
            this.attributes = this@buildCopy.attributes.copy()
        }.also {
            if (it.isSetter) {
                val originalParameter = it.valueParameters.first()
                val newParameter = buildCopyForValueParameter(
                    original = originalParameter,
                    returnTypeRef = propertyReturnTypeRef,
                    origin = origin,
                    containingDeclarationSymbol = it.symbol,
                    source = originalParameter.source,
                )
                it.replaceValueParameters(listOf(newParameter))
            } else {
                it.replaceReturnTypeRef(propertyReturnTypeRef)
            }
        }
    }.also { accessor ->
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
        newContextParameterTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
    ): FirField = buildField {
        source = baseField.source
        moduleData = session.nullableModuleData ?: baseField.moduleData
        this.origin = origin
        name = baseField.name
        isVar = baseField.isVar
        this.symbol = newSymbol
        status = baseField.status.copy(newVisibility, newModality, isExpect = isExpect)

        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseField.attributes.copy()
        configureAnnotationsAndSignature(
            baseField, newReceiverType, newContextParameterTypes, newReturnType,
            deferredReturnTypeCalculation, updateReceiver = false
        )
        deprecationsProvider = baseField.deprecationsProvider
    }.apply {
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
    }

    private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: FirSession,
        baseProperty: FirProperty,
        newTypeParameters: List<FirTypeParameter>?,
        newReceiverType: ConeKotlinType?,
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextParameterTypes, newReturnType, deferredReturnTypeCalculation
                )
                emptyList()
            }
            newTypeParameters == null -> {
                val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                    useSiteSession, baseProperty, symbol, ConeSubstitutor.Empty, origin,
                )
                val (copiedReceiverType, copiedContextParameterTypes, possibleReturnType) = substituteReceiverAndReturnType(
                    baseProperty, newReceiverType, newContextParameterTypes, newReturnType, substitutor
                )
                val (copiedReturnType, newCallableCopySubstitutionForTypeUpdater) = when (possibleReturnType) {
                    is Maybe.Value -> possibleReturnType.value to null
                    else -> null to DeferredReturnTypeOfSubstitution(substitutor, baseProperty.symbol)
                }
                configureAnnotationsAndSignature(
                    baseProperty, copiedReceiverType, copiedContextParameterTypes,
                    copiedReturnType, newCallableCopySubstitutionForTypeUpdater
                )
                copiedTypeParameters.filterIsInstance<FirTypeParameter>()
            }
            else -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextParameterTypes, newReturnType, deferredReturnTypeCalculation
                )
                newTypeParameters
            }
        }
    }

    private fun substituteReceiverAndReturnType(
        baseCallable: FirCallableDeclaration,
        newReceiverType: ConeKotlinType?,
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        substitutor: ConeSubstitutor,
    ): Triple<ConeKotlinType?, List<ConeKotlinType?>, Maybe<ConeKotlinType?>> {
        val copiedReceiverType = newReceiverType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.receiverParameter?.typeRef?.let {
            substitutor.substituteOrNull(it.coneType)
        }

        val copiedContextParameterTypes = newContextParameterTypes?.map {
            it?.let(substitutor::substituteOrNull)
        } ?: baseCallable.contextParameters.map {
            substitutor.substituteOrNull(it.returnTypeRef.coneType)
        }

        val copiedReturnType = newReturnType?.let {
            substitutor.substituteOrNull(it)
        } ?: baseCallable.returnTypeRef.let {
            val coneType = baseCallable.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return Triple(
                copiedReceiverType,
                copiedContextParameterTypes,
                Maybe.Nothing,
            )
            substitutor.substituteOrNull(coneType)
        }
        return Triple(copiedReceiverType, copiedContextParameterTypes, Maybe.Value(copiedReturnType))
    }

    private fun FirVariableBuilder.configureAnnotationsAndSignature(
        baseVariable: FirVariable,
        newReceiverType: ConeKotlinType?,
        newContextParameterTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
        updateReceiver: Boolean = true,
    ) {
        checkStatusIsResolved(baseVariable)
        annotations += baseVariable.annotations

        @Suppress("NAME_SHADOWING")
        val deferredReturnTypeCalculation = deferredReturnTypeCalculation
            ?: runIf(baseVariable.returnTypeRef is FirImplicitTypeRef) {
                DeferredReturnTypeOfSubstitution(ConeSubstitutor.Empty, baseVariable.symbol)
            }

        if (deferredReturnTypeCalculation != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
        } else {
            returnTypeRef = baseVariable.returnTypeRef.withReplacedReturnType(newReturnType)
        }

        if (updateReceiver) {
            receiverParameter = baseVariable.receiverParameter?.let { receiverParameter ->
                buildReceiverParameterCopy(receiverParameter) {
                    typeRef = receiverParameter.typeRef.withReplacedConeType(newReceiverType)
                    symbol = FirReceiverParameterSymbol()
                }
            }
        }

        contextParameters += baseVariable.contextParameters.zip(
            newContextParameterTypes ?: List(baseVariable.contextParameters.size) { null }
        ) { contextParameter, newType ->
            buildValueParameterCopy(contextParameter) {
                symbol = FirValueParameterSymbol(name)
                returnTypeRef = contextParameter.returnTypeRef.withReplacedConeType(newType)
            }
        }
    }

    fun createSubstitutionOverrideField(
        session: FirSession,
        baseField: FirField,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        newReturnType: ConeKotlinType? = null,
        newDispatchReceiverType: ConeSimpleKotlinType?,
        origin: FirDeclarationOrigin.SubstitutionOverride,
    ): FirFieldSymbol = buildField {
        val symbol = FirFieldSymbol(CallableId(derivedClassLookupTag.classId, baseField.name))
        moduleData = session.nullableModuleData ?: baseField.moduleData
        this.symbol = symbol
        this.origin = origin
        returnTypeRef = baseField.returnTypeRef.withReplacedConeType(newReturnType)

        source = baseField.source
        name = baseField.name
        isVar = baseField.isVar
        status = baseField.status
        resolvePhase = origin.resolvePhaseForCopy
        annotations += baseField.annotations
        attributes = baseField.attributes.copy()
        dispatchReceiverType = newDispatchReceiverType
    }.apply {
        originalForSubstitutionOverrideAttr = baseField
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
    }.symbol

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: FirSession,
        original: FirTypeParameterRefsOwner,
        symbolForOverride: FirBasedSymbol<*>,
        substitutor: ConeSubstitutor,
        origin: FirDeclarationOrigin,
        forceTypeParametersRecreation: Boolean = true,
    ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
        if (original.typeParameters.isEmpty()) return Pair(original.typeParameters, substitutor)
        val newTypeParameters = original.typeParameters.map { typeParameterRef ->
            val typeParameter = typeParameterRef.symbol.fir
            FirTypeParameterBuilder().apply {
                source = typeParameter.source
                moduleData = typeParameter.moduleData
                this.origin = origin
                resolvePhase = origin.resolvePhaseForCopy
                name = typeParameter.name
                symbol = FirTypeParameterSymbol()
                variance = typeParameter.variance
                isReified = typeParameter.isReified
                annotations += typeParameter.annotations
                containingDeclarationSymbol = symbolForOverride
            }
        }

        /**
         * Our final substitutor will serve two functions:
         *
         * 1. Substitute class type parameters according to the provided [substitutor].
         * 2. Substitute references to the declaration's own type parameters with copies declared in the substitution override
         *    (e.g., in upper bounds of type parameters, receiver, parameter and return types).
         *
         * There are two scenarios to consider:
         *
         * Regular function:
         * ```kt
         * class Foo<A> {
         *     fun <B : A> bar(foo: Foo<B>) {}
         * }
         * ```
         * substituted with `{A -> B}`
         *
         * In this scenario, we want to produce
         *
         * ```kt
         *     fun <B_ : B> bar(foo: Foo<B_>) {}
         * ```
         *
         * Notice that there is no loop in the upper bound of the fake override's type parameter `B_`
         * because it refers to the `B` from the unsubstituted `bar`.
         * Having a loop would lead to problems like infinite recursion when iterating all bounds recursively (KT-70389).
         * To achieve this, the order of substitution must be own type parameters, then class parameters.
         * If it was reversed, in the example above we would substitute the upper bound of `B_ : A` with `{ A -> B -> B_ }`
         * which would produce `B_ : B_`.
         *
         * The second scenario is substituted constructors, called through a typealias or an inner class of a generic outer:
         * ```kt
         * public class Outer<T> {
         *     public class Inner<E> {
         *         public <F extends E, G extends T> Inner(E x, java.util.List<F> y, G z) {}
         *     }
         * }
         *
         * Outer<Int>().Inner("", listOf<String>(), 1)
         * ```
         *
         * Constructors of generic classes are generic as well,
         * however the type parameters from the outer class are represented as [FirConstructedClassTypeParameterRef].
         * In the case of Java, they can also have their own type parameters.
         *
         * The substituted constructor should have the following signature:
         * ```kt
         * <E_, F_ : E_, G_ : Int> constructor(x: E_, y: List<F_>, z: G_): Inner<E_, Int>
         * ```
         * To achieve this, we substitute with `{F -> F_ | G -> G_} then {T -> kotlin/Int} then {E -> E_}`.
         * We apply own type parameters, then class parameters from outer types, then own class type parameters.
         *
         * Applying own type parameters first but own class type parameters last is necessary for the following scenario:
         *
         * ```kt
         * class TColl<T, C : Collection<T>>
         * typealias TC<T1, T2> = TColl<T1, T2>
         * ```
         *
         * The substitution is `{T -> T1 | C -> T2} then {T -> T_ | C -> C_}`.
         * The type parameter types are affected by both the class substitution and the substitution from original to copied declaration,
         * but the substitution of the class takes precedence.
         * The result is:
         *
         * ```kt
         * <T_, C_ : Collection<T1> constructor(): TColl<T1, T2>
         * ```
         */
        val (ownTypeParameters, constructedClassTypeParameters) = original.typeParameters
            .zip(newTypeParameters)
            .partition { it.first !is FirConstructedClassTypeParameterRef }

        fun substitutorFrom(
            pairs: List<Pair<FirTypeParameterRef, FirTypeParameterBuilder>>,
            useSiteSession: FirSession,
        ): ConeSubstitutor = substitutorByMap(
            pairs.associate { (originalTypeParameter, new) ->
                Pair(originalTypeParameter.symbol, ConeTypeParameterTypeImpl(new.symbol.toLookupTag(), isMarkedNullable = false))
            },
            useSiteSession
        )

        val chainedSubstitutor = ChainedSubstitutor(
            substitutorFrom(ownTypeParameters, useSiteSession),
            ChainedSubstitutor(
                substitutor,
                substitutorFrom(constructedClassTypeParameters, useSiteSession)
            )
        )

        var wereChangesInTypeParameters = forceTypeParametersRecreation
        for ((newTypeParameter, originalTypeParameter) in newTypeParameters.zip(original.typeParameters)) {
            for (boundTypeRef in originalTypeParameter.symbol.resolvedBounds) {
                val typeForBound = boundTypeRef.coneType
                val substitutedBound = chainedSubstitutor.substituteOrNull(typeForBound)
                if (substitutedBound != null) {
                    wereChangesInTypeParameters = true
                }
                newTypeParameter.bounds +=
                    buildResolvedTypeRef {
                        source = boundTypeRef.source
                        coneType = substitutedBound ?: typeForBound
                    }
            }
        }

        if (!wereChangesInTypeParameters) return Pair(original.typeParameters, substitutor)
        return Pair(
            newTypeParameters.map(FirTypeParameterBuilder::build),
            chainedSubstitutor
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
        checkWithAttachment(
            member.status is FirResolvedDeclarationStatus,
            message = {
                "Status should be resolved for a declaration to create its fake override, " +
                        "otherwise the status of the fake override will never be resolved." +
                        "The status was unresolved for ${member::class.java.simpleName}"
            }
        ) {
            withFirEntry("declaration", member)
            withEntry("declarationStatus", member.status) { it.toString() }
        }
    }

    /**
     * In Low Level FIR we cannot be sure that all copied elements are already resolved,
     * so we play safe with [FirResolvePhase.STATUS] phase in such cases.
     * Example:
     * ```kotlin
     * class MyClass : BaseClass<@Anno("super $constant") Int>()
     *
     * abstract class BaseClass<T : @Anno("bound $constant") Number> {
     *     var property: SCHEME
     * }
     * ```
     * here we can have `BaseClass.property` already in [FirResolvePhase.BODY_RESOLVE] phase,
     * but `MyClass` in [FirResolvePhase.STATUS].
     * So [FirResolvePhase.BODY_RESOLVE] on the fake override will lead to the problem because
     * effectively we will have unresolved `@Anno("super $constant")` annotation inside "fully resolve"
     * function
     */
    private val FirDeclarationOrigin.resolvePhaseForCopy: FirResolvePhase
        get() = if (isLazyResolvable) FirResolvePhase.STATUS else FirResolvePhase.BODY_RESOLVE
}
