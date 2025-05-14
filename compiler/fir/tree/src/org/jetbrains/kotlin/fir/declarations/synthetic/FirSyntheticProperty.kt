/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

class FirSyntheticProperty @FirImplementationDetail internal constructor(
    override val moduleData: FirModuleData,
    override val name: Name,
    override val isVar: Boolean,
    override val symbol: FirSyntheticPropertySymbol,
    private val customStatus: FirDeclarationStatus?,
    override val getter: FirSyntheticPropertyAccessor,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val setter: FirSyntheticPropertyAccessor? = null,
    override val deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
) : FirProperty() {
    init {
        @OptIn(FirImplementationDetail::class)
        symbol.bind(this)

        requireWithAttachment(
            customStatus == null || customStatus is FirResolvedDeclarationStatus,
            { "The status must be resolved as the synthetic property is stateless and hence is not supposed to be transformed later" },
        ) {
            withFirEntry("property", this@FirSyntheticProperty)
        }
    }

    override val status: FirDeclarationStatus
        get() = customStatus ?: getter.status

    override val backingField: FirBackingField? get() = null

    override val returnTypeRef: FirTypeRef
        get() = getter.returnTypeRef

    override val staticReceiverParameter: FirTypeRef?
        get() = null

    override val source: KtSourceElement?
        get() = null

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Synthetic.JavaProperty

    override val initializer: FirExpression?
        get() = null

    override val delegate: FirExpression?
        get() = null

    override val delegateFieldSymbol: FirDelegateFieldSymbol?
        get() = null

    override val isLocal: Boolean
        get() = false

    override val receiverParameter: FirReceiverParameter?
        get() = getter.receiverParameter

    override val isVal: Boolean
        get() = !isVar

    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override val typeParameters: List<FirTypeParameter>
        get() = getter.typeParameters

    override val containerSource: DeserializedContainerSource?
        get() = null

    override val controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override val bodyResolveState: FirPropertyBodyResolveState
        get() = FirPropertyBodyResolveState.ALL_BODIES_RESOLVED

    override val contextParameters: List<FirValueParameter>
        get() = getter.contextParameters

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        notSupported()
    }

    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {
        notSupported()
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {
        notSupported()
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        notSupported()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        notSupported()
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        notSupported()
    }

    override fun replaceDelegate(newDelegate: FirExpression?) {
        notSupported()
    }

    override fun replaceBodyResolveState(newBodyResolveState: FirPropertyBodyResolveState) {
        notSupported()
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {
        notSupported()
    }

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {
        notSupported()
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {
        notSupported()
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        notSupported()
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        notSupported()
    }

    private fun notSupported(): Nothing {
        error("Transformation of synthetic property isn't supported")
    }
}
