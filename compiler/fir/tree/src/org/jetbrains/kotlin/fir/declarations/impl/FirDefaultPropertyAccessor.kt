/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    status: FirDeclarationStatus,
    symbol: FirPropertyAccessorSymbol,
    resolvePhase: FirResolvePhase,
    attributes: FirDeclarationAttributes,
) : FirPropertyAccessorImpl(
    source,
    resolvePhase,
    moduleData,
    origin,
    attributes,
    status,
    propertyTypeRef,
    staticReceiverParameter = null,
    deprecationsProvider = UnresolvedDeprecationProvider,
    dispatchReceiverType = null,
    valueParameters,
    body = null,
    contractDescription = null,
    symbol,
    propertySymbol,
    isGetter,
    annotations = MutableOrEmptyList.empty(),
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
            parameterSource: KtSourceElement? = null,
        ): FirDefaultPropertyAccessor = if (isGetter) {
            FirDefaultPropertyGetter(
                source = source,
                moduleData = moduleData,
                origin = origin,
                propertyTypeRef = propertyTypeRef,
                visibility = visibility,
                propertySymbol = propertySymbol,
                modality = null,
            )
        } else {
            FirDefaultPropertySetter(
                source = source,
                moduleData = moduleData,
                origin = origin,
                propertyTypeRef = propertyTypeRef,
                visibility = visibility,
                propertySymbol = propertySymbol,
                modality = null,
                parameterAnnotations = parameterAnnotations,
                parameterSource = parameterSource,
            )
        }
    }
}

class FirDefaultPropertyGetter(
    source: KtSourceElement?,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    propertySymbol: FirPropertySymbol,
    status: FirDeclarationStatus,
    symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes()
) : FirDefaultPropertyAccessor(
    source,
    moduleData,
    origin,
    propertyTypeRef,
    valueParameters = mutableListOf(),
    propertySymbol,
    isGetter = true,
    status = status,
    symbol = symbol,
    resolvePhase = resolvePhase,
    attributes = attributes,
) {
    constructor(
        source: KtSourceElement?,
        moduleData: FirModuleData,
        origin: FirDeclarationOrigin,
        propertyTypeRef: FirTypeRef,
        visibility: Visibility,
        propertySymbol: FirPropertySymbol,
        modality: Modality?,
        effectiveVisibility: EffectiveVisibility? = null,
        isInline: Boolean = false,
        isOverride: Boolean = false,
        symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
        resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
        attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    ) : this(
        source,
        moduleData,
        origin,
        propertyTypeRef,
        propertySymbol,
        status = createStatus(visibility, modality, effectiveVisibility, isInline, isOverride),
        symbol = symbol,
        resolvePhase = resolvePhase,
        attributes = attributes,
    )
}

/**
 * @param [parameterSource] Should be specified only in the case of invalid code,
 * when default setter has an explicitly written value parameter, and thus it will have its own source
 */
class FirDefaultPropertySetter(
    source: KtSourceElement?,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    propertyTypeRef: FirTypeRef,
    propertySymbol: FirPropertySymbol,
    status: FirDeclarationStatus,
    propertyAccessorSymbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
    parameterSource: KtSourceElement? = null,
    parameterAnnotations: List<FirAnnotation> = emptyList(),
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes()
) : FirDefaultPropertyAccessor(
    source,
    moduleData,
    origin,
    FirImplicitUnitTypeRef(source),
    valueParameters = mutableListOf(
        buildDefaultSetterValueParameter builder@{
            this@builder.resolvePhase = resolvePhase
            this@builder.source = (parameterSource ?: source)?.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
            this@builder.containingDeclarationSymbol = propertyAccessorSymbol
            this@builder.moduleData = moduleData
            this@builder.origin = origin
            this@builder.returnTypeRef = propertyTypeRef
            this@builder.symbol = FirValueParameterSymbol(StandardNames.DEFAULT_VALUE_PARAMETER)
            this@builder.annotations += parameterAnnotations
        }
    ),
    propertySymbol,
    isGetter = false,
    status = status,
    symbol = propertyAccessorSymbol,
    resolvePhase = resolvePhase,
    attributes = attributes,
) {
    constructor(
        source: KtSourceElement?,
        moduleData: FirModuleData,
        origin: FirDeclarationOrigin,
        propertyTypeRef: FirTypeRef,
        visibility: Visibility,
        propertySymbol: FirPropertySymbol,
        modality: Modality?,
        effectiveVisibility: EffectiveVisibility? = null,
        isInline: Boolean = false,
        isOverride: Boolean = false,
        propertyAccessorSymbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol(),
        parameterSource: KtSourceElement? = null,
        parameterAnnotations: List<FirAnnotation> = emptyList(),
        resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
        attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    ) : this(
        source,
        moduleData,
        origin,
        propertyTypeRef,
        propertySymbol,
        status = createStatus(visibility, modality, effectiveVisibility, isInline, isOverride),
        propertyAccessorSymbol,
        parameterSource,
        parameterAnnotations,
        resolvePhase,
        attributes,
    )
}

private fun createStatus(
    visibility: Visibility,
    modality: Modality?,
    effectiveVisibility: EffectiveVisibility?,
    isInline: Boolean,
    isOverride: Boolean,
): FirDeclarationStatusImpl = when (effectiveVisibility) {
    null -> FirDeclarationStatusImpl(visibility, modality)
    else -> FirResolvedDeclarationStatusImpl(visibility, modality ?: Modality.FINAL, effectiveVisibility)
}.apply {
    this.isInline = isInline
    this.isOverride = isOverride
}
