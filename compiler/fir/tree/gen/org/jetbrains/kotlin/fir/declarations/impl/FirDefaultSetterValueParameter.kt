/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
internal class FirDefaultSetterValueParameter(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override var staticReceiverParameter: FirTypeRef?,
    override var deprecationsProvider: DeprecationsProvider,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val symbol: FirValueParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
) : FirValueParameter() {
    override val typeParameters: List<FirTypeParameterRef>
        get() = emptyList()
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override val receiverParameter: FirReceiverParameter?
        get() = null
    override val containerSource: DeserializedContainerSource?
        get() = null
    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = null
    override val contextParameters: List<FirValueParameter>
        get() = emptyList()
    override val name: Name = Name.identifier("value")
    override val initializer: FirExpression?
        get() = null
    override val delegate: FirExpression?
        get() = null
    override val isVar: Boolean
        get() = false
    override val isVal: Boolean
        get() = true
    override val getter: FirPropertyAccessor?
        get() = null
    override val setter: FirPropertyAccessor?
        get() = null
    override val backingField: FirBackingField?
        get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val defaultValue: FirExpression?
        get() = null
    override val isCrossinline: Boolean
        get() = false
    override val isNoinline: Boolean
        get() = false
    override val isVararg: Boolean
        get() = false
    override val valueParameterKind: FirValueParameterKind
        get() = FirValueParameterKind.Regular

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != FirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        staticReceiverParameter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformStaticReceiverParameter(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        staticReceiverParameter = staticReceiverParameter?.transform(transformer, data)
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {
        staticReceiverParameter = newStaticReceiverParameter
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {}

    override fun replaceInitializer(newInitializer: FirExpression?) {}

    override fun replaceDelegate(newDelegate: FirExpression?) {}

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceDefaultValue(newDefaultValue: FirExpression?) {}
}
