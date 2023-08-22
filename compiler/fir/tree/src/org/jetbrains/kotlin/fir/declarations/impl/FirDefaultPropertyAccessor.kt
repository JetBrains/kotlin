/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef

@OptIn(FirImplementationDetail::class)
abstract class FirDefaultPropertyAccessor(
    source: KtSourceElement?,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    valueParameters: MutableList<FirValueParameter>,
    propertySymbol: FirPropertySymbol,
    isGetter: Boolean,
    visibility: Visibility,
    modality: Modality = Modality.FINAL,
    effectiveVisibility: EffectiveVisibility? = null,
    isInline: Boolean = false,
    symbol: FirPropertyAccessorSymbol,
    resolvePhase: FirResolvePhase,
) : FirPropertyAccessorImpl(
    source,
    resolvePhase,
    moduleData,
    origin,
    FirDeclarationAttributes(),
    status = when (effectiveVisibility) {
        null -> FirDeclarationStatusImpl(visibility, modality)
        else -> FirResolvedDeclarationStatusImpl(visibility, modality, effectiveVisibility)
    }.apply {
        this.isInline = isInline
    },
    propertyTypeRef,
    deprecationsProvider = UnresolvedDeprecationProvider,
    containerSource = null,
    dispatchReceiverType = null,
    contextReceivers = MutableOrEmptyList.empty(),
    valueParameters,
    body = null,
    contractDescription = FirEmptyContractDescription,
    symbol,
    propertySymbol,
    isGetter,
    annotations = MutableOrEmptyList.empty(),
    typeParameters = mutableListOf(),
) {
    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = propertySymbol.dispatchReceiverType

    final override var body: FirBlock?
        get() = null
        set(_) {}

    companion object {
        fun createGetterOrSetter(
            source: KtSourceElement?,
            moduleData: FirModuleData,
            origin: FirDeclarationOrigin,
            propertyTypeRef: FirTypeRef,
            visibility: Visibility,
            propertySymbol: FirPropertySymbol,
            isGetter: Boolean,
            parameterAnnotations: List<FirAnnotation> = emptyList(),
        ): FirDefaultPropertyAccessor {
            return if (isGetter) {
                FirDefaultPropertyGetter(source, moduleData, origin, propertyTypeRef, visibility, propertySymbol, Modality.FINAL)
            } else {
                FirDefaultPropertySetter(
                    source, moduleData, origin, propertyTypeRef, visibility, propertySymbol, Modality.FINAL,
                    parameterAnnotations = parameterAnnotations
                )
            }
        }
    }
}

class FirDefaultPropertyGetter(
    source: KtSourceElement?,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    propertySymbol: FirPropertySymbol,
    modality: Modality = Modality.FINAL,
    effectiveVisibility: EffectiveVisibility? = null,
    isInline: Boolean = false,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
) : FirDefaultPropertyAccessor(
    source,
    moduleData,
    origin,
    propertyTypeRef,
    valueParameters = mutableListOf(),
    propertySymbol,
    isGetter = true,
    visibility = visibility,
    modality = modality,
    effectiveVisibility = effectiveVisibility,
    isInline = isInline,
    symbol = symbol,
    resolvePhase = resolvePhase,
)

class FirDefaultPropertySetter(
    source: KtSourceElement?,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility,
    propertySymbol: FirPropertySymbol,
    modality: Modality = Modality.FINAL,
    effectiveVisibility: EffectiveVisibility? = null,
    isInline: Boolean = false,
    propertyAccessorSymbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
    parameterAnnotations: List<FirAnnotation> = emptyList(),
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
) : FirDefaultPropertyAccessor(
    source,
    moduleData,
    origin,
    FirImplicitUnitTypeRef(source),
    valueParameters = mutableListOf(
        buildDefaultSetterValueParameter builder@{
            this@builder.resolvePhase = resolvePhase
            this@builder.source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
            this@builder.containingFunctionSymbol = propertyAccessorSymbol
            this@builder.moduleData = moduleData
            this@builder.origin = origin
            this@builder.returnTypeRef = propertyTypeRef
            this@builder.symbol = FirValueParameterSymbol(StandardNames.DEFAULT_VALUE_PARAMETER)
            this@builder.annotations += parameterAnnotations
        }
    ),
    propertySymbol,
    isGetter = false,
    visibility = visibility,
    modality = modality,
    effectiveVisibility = effectiveVisibility,
    isInline = isInline,
    symbol = propertyAccessorSymbol,
    resolvePhase = resolvePhase,
)
