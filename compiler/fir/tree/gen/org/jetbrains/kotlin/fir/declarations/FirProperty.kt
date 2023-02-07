/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirProperty : FirVariable(), FirTypeParametersOwner, FirControlFlowGraphOwner {
    abstract override val source: KtSourceElement?
    abstract override val resolvePhase: FirResolvePhase
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverParameter: FirReceiverParameter?
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val name: Name
    abstract override val initializer: FirExpression?
    abstract override val delegate: FirExpression?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: FirPropertyAccessor?
    abstract override val setter: FirPropertyAccessor?
    abstract override val backingField: FirBackingField?
    abstract override val annotations: List<FirAnnotation>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val contextReceivers: List<FirContextReceiver>
    abstract override val symbol: FirPropertySymbol
    abstract val delegateFieldSymbol: FirDelegateFieldSymbol?
    abstract val isLocal: Boolean
    abstract val bodyResolveState: FirPropertyBodyResolveState
    abstract override val typeParameters: List<FirTypeParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformProperty(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceInitializer(newInitializer: FirExpression?)

    abstract override fun replaceGetter(newGetter: FirPropertyAccessor?)

    abstract override fun replaceSetter(newSetter: FirPropertyAccessor?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract fun replaceBodyResolveState(newBodyResolveState: FirPropertyBodyResolveState)

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirProperty

    abstract fun <D> transformContextReceivers(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirProperty
}
