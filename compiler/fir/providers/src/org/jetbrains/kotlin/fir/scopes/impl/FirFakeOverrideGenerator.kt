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
import org.jetbrains.kotlin.fir.scopes.CallableCopyDeferredReturnTypeCalculation
import org.jetbrains.kotlin.fir.scopes.CallableCopySubstitution
import org.jetbrains.kotlin.fir.scopes.callableCopyDeferredTypeCalculation
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
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
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newParameterTypes: List<ConeKotlinType?>? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseFunction.isExpect,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null,
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
            deferredReturnTypeCalculation = callableCopySubstitutionForTypeUpdater
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
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation? = null,
        newSource: KtSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source ?: baseFunction.source,
    ): FirSimpleFunction = buildSimpleFunction {
        source = newSource
        moduleData = session.nullableModuleData ?: baseFunction.moduleData
        this.origin = origin
        name = baseFunction.name
        status = baseFunction.status.copy(newVisibility, newModality, isExpect = isExpect, isOverride = true)
        symbol = newSymbol
        resolvePhase = origin.resolvePhaseForCopy

        dispatchReceiverType = newDispatchReceiverType
        attributes = baseFunction.attributes.copy()
        typeParameters += configureAnnotationsTypeParametersAndSignature(
            session, baseFunction, newParameterTypes, newTypeParameters,
            newReceiverType, newContextReceiverTypes, newReturnType, deferredReturnTypeCalculation, newSymbol,
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
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newTypeParameters: List<FirTypeParameterRef>?,
        isExpect: Boolean,
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation?,
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
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation?,
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
                    newContextReceiverTypes,
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
                    newContextReceiverTypes,
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
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredTypeCalculation: CallableCopyDeferredReturnTypeCalculation?,
        origin: FirDeclarationOrigin,
        copyDefaultValues: Boolean = false,
    ) {
        checkStatusIsResolved(baseFunction)
        annotations += baseFunction.annotations

        @Suppress("NAME_SHADOWING")
        val deferredTypeCalculation = deferredTypeCalculation
            ?: runIf(baseFunction.returnTypeRef is FirImplicitTypeRef) {
                CallableCopySubstitution(ConeSubstitutor.Empty, baseFunction.symbol)
            }

        if (deferredTypeCalculation != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.callableCopyDeferredTypeCalculation = deferredTypeCalculation
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
            buildCopyForValueParameter(
                valueParameter,
                valueParameter.returnTypeRef.withReplacedConeType(newType),
                origin,
                fakeFunctionSymbol,
                this@configureAnnotationsAndSignature.source ?: valueParameter.source,
                copyDefaultValues,
            )
        }

        contextReceivers += baseFunction.contextReceivers.zip(
            newContextReceiverTypes ?: List(baseFunction.contextReceivers.size) { null }
        ) { contextReceiver, newType ->
            buildContextReceiverCopy(contextReceiver) {
                typeRef = contextReceiver.typeRef.withReplacedConeType(newType)
            }
        }
    }

    private fun buildCopyForValueParameter(
        original: FirValueParameter,
        returnTypeRef: FirTypeRef,
        origin: FirDeclarationOrigin,
        containingFunctionSymbol: FirFunctionSymbol<*>,
        source: KtSourceElement?,
        copyDefaultValues: Boolean = true,
    ): FirValueParameter = buildValueParameterCopy(original) {
        this.origin = origin
        this.source = source
        this.returnTypeRef = returnTypeRef
        symbol = FirValueParameterSymbol(original.name)
        this.containingFunctionSymbol = containingFunctionSymbol
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
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newTypeParameters: List<FirTypeParameter>? = null,
        isExpect: Boolean = baseProperty.isExpect,
        callableCopySubstitutionForTypeUpdater: CallableCopySubstitution? = null,
    ): FirPropertySymbol {
        createCopyForFirProperty(
            symbolForSubstitutionOverride, baseProperty, derivedClassLookupTag, session, origin,
            isExpect, newDispatchReceiverType, newTypeParameters, newReceiverType, newContextReceiverTypes, newReturnType,
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
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation? = null,
        newSource: KtSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source ?: baseProperty.source,
    ): FirProperty = buildProperty {
        source = newSource
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
            newContextReceiverTypes,
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
            resolvePhase = origin.resolvePhaseForCopy,
        ).apply {
            replaceAnnotations(this@buildCopy.annotations)
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
            resolvePhase = origin.resolvePhaseForCopy,
        ).apply {
            replaceAnnotations(this@buildCopy.annotations)
        }
        else -> buildPropertyAccessorCopy(this) {
            this.symbol = FirPropertyAccessorSymbol()
            this.moduleData = moduleData
            this.origin = origin
            this.propertySymbol = propertySymbol
            this.dispatchReceiverType = dispatchReceiverType
            this.body = null
            resolvePhase = origin.resolvePhaseForCopy
        }.also {
            if (it.isSetter) {
                val originalParameter = it.valueParameters.first()
                val newParameter = buildCopyForValueParameter(
                    original = originalParameter,
                    returnTypeRef = propertyReturnTypeRef,
                    origin = origin,
                    containingFunctionSymbol = it.symbol,
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
        newContextReceiverTypes: List<ConeKotlinType?>? = null,
        newReturnType: ConeKotlinType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation? = null,
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
            baseField, newReceiverType, newContextReceiverTypes, newReturnType,
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
        newContextReceiverTypes: List<ConeKotlinType?>?,
        newReturnType: ConeKotlinType?,
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation?,
    ): List<FirTypeParameter> {
        return when {
            baseProperty.typeParameters.isEmpty() -> {
                configureAnnotationsAndSignature(
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, deferredReturnTypeCalculation
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
                    baseProperty, newReceiverType, newContextReceiverTypes, newReturnType, deferredReturnTypeCalculation
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
        substitutor: ConeSubstitutor,
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
        deferredReturnTypeCalculation: CallableCopyDeferredReturnTypeCalculation?,
        updateReceiver: Boolean = true,
    ) {
        checkStatusIsResolved(baseVariable)
        annotations += baseVariable.annotations

        @Suppress("NAME_SHADOWING")
        val deferredReturnTypeCalculation = deferredReturnTypeCalculation
            ?: runIf(baseVariable.returnTypeRef is FirImplicitTypeRef) {
                CallableCopySubstitution(ConeSubstitutor.Empty, baseVariable.symbol)
            }

        if (deferredReturnTypeCalculation != null) {
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            attributes.callableCopyDeferredTypeCalculation = deferredReturnTypeCalculation
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
        dispatchReceiverType = baseField.dispatchReceiverType
    }.apply {
        originalForSubstitutionOverrideAttr = baseField
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
    }.symbol

    // Returns a list of type parameters, and a substitutor that should be used for all other types
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: FirSession,
        member: FirTypeParameterRefsOwner,
        symbolForOverride: FirBasedSymbol<*>,
        substitutor: ConeSubstitutor,
        origin: FirDeclarationOrigin,
        forceTypeParametersRecreation: Boolean = true,
    ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
        if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
        val newTypeParameters = member.typeParameters.map { typeParameterRef ->
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
